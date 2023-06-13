package com.eneskoc.familytracker.data

import android.location.Location
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow

interface AuthRepository {
    val currentUser:FirebaseUser?
    suspend fun login(email:String,password:String): Resource<FirebaseUser>
    suspend fun signup(name:String,username:String,email:String,password:String): Resource<FirebaseUser>
    fun logout()
    suspend fun sendLocationData(location:Location,batteryLevel:Int):Resource<Unit>
    suspend fun findUser(username:String):Resource<UserDataHolder>
    suspend fun sendFollowRequest(uid: String):Resource<Unit>
    suspend fun acceptFollowRequest(senderId: String):Resource<Unit>
    suspend fun rejectFollowRequest(senderId: String):Resource<Unit>
    suspend fun listenToFollowRequests():Resource<List<UserDataHolder>>
    suspend fun listenToFollowingUser():Resource<List<UserDataHolder>>
    suspend fun listenToFollowersUser():Resource<List<UserDataHolder>>
    suspend fun listenToLocation(followersUidList: List<String>):Resource<List<UserDataHolder>>
}