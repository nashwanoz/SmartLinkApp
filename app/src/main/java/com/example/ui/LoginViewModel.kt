package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    SIGN_IN,
    SIGN_UP,
    FORGOT_PASSWORD,
    LOGGED_IN
}

data class UserProfile(
    val name: String,
    val email: String,
    val joinedDate: String = "August 2026",
    val role: String = "عضو مميز / Premium Member"
)

data class LoginUiState(
    val authMode: AuthMode = AuthMode.SIGN_IN,
    val isArabic: Boolean = true,
    val isDarkTheme: Boolean = false,
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val resetEmailSent: Boolean = false,
    val currentUser: UserProfile? = null
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun toggleLanguage() {
        _uiState.update { it.copy(isArabic = !it.isArabic, errorMessage = null, successMessage = null) }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun setAuthMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                authMode = mode,
                errorMessage = null,
                successMessage = null,
                resetEmailSent = false
            )
        }
    }

    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name, errorMessage = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email.trim(), errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, errorMessage = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update { it.copy(confirmPassword = confirmPassword, errorMessage = null) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun toggleRememberMe(value: Boolean) {
        _uiState.update { it.copy(rememberMe = value) }
    }

    fun fillDemoCredentials() {
        _uiState.update {
            it.copy(
                fullName = if (it.isArabic) "أحمد المنصور" else "Ahmed Mansour",
                email = "demo.user@example.com",
                password = "Password123!",
                confirmPassword = "Password123!",
                errorMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun authenticate() {
        val state = _uiState.value
        val isAr = state.isArabic

        if (state.authMode == AuthMode.SIGN_UP) {
            if (state.fullName.isBlank()) {
                _uiState.update {
                    it.copy(errorMessage = if (isAr) "يرجى كتابة الاسم الكامل" else "Please enter your full name")
                }
                return
            }
        }

        if (state.email.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = if (isAr) "يرجى إدخال البريد الإلكتروني" else "Please enter your email address")
            }
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update {
                it.copy(errorMessage = if (isAr) "صيغة البريد الإلكتروني غير صحيحة" else "Invalid email format")
            }
            return
        }

        if (state.password.length < 6) {
            _uiState.update {
                it.copy(errorMessage = if (isAr) "كلمة المرور يجب أن لا تقل عن 6 خانات" else "Password must be at least 6 characters")
            }
            return
        }

        if (state.authMode == AuthMode.SIGN_UP && state.password != state.confirmPassword) {
            _uiState.update {
                it.copy(errorMessage = if (isAr) "كلمتا المرور غير متطابقتين" else "Passwords do not match")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(1200) // Simulate fast network authentication

            val displayName = if (state.fullName.isNotBlank()) state.fullName else state.email.substringBefore("@").replaceFirstChar { it.uppercase() }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    authMode = AuthMode.LOGGED_IN,
                    currentUser = UserProfile(
                        name = displayName,
                        email = state.email,
                        role = if (isAr) "مستخدم مسجّل" else "Verified User"
                    ),
                    successMessage = if (isAr) "تم تسجيل الدخول بنجاح!" else "Successfully signed in!"
                )
            }
        }
    }

    fun sendPasswordReset() {
        val state = _uiState.value
        val isAr = state.isArabic

        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update {
                it.copy(errorMessage = if (isAr) "يرجى إدخال بريد إلكتروني صالح لاستعادة الحساب" else "Please enter a valid recovery email")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(1000)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    resetEmailSent = true,
                    successMessage = if (isAr) "تم إرسال رابط إعادة تعيين كلمة المرور إلى بريدك!" else "Reset link sent to your email!"
                )
            }
        }
    }

    fun authenticateWithBiometrics() {
        val isAr = _uiState.value.isArabic
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(800)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    authMode = AuthMode.LOGGED_IN,
                    currentUser = UserProfile(
                        name = if (isAr) "مستخدم البصمة الذكية" else "Biometric User",
                        email = "biometric.auth@example.com",
                        role = if (isAr) "توثيق بيومتري آمن" else "Biometric Verified"
                    ),
                    successMessage = if (isAr) "تم التحقق بالبصمة بنجاح!" else "Biometric verification successful!"
                )
            }
        }
    }

    fun authenticateWithGoogle() {
        val isAr = _uiState.value.isArabic
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            delay(1000)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    authMode = AuthMode.LOGGED_IN,
                    currentUser = UserProfile(
                        name = if (isAr) "مستخدم Google" else "Google Account User",
                        email = "user.google@gmail.com",
                        role = if (isAr) "حساب Google موثق" else "Google Verified"
                    ),
                    successMessage = if (isAr) "تم الدخول بواسطة Google!" else "Signed in with Google!"
                )
            }
        }
    }

    fun logout() {
        _uiState.update {
            it.copy(
                authMode = AuthMode.SIGN_IN,
                currentUser = null,
                password = "",
                confirmPassword = "",
                errorMessage = null,
                successMessage = null,
                resetEmailSent = false
            )
        }
    }
}
