package com.eneskoc.familytracker.data

import android.location.Location
import com.eneskoc.familytracker.data.models.UserDataHolder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore
) : AuthRepository {
    override val currentUser: FirebaseUser? get() = firebaseAuth.currentUser


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
            }else{
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()

                result.user?.uid?.let { uid ->
                    val userDocument = firestore.collection("users").document(uid)
                    val usernameDocument = firestore.collection("usernames").document(username)

                    firestore.runBatch { batch ->
                        batch.set(userDocument, hashMapOf(
                            "location" to GeoPoint(0.0, 0.0),
                            "batteryLevel" to 0,
                            "username" to username,
                            "displayName" to name)
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
            val updateData = mapOf(
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
            val userDataHolder = UserDataHolder(uid, displayName!!, username)

            Resource.Success(userDataHolder)
        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Failure(e)
        }
    }


}