package com.montilivi.esdeveniments.ui.location

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.snackbar.Snackbar
import com.montilivi.esdeveniments.R
import com.montilivi.esdeveniments.utils.FirebaseReferences
import java.util.*

class SelectLocationFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var selectedMarker: Marker? = null
    private var selectedLatLng: LatLng? = null
    private var selectedAddress: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_select_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), getString(R.string.google_maps_key), Locale.getDefault())
        }

        val autocompleteFragment = childFragmentManager.findFragmentById(R.id.autocompleteFragment)
                as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS))
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let {
                    selectedLatLng = it
                    selectedAddress = "${place.name}, ${place.address}"
                    moveMarker(it)
                }
            }

            override fun onError(status: Status) {
                Snackbar.make(requireView(), getString(R.string.error_place_search, status.statusMessage ?: ""), Snackbar.LENGTH_SHORT).show()
            }
        })

        view.findViewById<View>(R.id.btnConfirmLocation).setOnClickListener {
            if (selectedLatLng != null && selectedAddress != null) {
                val result = Bundle().apply {
                    putDouble("lat", selectedLatLng!!.latitude)
                    putDouble("lng", selectedLatLng!!.longitude)
                    putString("address", selectedAddress)
                }
                setFragmentResult("location_result", result)
                requireActivity().onBackPressedDispatcher.onBackPressed()
            } else {
                Snackbar.make(requireView(), getString(R.string.invalid_location), Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val userId = FirebaseReferences.auth.currentUser?.uid

        if (userId != null) {
            FirebaseReferences.usersCollection.document(userId).get()
                .addOnSuccessListener { document ->
                    val locationData = document.getGeoPoint("location")
                    if (locationData != null) {
                        val preferredLatLng = LatLng(locationData.latitude, locationData.longitude)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(preferredLatLng, 13f))
                    } else {
                        centrarEnUbicacionActual()
                    }
                }
                .addOnFailureListener {
                    centrarEnUbicacionActual()
                }
        } else {
            centrarEnUbicacionActual()
        }

        googleMap.setOnMapClickListener { latLng ->
            selectedLatLng = latLng

            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = try {
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            } catch (e: Exception) {
                null
            }

            selectedAddress = if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]

                val nombre = addr.featureName
                val calle = addr.thoroughfare
                val numero = addr.subThoroughfare
                val cp = addr.postalCode
                val municipio = addr.locality

                val calleCompleta = when {
                    !calle.isNullOrBlank() && !numero.isNullOrBlank() -> "$calle $numero"
                    !calle.isNullOrBlank() -> calle
                    else -> null
                }

                val cuerpo = listOfNotNull(
                    calleCompleta,
                    if (!cp.isNullOrBlank() && !municipio.isNullOrBlank()) "$cp $municipio"
                    else cp ?: municipio
                ).joinToString(", ")

                val nombreEsValido = !nombre.isNullOrBlank()
                        && nombre != numero
                        && (calle == null || !nombre.contains(calle, ignoreCase = true))

                if (nombreEsValido) {
                    "$nombre, $cuerpo"
                } else {
                    cuerpo
                }
            } else {
                "Lat: ${latLng.latitude}, Lng: ${latLng.longitude}"
            }

            moveMarker(latLng)
        }
    }

    private fun centrarEnUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
            val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
                }
            }
        }
    }


    private fun moveMarker(position: LatLng) {
        selectedMarker?.remove()
        selectedMarker = googleMap.addMarker(MarkerOptions().position(position))
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f))
    }
}
