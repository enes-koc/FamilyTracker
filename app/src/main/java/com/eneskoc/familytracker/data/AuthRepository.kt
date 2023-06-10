package com.eneskoc.familytracker.data

import android.location.Location
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.google.firebase.auth.FirebaseUser

interface AuthRepository {
    val currentUser:FirebaseUser?
    suspend fun login(email:String,password:String): Resource<FirebaseUser>
    suspend fun signup(name:String,username:String,email:String,password:String): Resource<FirebaseUser>
    fun logout()
    suspend fun sendLocationData(location:Location,batteryLevel:Int):Resource<Unit>
    suspend fun findUser(username:String):Resource<UserDataHolder>
}