package com.eneskoc.familytracker.data

import android.location.Location
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore
) : AuthRepository {
    override val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser


    override suspend fun login(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Resource.Success(result.user!!)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun signup(
        name: String,
        username: String,
        email: String,
        password: String
    ): Resource<FirebaseUser> {
        return try {

            val firestore = firebaseFirestore
            val usernameQuery = firestore.collection("usernames").document(username).get().await()
            println(usernameQuery)
            if (usernameQuery.exists()) {
                return Resource.Failure(Exception("Username is already taken."))
            } else {
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()

                result.user?.uid?.let { uid ->
                    val userDocument = firestore.collection("users").document(uid)
                    val usernameDocument = firestore.collection("usernames").document(username)

                    firestore.runBatch { batch ->
                        batch.set(
                            userDocument, hashMapOf(
                                "location" to GeoPoint(0.0, 0.0),
                                "batteryLevel" to 0,
                                "username" to username,
                                "displayName" to name
                            )
                        )
                        batch.set(usernameDocument, hashMapOf("uid" to uid))
                    }.await()
                }
                Resource.Success(result.user!!)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override fun logout() {
        firebaseAuth.signOut()
    }

    override suspend fun sendLocationData(location: Location, batteryLevel: Int): Resource<Unit> {
        return try {
            val userId = currentUser?.uid
            val firestore = firebaseFirestore

            val userRef = firestore.collection("users").document(userId!!)
            val updateData = hashMapOf<String, Any>(
                "location" to GeoPoint(location.latitude, location.longitude),
                "batteryLevel" to batteryLevel
            )

            userRef.update(updateData).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun findUser(username: String): Resource<UserDataHolder> {
        return try {
            val firestore = firebaseFirestore
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("username", username)
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Resource.Failure(Exception("User not found"))
            }

            val documentSnapshot = querySnapshot.documents.first()
            val uid = documentSnapshot.id
            val displayName = documentSnapshot.getString("displayName")
            val userDataHolder = UserDataHolder(uid, null,displayName!!, null,username!!)

            Resource.Success(userDataHolder)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun sendFollowRequest(uid: String): Resource<Unit> {
        return try {
            val currentUserId = currentUser?.uid
            val firestore = firebaseFirestore

            val requestsRef = firestore.collection("requests")
            val request = hashMapOf(
                "senderId" to currentUserId, // Current user
                "receiverId" to uid, // The user to whom the request was sent
                "status" to "pending" // Request status
            )

            requestsRef.add(request).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun listenToFollowRequests(): Resource<List<UserDataHolder>> {
        return try {
            val currentUserId = currentUser?.uid
            val firestore = firebaseFirestore
            val userDataList: MutableList<UserDataHolder> = mutableListOf()

            val requestsRef = firestore.collection("requests")
            val requestsQuery = requestsRef.whereEqualTo("receiverId", currentUserId)

            val querySnapshot = requestsQuery.get().await()
            for (documentSnapshot in querySnapshot.documents) {
                val requestStatus = documentSnapshot.getString("status")
                if (requestStatus == "pending") {
                    val senderId = documentSnapshot.getString("senderId")

                    val queryUserSnapshot = firestore.collection("users")
                        .document(senderId!!)
                        .get()
                        .await()

                    val uid = queryUserSnapshot.id
                    val displayName = queryUserSnapshot.getString("displayName")
                    val username = queryUserSnapshot.getString("username")
                    val userDataHolder = UserDataHolder(uid, null,displayName!!, null,username!!)

                    userDataList.add(userDataHolder)
                }
            }

            Resource.Success(userDataList)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun acceptFollowRequest(senderId: String): Resource<Unit> {
        return try {
            val currentUserId = currentUser?.uid
            val firestore = firebaseFirestore

            val requestsRef = firestore.collection("requests")
            val requestsQuery = requestsRef.whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("senderId", senderId)
            val querySnapshot = requestsQuery.get().await()
            for (documentSnapshot in querySnapshot.documents) {
                val requestStatus = documentSnapshot.getString("status")
                if (requestStatus == "pending") {
                    val documentRef = documentSnapshot.reference
                    val updateData = mutableMapOf<String, Any>("status" to "accepted")
                    documentRef.update(updateData).await()
                }
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun rejectFollowRequest(senderId: String): Resource<Unit> {
        return try {
            val currentUserId = currentUser?.uid
            val firestore = firebaseFirestore

            val requestsRef = firestore.collection("requests")
            val requestsQuery = requestsRef.whereEqualTo("receiverId", currentUserId)
                .whereEqualTo("senderId", senderId)
            val querySnapshot = requestsQuery.get().await()
            for (documentSnapshot in querySnapshot.documents) {
                val requestStatus = documentSnapshot.getString("status")
                if (requestStatus == "pending") {
                    val documentRef = documentSnapshot.reference
                    documentRef.delete().await()
                }
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun listenToFollowingUser(): Resource<List<UserDataHolder>> {
        return try {
            val currentUserId = currentUser?.uid
            val firestore = firebaseFirestore
            val userDataList: MutableList<UserDataHolder> = mutableListOf()

            val requestsRef = firestore.collection("requests")
            val requestsQuery = requestsRef.whereEqualTo("senderId", currentUserId)

            val querySnapshot = requestsQuery.get().await()
            for (documentSnapshot in querySnapshot.documents) {
                val requestStatus = documentSnapshot.getString("status")
                if (requestStatus == "accepted") {
                    val reveiverId = documentSnapshot.getString("receiverId")

                    val queryUserSnapshot = firestore.collection("users")
                        .document(reveiverId!!)
                        .get()
                        .await()

                    val uid = queryUserSnapshot.id
                    val displayName = queryUserSnapshot.getString("displayName")
                    val username = queryUserSnapshot.getString("username")
                    val batteryLevel = queryUserSnapshot.getLong("batteryLevel").toString()
                    val geoPoint = queryUserSnapshot.getGeoPoint("location")

                    val location = Location("providerGeoPoint")
                    location.latitude = geoPoint?.latitude ?: 0.0
                    location.longitude = geoPoint?.longitude ?: 0.0

                    val userDataHolder = UserDataHolder(uid, batteryLevel,displayName!!, location,username!!)

                    userDataList.add(userDataHolder)
                }
            }

            Resource.Success(userDataList)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun listenToFollowersUser(): Resource<List<UserDataHolder>> {
        return try {
            val currentUserId = currentUser?.uid
            val firestore = firebaseFirestore
            val userDataList: MutableList<UserDataHolder> = mutableListOf()

            val requestsRef = firestore.collection("requests")
            val requestsQuery = requestsRef.whereEqualTo("receiverId", currentUserId)

            val querySnapshot = requestsQuery.get().await()
            for (documentSnapshot in querySnapshot.documents) {
                val requestStatus = documentSnapshot.getString("status")
                if (requestStatus == "accepted") {
                    val senderId = documentSnapshot.getString("senderId")

                    val queryUserSnapshot = firestore.collection("users")
                        .document(senderId!!)
                        .get()
                        .await()

                    val uid = queryUserSnapshot.id
                    val displayName = queryUserSnapshot.getString("displayName")
                    val username = queryUserSnapshot.getString("username")
                    val userDataHolder = UserDataHolder(uid, null,displayName!!, null,username!!)

                    userDataList.add(userDataHolder)
                }
            }

            Resource.Success(userDataList)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }

    override suspend fun listenToLocation(followersUidList: List<String>): Resource<List<UserDataHolder>> {
        return try {
            val firestore = firebaseFirestore
            val followersList: MutableList<UserDataHolder> = mutableListOf()
            followersList.clear()
            for (followerUid in followersUidList) {
                val queryUserSnapshot = firestore.collection("users")
                    .document(followerUid)
                    .get()
                    .await()

                val uid = queryUserSnapshot.id
                val username = queryUserSnapshot.getString("username")
                val displayName = queryUserSnapshot.getString("displayName")
                val batteryLevel = queryUserSnapshot.getLong("batteryLevel").toString()
                val geoPoint = queryUserSnapshot.getGeoPoint("location")

                val location = Location("providerGeoPoint")
                location.latitude = geoPoint?.latitude ?: 0.0
                location.longitude = geoPoint?.longitude ?: 0.0

                val userDataHolder = UserDataHolder(uid, batteryLevel, displayName!!, location, username!!)

                followersList.add(userDataHolder)
            }

            Resource.Success(followersList)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }
}