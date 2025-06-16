package com.montilivi.esdeveniments.ui.events.create

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.GeoPoint
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.data.model.Event
import com.montilivi.esdeveniments.data.repository.*
import com.montilivi.esdeveniments.databinding.FragmentCreateEventBinding
import com.montilivi.esdeveniments.utils.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CreateEventFragment : Fragment() {

    private var _binding: FragmentCreateEventBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateEventViewModel by viewModels {
        CreateEventViewModelFactory(EventRepository(), AuthRepository(), CategoryRepository())
    }

    private val eventRepository = EventRepository()
    private var selectedImageUri: Uri? = null
    private lateinit var galleryLauncher: ActivityResultLauncher<Intent>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraImageUri: Uri
    private val categoryMap = mutableMapOf<String, String>()
    private val args: CreateEventFragmentArgs by navArgs()
    private var eventReady = false
    private var categoriesReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupImageLaunchers()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        selectedImageUri?.let {
            binding.ivEventImage.setImageURI(it)
            binding.ivEventImage.tag = it.toString()
        }

        setupLocationListener()

        FirebaseReferences.auth.currentUser?.uid?.let { userId ->
            FirebaseReferences.usersCollection.document(userId).get()
                .addOnSuccessListener { doc ->
                    if (!(doc.getBoolean("isOrganizer") ?: false)) {
                        Snackbar.make(view, getString(R.string.permission_denied), Snackbar.LENGTH_LONG).show()
                        findNavController().popBackStack()
                        return@addOnSuccessListener
                    }

                    if (!args.eventId.isNullOrBlank()) {
                        viewModel.loadEventForEdit(args.eventId!!)
                    }

                    viewModel.loadCategories()
                    setupUI()
                }
                .addOnFailureListener {
                    Snackbar.make(view, getString(R.string.permission_error), Snackbar.LENGTH_LONG).show()
                    findNavController().popBackStack()
                }
        }

        binding.btnCreateEvent.text = if (args.eventId.isNullOrEmpty())
            getString(R.string.create_event) else getString(R.string.update_event)
    }

    private fun setupUI() {
        setupCategoryDropdown()
        setupInteractionListeners()
        setupObservers()
    }

    private fun setupImageLaunchers() {
        galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                selectedImageUri = it.data?.data
                binding.ivEventImage.setImageURI(selectedImageUri)
                binding.ivEventImage.tag = selectedImageUri.toString()
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                selectedImageUri = cameraImageUri
                binding.ivEventImage.setImageURI(selectedImageUri)
                binding.ivEventImage.tag = selectedImageUri.toString()
            }
        }
    }

    private fun setupLocationListener() {
        setFragmentResultListener("location_result") { _, bundle ->
            val lat = bundle.getDouble("lat")
            val lng = bundle.getDouble("lng")
            val address = bundle.getString("address")

            binding.tvSelectedLocation.text = address
            binding.tvSelectedLocation.tag = GeoPoint(lat, lng)
        }
    }

    private fun setupObservers() {
        viewModel.eventCreationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is CreateEventViewModel.EventCreationResult.Success -> {
                    toggleProgressOverlay(false)
                    Snackbar.make(requireView(), getString(R.string.event_created_success), Snackbar.LENGTH_SHORT).show()
                    val action = CreateEventFragmentDirections.actionCreateEventToEventDetail(result.eventId)
                    findNavController().navigate(action, NavOptions.Builder().setPopUpTo(R.id.createEventFragment, true).build())
                }
                is CreateEventViewModel.EventCreationResult.Error -> {
                    toggleProgressOverlay(false)
                    Snackbar.make(requireView(), result.message, Snackbar.LENGTH_LONG).show()
                }
                null -> {}
            }
        }

        viewModel.eventData.observe(viewLifecycleOwner) { event ->
            event?.let {
                eventReady = true
                fillFormFields(it)
                updateCategoryUIIfReady()
            }
        }

        viewModel.categoriesMap.observe(viewLifecycleOwner) { map ->
            categoryMap.clear()
            categoryMap.putAll(map)
            categoriesReady = true
            updateCategoryUIIfReady()
        }
    }

    private fun setupCategoryDropdown() {
        viewModel.categoriesMap.observe(viewLifecycleOwner) { map ->
            val categoryNames = map.keys.toTypedArray()

            binding.btnSelectCategory.setOnClickListener {
                if (categoryNames.isEmpty()) {
                    Snackbar.make(requireView(), getString(R.string.no_categories_available), Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.select_category)
                    .setItems(categoryNames) { _, which ->
                        val selectedName = categoryNames[which]
                        binding.tvSelectedCategory.text = selectedName
                        viewModel.selectedCategory.value = map[selectedName] ?: selectedName
                    }
                    .show()
            }
        }
    }

    private fun setupInteractionListeners() {
        binding.btnSelectDateTime.setOnClickListener { showDateTimePicker() }
        binding.btnSelectLocation.setOnClickListener { findNavController().navigate(R.id.selectLocationFragment) }
        binding.btnSelectImage.setOnClickListener { showImageSelectionDialog() }
        binding.btnCreateEvent.setOnClickListener { onCreateEventClicked() }
    }

    private fun onCreateEventClicked() {
        val imageUri = binding.ivEventImage.tag as? String
        val userId = FirebaseReferences.auth.currentUser?.uid ?: return
        val event = buildEventFromForm(userId)

        if (!validarFormulario()) return

        toggleProgressOverlay(true)
        binding.root.post {
            binding.root.findViewById<CircularProgressIndicator>(R.id.progressIndicator).apply {
                isIndeterminate = true
                show()
            }
        }

        lifecycleScope.launch {
            val isEditing = !args.eventId.isNullOrBlank()
            val eventId = args.eventId ?: FirebaseReferences.eventsCollection.document().id

            val finalImageUrl = if (imageUri != null && imageUri.startsWith("content://")) {
                val result = eventRepository.uploadEventImage(
                    context = requireContext(),
                    organizerId = userId,
                    eventId = eventId,
                    imageUri = Uri.parse(imageUri),
                    oldImageUrl = if (isEditing) event.imageUrl else null
                )
                if (result.isFailure) {
                    toggleProgressOverlay(false)
                    Snackbar.make(requireView(), "Fallo al subir la imagen", Snackbar.LENGTH_LONG).show()
                    return@launch
                }
                result.getOrNull()
            } else event.imageUrl

            val updatedEvent = event.copy(eventId = eventId, imageUrl = finalImageUrl ?: "")
            val eventMap = updatedEvent.toFirestoreMap() as Map<String, Any>

            try {
                if (!args.eventId.isNullOrBlank()) {
                    viewModel.updateEvent(args.eventId!!, eventMap)
                    viewModel.setEventCreatedWithId(args.eventId!!)
                } else {
                    viewModel.createEvent(updatedEvent)
                }
            } catch (e: Exception) {
                toggleProgressOverlay(false)
                Snackbar.make(requireView(), "Error procesando evento: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun updateDateRangeText() {
        val timestamps = viewModel.selectedTimestamps.sorted()
        if (timestamps.isEmpty()) {
            binding.tvEventDateRange.text = getString(R.string.no_date_selected)
            return
        }
        val f = SimpleDateFormat("d MMMM", Locale.getDefault())
        val sf = SimpleDateFormat("EEEE, d MMMM 'a las' HH:mm", Locale("es", "ES"))
        val start = Date(timestamps.first())
        val end = Date(timestamps.last())
        binding.tvEventDateRange.text = if (start == end) sf.format(start)
        else getString(R.string.event_date_range, f.format(start), f.format(end))
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            calendar.set(y, m, d)
            TimePickerDialog(requireContext(), { _, h, min ->
                calendar.set(Calendar.HOUR_OF_DAY, h)
                calendar.set(Calendar.MINUTE, min)
                val start = calendar.timeInMillis
                if (start < System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2)) {
                    Snackbar.make(requireView(), getString(R.string.event_start_too_soon), Snackbar.LENGTH_LONG).show()
                    return@TimePickerDialog
                }

                DatePickerDialog(requireContext(), { _, ey, em, ed ->
                    calendar.set(ey, em, ed)
                    TimePickerDialog(requireContext(), { _, eh, emin ->
                        calendar.set(Calendar.HOUR_OF_DAY, eh)
                        calendar.set(Calendar.MINUTE, emin)
                        val end = calendar.timeInMillis
                        viewModel.selectedTimestamps.clear()
                        viewModel.selectedTimestamps.add(start)
                        viewModel.selectedTimestamps.add(end)
                        viewModel.startDate.value = start
                        viewModel.endDate.value = end
                        updateDateRangeText()
                    }, 0, 0, true).show()
                }, y, m, d).show()
            }, 0, 0, true).show()
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showImageSelectionDialog() {
        val options = arrayOf(getString(R.string.gallery), getString(R.string.camera))
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.select_image_title))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        galleryLauncher.launch(intent)
                    }
                    1 -> {
                        val file = File.createTempFile("event_image", ".jpg", requireContext().cacheDir)
                        cameraImageUri = FileProvider.getUriForFile(requireContext(), "com.montilivi.esdeveniments.fileprovider", file)
                        cameraLauncher.launch(cameraImageUri)
                    }
                }
            }.show()
    }

    //recupera dades en cas d'edició
    private fun fillFormFields(event: Event) {
        binding.etTitle.setText(event.title)
        binding.etDescription.setText(event.description)

        if (binding.tvSelectedLocation.tag == null) {
            binding.tvSelectedLocation.text = event.locationName
            binding.tvSelectedLocation.tag = event.location
        }

        viewModel.startDate.value = event.startDate
        viewModel.endDate.value = event.endDate
        viewModel.selectedTimestamps.clear()
        viewModel.selectedTimestamps.add(event.startDate)
        viewModel.selectedTimestamps.add(event.endDate)
        updateDateRangeText()

        event.imageUrl.takeIf { it.isNotBlank() }?.let {
            Glide.with(this)
                .load(it)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.DATA)
                .into(binding.ivEventImage)
            binding.ivEventImage.tag = it
        }
    }

    //valida que tot sigui omplert
    private fun validarFormulario(): Boolean {
        val titulo = binding.etTitle.text.toString().trim()
        val descripcion = binding.etDescription.text.toString().trim()
        val location = binding.tvSelectedLocation.tag as? GeoPoint
        val categoria = viewModel.selectedCategory.value
        val fechaInicio = viewModel.startDate.value ?: 0L
        val fechaFin = viewModel.endDate.value ?: 0L
        val imageTag = binding.ivEventImage.tag as? String

        val ahora = System.currentTimeMillis()
        val dosDiasEnMillis = 2 * 24 * 60 * 60 * 1000

        return when {
            titulo.isBlank() -> {
                binding.etTitle.error = getString(R.string.title_required)
                binding.etTitle.requestFocus()
                false
            }
            descripcion.isBlank() -> {
                binding.etDescription.error = getString(R.string.description_required)
                binding.etDescription.requestFocus()
                false
            }
            location == null -> {
                Snackbar.make(requireView(), getString(R.string.location_required), Snackbar.LENGTH_SHORT).show()
                scrollTo(binding.tvSelectedLocation)
                false
            }
            categoria.isNullOrBlank() -> {
                Snackbar.make(requireView(), getString(R.string.category_required), Snackbar.LENGTH_SHORT).show()
                scrollTo(binding.btnSelectCategory)
                false
            }
            fechaFin < fechaInicio -> {
                Snackbar.make(requireView(), getString(R.string.invalid_dates), Snackbar.LENGTH_SHORT).show()
                scrollTo(binding.btnSelectDateTime)
                false
            }
            args.eventId.isNullOrEmpty() && (imageTag == null || !imageTag.startsWith("content://")) -> {
                Snackbar.make(requireView(), getString(R.string.select_image_title), Snackbar.LENGTH_SHORT).show()
                scrollTo(binding.ivEventImage)
                false
            }
            fechaInicio - ahora < dosDiasEnMillis -> {
                Snackbar.make(requireView(), getString(R.string.event_start_too_soon), Snackbar.LENGTH_SHORT).show()
                scrollTo(binding.btnSelectDateTime)
                false
            }
            else -> true
        }
    }

    private fun scrollTo(view: View) {
        binding.scrollView.post {
            binding.scrollView.smoothScrollTo(0, view.top)
        }
    }

    //construeix l'esdeveniment
    private fun buildEventFromForm(userId: String): Event {
        return Event(
            eventId = args.eventId ?: "",
            title = binding.etTitle.text.toString().trim(),
            description = binding.etDescription.text.toString().trim(),
            category = viewModel.selectedCategory.value ?: "",
            startDate = viewModel.startDate.value ?: 0L,
            endDate = viewModel.endDate.value ?: 0L,
            location = binding.tvSelectedLocation.tag as? GeoPoint,
            locationName = binding.tvSelectedLocation.text.toString(),
            imageUrl = viewModel.eventData.value?.imageUrl ?: "",
            creatorId = userId,
            createdAt = if (args.eventId.isNullOrBlank()) com.google.firebase.Timestamp.now() else viewModel.eventData.value?.createdAt,
            updatedAt = com.google.firebase.Timestamp.now()
        )
    }

    private fun updateCategoryUIIfReady() {
        if (!eventReady || !categoriesReady) return
        val event = viewModel.eventData.value ?: return
        val displayName = categoryMap.entries.find { it.value == event.category }?.key
        if (displayName != null) {
            binding.tvSelectedCategory.text = displayName
            viewModel.selectedCategory.value = event.category
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
