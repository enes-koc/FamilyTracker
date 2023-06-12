package com.eneskoc.familytracker.data.models

import android.location.Location

data class UserDataHolder(
    val uid: String?,
    val batteryLevel: String?,
    val displayName: String?,
    val location: Location?,
    val username: String?,
)
