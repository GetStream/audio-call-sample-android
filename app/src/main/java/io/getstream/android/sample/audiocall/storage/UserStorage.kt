package io.getstream.android.sample.audiocall.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.getstream.android.sample.audiocall.storage.UserStorage.delete
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull

/**
 * A simple [UserStorage] that will handle mocked login/logout and storage functionality.
 */
object UserStorage {
    private const val USER_KEY = "user_key"
    private const val TOKEN_KEY = "token_key"
    private val Context.dataStore by preferencesDataStore(name = "user_storage")

    private val userStorageKeys =
        Pair(stringPreferencesKey(USER_KEY), stringPreferencesKey(TOKEN_KEY))

    /**
     * Obtain a user flow which reflects current user data.
     */
    fun user(context: Context): Flow<UserData> = context.dataStore.data.mapNotNull {
        val userId = it[userStorageKeys.first]
        val token = it[userStorageKeys.second]
        if (!userId.isNullOrBlank() && !token.isNullOrBlank()) {
            UserData.AudioCallUser(userId, token)
        } else {
            UserData.NoUser
        }
    }

    /**
     * Load the user directly.
     */
    suspend fun loadUser(context: Context): UserData =
        user(context).firstOrNull() ?: UserData.NoUser

    /**
     * Store the user data.
     */
    suspend fun store(context: Context, userData: UserData) = context.dataStore.edit {
        it[userStorageKeys.first] = userData.userId
        it[userStorageKeys.second] = userData.token
    }

    /**
     * Update just the token, without a new user ID.
     */
    suspend fun updateToken(context: Context, token: String) = context.dataStore.edit {
        it[userStorageKeys.second] = token
    }

    /**
     * Delete the user data.
     */
    suspend fun delete(context: Context) = context.dataStore.edit {
        it.remove(userStorageKeys.first) // id
        it.remove(userStorageKeys.second) //  token
    }
}

/**
 * A sealed class representing the user data state.
 */
sealed class UserData(val userId: String, val token: String) {
    /** No user, user needs to login */
    data object NoUser : UserData("", "")

    /** A present logged in user. */
    class AudioCallUser(userId: String, token: String) : UserData(userId, token)
}