package com.eneskoc.familytracker.data

import android.location.Location
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    val currentUser:FirebaseUser?
    suspend fun login(email:String,password:String): Resource<FirebaseUser>
    suspend fun signup(name:String,email:String,password:String): Resource<FirebaseUser>
    fun logout()
    suspend fun sendLocationData(location:Location,batteryLevel:Float):Resource<Unit>
}