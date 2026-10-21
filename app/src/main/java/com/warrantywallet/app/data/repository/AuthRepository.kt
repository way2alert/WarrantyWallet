package com.warrantywallet.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.warrantywallet.app.core.datastore.PreferenceManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class UserSession(
    val userId: String? = null,
    val email: String? = null,
    val isGuest: Boolean = true,
    val isSignedIn: Boolean = false
)

class AuthRepository(
    private val preferenceManager: PreferenceManager
) {
    private val firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }

    private val _userSession = MutableStateFlow(
        UserSession(
            userId = firebaseAuth?.currentUser?.uid,
            email = firebaseAuth?.currentUser?.email,
            isGuest = firebaseAuth?.currentUser == null,
            isSignedIn = firebaseAuth?.currentUser != null
        )
    )
    val userSession: Flow<UserSession> = _userSession.asStateFlow()

    init {
        firebaseAuth?.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                _userSession.value = UserSession(
                    userId = user.uid,
                    email = user.email,
                    isGuest = false,
                    isSignedIn = true
                )
            } else {
                _userSession.value = UserSession(isGuest = true, isSignedIn = false)
            }
        }
    }

    suspend fun continueAsGuest() {
        preferenceManager.setGuestMode(true)
        preferenceManager.setUserId(null)
        _userSession.value = UserSession(isGuest = true, isSignedIn = false)
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<String> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth not initialized."))
        return try {
            val result = auth.signInWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: ""
            preferenceManager.setGuestMode(false)
            preferenceManager.setUserId(uid)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String): Result<String> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth not initialized."))
        return try {
            val result = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = result.user?.uid ?: ""
            preferenceManager.setGuestMode(false)
            preferenceManager.setUserId(uid)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<String> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth not initialized."))
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val uid = result.user?.uid ?: ""
            preferenceManager.setGuestMode(false)
            preferenceManager.setUserId(uid)
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth not initialized."))
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        preferenceManager.setUserId(null)
        preferenceManager.setGuestMode(true)
        _userSession.value = UserSession(isGuest = true, isSignedIn = false)
    }

    suspend fun deleteAccount(): Result<Unit> {
        val auth = firebaseAuth ?: return Result.failure(Exception("Firebase Auth not initialized."))
        return try {
            auth.currentUser?.delete()?.await()
            signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
