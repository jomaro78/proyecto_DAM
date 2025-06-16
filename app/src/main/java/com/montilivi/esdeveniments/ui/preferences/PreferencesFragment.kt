package com.montilivi.esdeveniments.ui.preferences

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.method.Touch.scrollTo
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.utils.FirebaseReferences
import com.montilivi.esdeveniments.data.model.AuthProvider
import com.montilivi.esdeveniments.data.model.User
import com.montilivi.esdeveniments.data.repository.CategoryRepository
import com.montilivi.esdeveniments.databinding.FragmentPreferencesBinding
import com.montilivi.esdeveniments.data.repository.UserRepository
import com.montilivi.esdeveniments.utils.ImageUtils
import com.montilivi.esdeveniments.utils.toggleProgressOverlay

class PreferencesFragment : Fragment(R.layout.fragment_preferences) {

    private var _binding: FragmentPreferencesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PreferencesViewModel

    private val selectedCategories = mutableListOf<String>()
    private var currentLocation: LatLng? = null
    private var userReady = false
    private var categoriesReady = false

    private val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            viewModel.fetchCurrentLocation(requireContext())
        } else {
            Snackbar.make(requireView(), getString(R.string.location_permission_denied), Snackbar.LENGTH_SHORT).show()
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.ivProfileImage.setImageURI(it)
            binding.ivProfileImage.tag = it.toString()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPreferencesBinding.bind(view)

        val userRepository = UserRepository()
        val categoryRepository = CategoryRepository()
        val factory = PreferencesViewModelFactory(userRepository, categoryRepository)
        viewModel = ViewModelProvider(this, factory)[PreferencesViewModel::class.java]

        val userId = FirebaseReferences.auth.currentUser?.uid ?: return
        viewModel.loadUserPreferences(userId)
        viewModel.loadCategories()

        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)

        //per no poder retrocedir sense haver guardat
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Snackbar.make(requireView(), "Debes completar tus preferencias para continuar.", Snackbar.LENGTH_SHORT).show()
            }
        })

        setupUI()
        setupObservers()
    }

    private fun setupUI() {
        binding.seekDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvDistance.text = getString(R.string.pf_distance, progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnGetLocation.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), locationPermission) != PackageManager.PERMISSION_GRANTED) {
                requestPermission.launch(locationPermission)
            } else viewModel.fetchCurrentLocation(requireContext())
        }

        binding.btnSelectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.btnSelectCategories.setOnClickListener {
            val availableCategories = viewModel.categoriesMap.value?.keys?.toList() ?: emptyList()

            if (availableCategories.isEmpty()) {
                Snackbar.make(requireView(), getString(R.string.no_categories_available), Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedItems = BooleanArray(availableCategories.size) { index ->
                val id = viewModel.categoriesMap.value?.get(availableCategories[index])
                selectedCategories.contains(id)
            }

            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.ad_select_categories))
                .setMultiChoiceItems(availableCategories.toTypedArray(), selectedItems) { _, which, isChecked ->
                    val id = viewModel.categoriesMap.value?.get(availableCategories[which])
                    if (id != null) {
                        if (isChecked) {
                            if (!selectedCategories.contains(id)) selectedCategories.add(id)
                        } else {
                            selectedCategories.remove(id)
                        }
                    }
                }
                .setPositiveButton(getString(R.string.ad_accept)) { _, _ ->
                    val visibles = availableCategories.filter {
                        val id = viewModel.categoriesMap.value?.get(it)
                        selectedCategories.contains(id)
                    }
                    binding.tvSelectedCategories.text = visibles.joinToString(", ")
                }
                .setNegativeButton(getString(R.string.ad_cancel), null)
                .show()
        }

        binding.btnSavePreferences.setOnClickListener {
            if (!validarFormulario()) return@setOnClickListener
            val uriTag = binding.ivProfileImage.tag as? String
            val userId = FirebaseReferences.auth.currentUser?.uid ?: return@setOnClickListener

            val user = buildUserFromForm(userId)
            toggleProgressOverlay(true)

            binding.root.post {
                val indicator = binding.root.findViewById<CircularProgressIndicator>(R.id.progressIndicator)
                indicator.isIndeterminate = true
                indicator.show()
            }

            if (uriTag != null && uriTag.startsWith("content://")) {
                val imageUri = Uri.parse(uriTag)
                viewModel.uploadImageAndSave(user, requireContext(), imageUri)
            } else {
                viewModel.saveUserPreferences(user.toFirestoreMap())
            }
        }
    }

    private fun setupObservers() {
        fun updateVisibleCategories() {
            if (userReady && categoriesReady) {
                val visibleNames = viewModel.categoriesMap.value
                    ?.filter { (_, id) -> selectedCategories.contains(id) }
                    ?.map { it.key }
                    ?: emptyList()
                binding.tvSelectedCategories.text = visibleNames.joinToString(", ")
            }
        }

        viewModel.categoriesMap.observe(viewLifecycleOwner) {
            categoriesReady = true
            updateVisibleCategories()
        }

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                userReady = true
                binding.etUsername.setText(it.username)
                selectedCategories.clear()
                selectedCategories.addAll(it.selectedCategories)
                updateVisibleCategories()

                binding.seekDistance.progress = it.maxDistance
                binding.tvDistance.text = getString(R.string.pf_distance, it.maxDistance)
                binding.cbOrganizer.isChecked = it.isOrganizer

                binding.ivProfileImage.tag = it.profileImageUri
                val imageUrl = it.profileImageUri
                if (!imageUrl.isNullOrBlank()) {
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .into(binding.ivProfileImage)
                }

                binding.tvLocation.tag = it.location
                it.location?.let { loc ->
                    currentLocation = LatLng(loc.latitude, loc.longitude)
                    binding.tvLocation.text = getString(R.string.pf_ubication, loc.latitude, loc.longitude)
                }
            }
        }

        viewModel.location.observe(viewLifecycleOwner) { loc ->
            if (loc != null) {
                binding.tvLocation.text = getString(R.string.pf_ubication, loc.latitude, loc.longitude)
            }
            binding.tvLocation.tag = loc
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is PreferencesViewModel.SaveResult.Success -> {
                    toggleProgressOverlay(false)
                    Snackbar.make(requireView(), getString(R.string.preferences_saved), Snackbar.LENGTH_SHORT).show()
                    findNavController().navigate(
                        R.id.action_preferences_to_home,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.preferencesFragment, true)
                            .build()
                    )
                }
                is PreferencesViewModel.SaveResult.Error -> {
                    toggleProgressOverlay(false)
                    Snackbar.make(requireView(), getString(R.string.save_preferences_error, result.message), Snackbar.LENGTH_SHORT).show()
                }
            }
        }
    }

    // munta un user a partir de les dades del formulari
    private fun buildUserFromForm(userId: String): User {
        val existingUser = viewModel.userData.value
        val now = Timestamp.now()
        val authEmail = FirebaseReferences.auth.currentUser?.email ?: ""
        val providerId = FirebaseReferences.auth.currentUser?.providerData?.lastOrNull()?.providerId
        val authProvider = when (providerId) {
            "google.com" -> AuthProvider.GOOGLE
            else -> AuthProvider.EMAIL
        }

        return User(
            userId = userId,
            username = binding.etUsername.text.toString(),
            email = existingUser?.email?.takeIf { it.isNotBlank() } ?: authEmail,
            profileImageUri = existingUser?.profileImageUri,
            selectedCategories = selectedCategories,
            maxDistance = binding.seekDistance.progress,
            location = binding.tvLocation.tag as? GeoPoint ?: viewModel.location.value,
            authProvider = existingUser?.authProvider ?: authProvider,
            isOrganizer = binding.cbOrganizer.isChecked,
            createdAt = existingUser?.createdAt ?: now,
            updatedAt = now
        )
    }

    // valida que no hi han camps buids
    private fun validarFormulario(): Boolean {
        val nombre = binding.etUsername.text.toString().trim()
        val categoriasSeleccionadas = selectedCategories
        val ubicacion = binding.tvLocation.tag as? GeoPoint
        val imagen = binding.ivProfileImage.tag as? String

        return when {
            nombre.isBlank() -> {
                binding.etUsername.error = getString(R.string.username_required)
                binding.etUsername.requestFocus()
                false
            }
            categoriasSeleccionadas.isEmpty() -> {
                Snackbar.make(requireView(), getString(R.string.category_required), Snackbar.LENGTH_SHORT).show()
                scrollTo(binding.tvSelectedCategories)
                false
            }
            ubicacion == null -> {
                Snackbar.make(requireView(), getString(R.string.location_required), Snackbar.LENGTH_SHORT).show()
                scrollTo(binding.tvLocation)
                false
            }
            imagen.isNullOrBlank() -> {
                Snackbar.make(requireView(), getString(R.string.image_required), Snackbar.LENGTH_SHORT).show()
                scrollTo(binding.ivProfileImage)
                false
            }
            else -> true
        }
    }

    // ens porta a l'element que no hem omplert
    private fun scrollTo(view: View) {
        binding.scrollView.post {
            binding.scrollView.smoothScrollTo(0, view.top)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
