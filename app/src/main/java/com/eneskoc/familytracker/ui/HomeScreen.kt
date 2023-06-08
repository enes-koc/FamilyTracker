package com.eneskoc.familytracker.ui

import android.Manifest
import android.annotation.SuppressLint

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.eneskoc.familytracker.R
import com.eneskoc.familytracker.data.Resource
import com.eneskoc.familytracker.databinding.FragmentHomeScreenBinding
import com.eneskoc.familytracker.ui.auth.AuthViewModel
import com.eneskoc.familytracker.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.eneskoc.familytracker.other.TrackingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.material.snackbar.Snackbar
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeScreen : Fragment(), EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!

    private val authViewModel by viewModels<AuthViewModel>()
    private var map: GoogleMap? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        createLocationRequest()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0?.lastLocation?.let { location ->
                    // Konum güncellemesi alındığında burası çalışır
                    handleLocationUpdate(location)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        requestPermission()
        binding.mapView.getMapAsync {
            map = it
        }


        binding.btnTest.setOnClickListener {
            startLocationUpdates()
//            val location = Location("providerName")
//            location.latitude = 50.4935
//            location.longitude = -122.1402
//            val batteryLevel = 50f
//
//            authViewModel.sendLocationData(location, batteryLevel)
//
//            viewLifecycleOwner.lifecycleScope.launch {
//                authViewModel.sendDataFlow.collect { resource ->
//                    when (resource) {
//                        is Resource.Success -> {}
//                        is Resource.Failure -> {
//                            val exception = resource.exception
//                            Snackbar.make(view, exception.message.toString(), Snackbar.LENGTH_LONG).show()
//                        }
//                        is Resource.Loading -> {}
//                        else -> {}
//                    }
//                }
//            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            authViewModel?.logout()
            findNavController().navigate(R.id.action_homeScreen_to_loginScreen)
        }
    }

    private fun requestPermission() {
        if (TrackingUtil.hasLocationPermission(requireContext())) {
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                //Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            SettingsDialog.Builder(requireContext()).build().show()
        } else {
            requestPermission()
        }
    }
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {}

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, true)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // Konum güncellemesi için istenen aralık (ms)
            fastestInterval = 5000 // Konum güncellemeleri için maksimum aralık (ms)
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY // Konum hassasiyeti
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(requireContext())
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // Ayarlar doğru olduğunda burası çalışır
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (TrackingUtil.hasLocationPermission(requireContext())) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    private fun handleLocationUpdate(location: Location) {
        // Konum güncellemeleri burada kullanılabilir

        val latitude = location.latitude
        val longitude = location.longitude
        println("Latitude: $latitude, Longitude: $longitude")

        authViewModel.sendLocationData(location, 50f)

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.sendDataFlow.collect { resource ->
                when (resource) {
                    is Resource.Success -> {}
                    is Resource.Failure -> {
                        val exception = resource.exception
                        //Snackbar.make(view, exception.message.toString(), Snackbar.LENGTH_LONG).show()
                    }
                    is Resource.Loading -> {}
                    else -> {}
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
        stopLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }
}