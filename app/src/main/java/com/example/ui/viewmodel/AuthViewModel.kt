package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.UserEntity
import com.example.data.database.StudyLogEntity
import com.example.data.repository.UserRepository
import com.example.data.repository.StreakReward
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Authenticated(val user: UserEntity) : AuthUiState()
    object Unauthenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserRepository(application)
    
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val leaderboardFlow = repository.getLeaderboardFlow()
    val streakRewardFlow = repository.streakRewardState
    val studyLogsFlow = repository.getStudyLogsFlow()

    fun clearStreakReward() {
        repository.clearStreakReward()
    }

    init {
        checkSession()
    }

    fun checkSession() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                repository.loadInitialUser()
                val user = repository.currentUserState.value
                if (user != null) {
                    _uiState.value = AuthUiState.Authenticated(user)
                } else {
                    _uiState.value = AuthUiState.Unauthenticated
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Failed to check active session: ${e.localizedMessage}")
            }
        }
    }

    fun login(email: String, passwordText: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val success = repository.loginWithEmail(email, passwordText)
            if (success) {
                val user = repository.currentUserState.value
                if (user != null) {
                    _uiState.value = AuthUiState.Authenticated(user)
                } else {
                    _uiState.value = AuthUiState.Unauthenticated
                }
            } else {
                _uiState.value = AuthUiState.Error("Incorrect email or password. Please try again.")
            }
        }
    }

    fun signUp(email: String, username: String, passwordText: String) {
        if (email.isBlank() || username.isBlank() || passwordText.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill out all fields.")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val success = repository.signUpWithEmail(email, username, passwordText)
            if (success) {
                val user = repository.currentUserState.value
                if (user != null) {
                    _uiState.value = AuthUiState.Authenticated(user)
                } else {
                    _uiState.value = AuthUiState.Unauthenticated
                }
            } else {
                _uiState.value = AuthUiState.Error("An account with this email already exists.")
            }
        }
    }

    fun handleGoogleAuth(email: String, displayName: String, avatarEmoji: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val success = repository.loginOrSignUpWithGoogle(email, displayName, avatarEmoji)
            if (success) {
                val user = repository.currentUserState.value
                if (user != null) {
                    _uiState.value = AuthUiState.Authenticated(user)
                } else {
                    _uiState.value = AuthUiState.Unauthenticated
                }
            } else {
                _uiState.value = AuthUiState.Error("Google Auth integration failed.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AuthUiState.Unauthenticated
        }
    }

    fun changeTargetLanguage(langCode: String) {
        viewModelScope.launch {
            repository.updateTargetLanguage(langCode)
            val user = repository.currentUserState.value
            if (user != null) {
                _uiState.value = AuthUiState.Authenticated(user)
            }
        }
    }

    fun updateUserProfile(username: String, email: String, dailyGoalMinutes: Int, avatarEmoji: String, targetLanguageCode: String) {
        viewModelScope.launch {
            repository.updateUserProfile(username, email, dailyGoalMinutes, avatarEmoji, targetLanguageCode)
            val user = repository.currentUserState.value
            if (user != null) {
                _uiState.value = AuthUiState.Authenticated(user)
            }
        }
    }

    fun awardXp(xp: Int, activityType: String) {
        viewModelScope.launch {
            repository.awardXp(xp, activityType)
            val user = repository.currentUserState.value
            if (user != null) {
                _uiState.value = AuthUiState.Authenticated(user)
            }
        }
    }

    fun updatePronunciationScore(score: Int) {
        viewModelScope.launch {
            repository.updatePronunciationScore(score)
            val user = repository.currentUserState.value
            if (user != null) {
                _uiState.value = AuthUiState.Authenticated(user)
            }
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            val lastUser = repository.currentUserState.value
            if (lastUser != null) {
                _uiState.value = AuthUiState.Authenticated(lastUser)
            } else {
                _uiState.value = AuthUiState.Unauthenticated
            }
        }
    }
}
