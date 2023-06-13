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
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.eneskoc.familytracker.databinding.FragmentHomeScreenBinding
import com.eneskoc.familytracker.other.Constants.ACTION_START_OR_RESUME_SERVICE
import com.eneskoc.familytracker.other.Constants.ACTION_STOP_SERVICE
import com.eneskoc.familytracker.ui.auth.AuthViewModel
import com.eneskoc.familytracker.other.Constants.REQUEST_CODE_LOCATION_PERMISSION
import com.eneskoc.familytracker.other.TrackingUtil
import com.eneskoc.familytracker.servives.TrackingServices
import com.eneskoc.familytracker.ui.notification.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.*

@AndroidEntryPoint
class HomeScreen : Fragment(), EasyPermissions.PermissionCallbacks {

    private var _binding: FragmentHomeScreenBinding? = null
    private val binding get() = _binding!!

    private val authViewModel by viewModels<AuthViewModel>()
    private var map: GoogleMap? = null

    private var isTracking = false
    private lateinit var lastLocation: LatLng
    private var batteryLevel = 0

    private lateinit var followingAdapter: HomeScreenFollowingAdapter
    private lateinit var followersAdapter: HomeScreenFollowersAdapter

    private var locationJob: Job? = null
    private var followingList: MutableList<UserDataHolder> = mutableListOf()

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
        subscribeToObservers()
        startLoop()

        followingAdapter = HomeScreenFollowingAdapter(emptyList())
        //followingAdapter.setOnItemClickListener(this)
        binding.recyclerViewFollowing.adapter = followingAdapter

        followersAdapter = HomeScreenFollowersAdapter(emptyList())
        //followersAdapter.setOnItemClickListener(this)
        binding.recyclerViewFollowers.adapter = followersAdapter

        listenFollowingUser()

        binding.toolbar.title = authViewModel.currentUser?.displayName ?: "Empty"

        binding.mapView.getMapAsync {
            map = it
            //updateMapCamera(followingList)
        }


        val topBarSwitch = binding.toolbar.findViewById<SwitchCompat>(R.id.top_app_bar_switch)
        val topBarSearch = binding.toolbar.findViewById<ImageView>(R.id.top_app_bar_search)
        val topBarNotification =
            binding.toolbar.findViewById<ImageView>(R.id.top_app_bar_notification)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            authViewModel.logout()
            findNavController().navigate(R.id.action_homeScreen_to_loginScreen)
            locationJob?.cancel()
        }

        binding.toolbar.setNavigationOnClickListener {
            authViewModel.logout()
            findNavController().navigate(R.id.action_homeScreen_to_loginScreen)
            locationJob?.cancel()
        }

        topBarNotification.setOnClickListener {
            val notificationPopup = NotificationDialogFragmentScreen()
            notificationPopup.show(requireActivity().supportFragmentManager, "NotificationDialog")
            listenToLocation()
        }

        topBarSearch.setOnClickListener {
            val searchPopup = UserSearchDialogFragmentScreen()
            searchPopup.show(requireActivity().supportFragmentManager, "SearchDialog")
        }

        topBarSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
            } else {
                sendCommandToService(ACTION_STOP_SERVICE)
            }
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        //Following
                        binding.recyclerViewFollowing.visibility = View.VISIBLE
                        binding.recyclerViewFollowers.visibility = View.GONE
                        listenFollowingUser()
                    }
                    1 -> {
                        //Followers
                        binding.recyclerViewFollowing.visibility = View.GONE
                        binding.recyclerViewFollowers.visibility = View.VISIBLE
                        listenFollowersUser()

                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    fun startLoop() {
        locationJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                delay(3_000)
                listenToLocation()
            }
        }
    }

    fun listenToLocation() {
        val tempFollowingList = mutableListOf<String>()
        authViewModel.listenToFollowingUser()
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.listenToFollowingUserFlow.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        resource.result.forEach { tempFollowingList.add(it.uid!!) }
                        authViewModel.listenToLocation(tempFollowingList)
                    }
                    is Resource.Failure -> {}
                    is Resource.Loading -> {}
                    else -> {}
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.listenToLocationFlow.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        followingList.clear()
                        followingList.addAll(resource.result)

                        CoroutineScope(Dispatchers.Main).launch {
                            binding.mapView.getMapAsync {
                                it.clear()
                                updateMapMarker(followingList)
                            }
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

    fun listenFollowingUser() {
        authViewModel.listenToFollowingUser()

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.listenToFollowingUserFlow.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        followingAdapter.userDataList = resource.result
                        followingAdapter.notifyDataSetChanged()


                        if (resource.result.isEmpty()) {
                            binding.tvResultMessage.visibility = View.VISIBLE
                            binding.recyclerViewFollowing.visibility = View.GONE
                            binding.tvResultMessage.text =
                                "You are not following any users. You can search for users to follow."

                        } else {
                            binding.tvResultMessage.visibility = View.GONE
                            binding.recyclerViewFollowing.visibility = View.VISIBLE
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

    fun listenFollowersUser() {
        authViewModel.listenToFollowersUser()

        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.listenToFollowersUserFlow.collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        followersAdapter.userDataList = resource.result
                        followersAdapter.notifyDataSetChanged()

                        if (resource.result.isEmpty()) {
                            binding.tvResultMessage.visibility = View.VISIBLE
                            binding.recyclerViewFollowers.visibility = View.GONE
                            binding.tvResultMessage.text = "There are no users following you."
                        } else {
                            binding.tvResultMessage.visibility = View.GONE
                            binding.recyclerViewFollowers.visibility = View.VISIBLE
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

    fun updateMapMarker(followingList: MutableList<UserDataHolder>) {
        for (follower in followingList) {
            val markerOptions = MarkerOptions()
                .position(LatLng(follower.location!!.latitude, follower.location.longitude))
                .title(follower.displayName)
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
            sendDataToFireStore(lastLocation, batteryLevel)
        })

        TrackingServices.batteryLevel.observe(viewLifecycleOwner, Observer {
            batteryLevel = it
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
        locationJob?.cancel()
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