package com.eneskoc.familytracker.ui

import android.Manifest
import android.content.Intent

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.addCallback
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.eneskoc.familytracker.R
import com.eneskoc.familytracker.data.Resource
import com.eneskoc.familytracker.databinding.FragmentHomeScreenBinding
import com.eneskoc.familytracker.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.eneskoc.familytracker.other.Constants.ACTION_STOP_SERVICE
import com.eneskoc.familytracker.ui.auth.AuthViewModel
import com.eneskoc.familytracker.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.eneskoc.familytracker.other.TrackingUtil
import com.eneskoc.familytracker.servives.TrackingServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
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

    private var isTracking = false
    private lateinit var lastLocation: LatLng
    private var batteryLevel = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.mapView.onCreate(savedInstanceState)
        requestPermission()
        binding.mapView.getMapAsync {
            map = it
            val locationList: List<LatLng> = listOf(
                LatLng(37.183208, 33.211214),
                LatLng(37.178992, 33.218786),
                LatLng(37.188592, 33.218786)
            )
            updateMapMarker(locationList)
            updateMapCamera(locationList)
        }
        subscribeToObservers()

        val topBarSwitch = binding.toolbar.findViewById<SwitchCompat>(R.id.top_app_bar_switch)
        val topBarSearch = binding.toolbar.findViewById<ImageView>(R.id.top_app_bar_search)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            authViewModel.logout()
            findNavController().navigate(R.id.action_homeScreen_to_loginScreen)
        }

        binding.toolbar.setNavigationOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.action_homeScreen_to_loginScreen)
        }

        topBarSearch.setOnClickListener {
            val searchPopup = UserSearchScreen()
            searchPopup.show(requireActivity().supportFragmentManager, "SearchDialog")
        }

        topBarSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            } else {
                sendCommandToService(ACTION_STOP_SERVICE)
            }
        }

        binding.btnTest.setOnClickListener {
            authViewModel.listenToFollowRequests()

            viewLifecycleOwner.lifecycleScope.launch {
                authViewModel.listenToFollowRequestsFlow.collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            resource.result.forEach {
                                println(it.displayName)
                            }
                        }
                        is Resource.Failure -> {
                            val exception = resource.exception
                            Snackbar.make(
                                requireView(),
                                exception.message.toString(),
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                        is Resource.Loading -> {}
                        else -> {}
                    }
                }
            }
        }
    }

    fun updateMapMarker(locationList: List<LatLng>) {
        for (location in locationList) {
            val markerOptions = MarkerOptions()
                .position(location)
                .title("Marker Header")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            map?.addMarker(markerOptions)
        }
    }

    fun updateMapCamera(locationList: List<LatLng>) {
        val builder = LatLngBounds.Builder()
        locationList.forEach {
            builder.include(it)
        }
        val bounds = builder.build()
        val padding = 100 // Markerları çevreleyen kenar boşluğu
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        map?.moveCamera(cameraUpdate)
    }

    fun sendDataToFireStore(location: LatLng, batteryLevel: Int) {
        val fireStoreLocation = Location("providerName")
        fireStoreLocation.latitude = location.latitude
        fireStoreLocation.longitude = location.longitude

        authViewModel.sendLocationData(fireStoreLocation, batteryLevel)

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.sendDataFlow.collect { resource ->
                when (resource) {
                    is Resource.Success -> {

                    }
                    is Resource.Failure -> {
                        val exception = resource.exception
                        Snackbar.make(
                            requireView(),
                            exception.message.toString(),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    is Resource.Loading -> {}
                    else -> {}
                }
            }
        }
    }

    private fun subscribeToObservers() {
        TrackingServices.isTracking.observe(viewLifecycleOwner, Observer {
            isTracking = it
        })
        TrackingServices.location.observe(viewLifecycleOwner, Observer {
            lastLocation = it
            println("LOCATION : ${lastLocation.latitude}, ${lastLocation.longitude}")
            sendDataToFireStore(lastLocation, batteryLevel)
        })

        TrackingServices.batteryLevel.observe(viewLifecycleOwner, Observer {
            batteryLevel = it
            println("BATTERY LEVEL : $batteryLevel")
        })
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

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingServices::class.java).also {
            it.action = action
            requireActivity().startService(it)
        }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {}

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, true)
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