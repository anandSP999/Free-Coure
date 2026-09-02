package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.supabase.SupabaseService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PortalTab {
    EXPLORE, FAVORITES, SUBSCRIPTIONS, PROFILE
}

data class PortalUiState(
    val isLoggedIn: Boolean = false,
    val isAuthLoading: Boolean = false,
    val authError: String? = null,
    val isSignUpMode: Boolean = false,
    val currentUser: UserProfile? = null,
    val activeTab: PortalTab = PortalTab.EXPLORE,
    val banners: List<Banner> = emptyList(),
    val courses: List<Course> = emptyList(),
    val searchQuery: String = "",
    val favoriteCourseIds: Set<Long> = emptySet(),
    val enrollments: List<Enrollment> = emptyList(),
    val isLoadingCourses: Boolean = false,
    
    // Course Detail & Player View
    val activeCourse: Course? = null,
    val chapters: List<Chapter> = emptyList(),
    val activeChapter: Chapter? = null,
    val academyProfile: AcademyProfile? = null,
    val completedChapterIds: List<Long> = emptyList(),
    val isLoadingCourseDetail: Boolean = false,
    
    // Quiz View
    val currentQuiz: Quiz? = null,
    val selectedQuizOption: Int = -1,
    val quizResult: String? = null,
    val isQuizCorrect: Boolean? = null,
    val isQuizLoading: Boolean = false,
    
    // Payment Dialog
    val showPaymentModal: Boolean = false,
    val originalPrice: Int = 0,
    val discountedPrice: Int = 0,
    val couponCodeInput: String = "",
    val couponMessage: String? = null,
    val isCouponSuccess: Boolean? = null,
    val isApplyingCoupon: Boolean = false,
    
    // Certificate Dialog
    val showCertificateModal: Boolean = false,
    val certificateStudentName: String = "",
    val certificateId: String = "",
    
    // Tax Invoice Dialog
    val showInvoiceModal: Boolean = false,
    val selectedInvoiceEnrollment: Enrollment? = null,
    val invoiceAcademyProfile: AcademyProfile? = null,
    val invoiceTxnId: String = ""
)

class PortalViewModel(application: Application) : AndroidViewModel(application) {

    private val supabase = SupabaseService(application.applicationContext)

    private val _uiState = MutableStateFlow(PortalUiState())
    val uiState: StateFlow<PortalUiState> = _uiState.asStateFlow()

    init {
        checkInitialSession()
    }

    private fun checkInitialSession() {
        val savedUser = supabase.getSavedUser()
        if (savedUser != null) {
            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    currentUser = savedUser,
                    favoriteCourseIds = supabase.getFavorites(savedUser.email)
                )
            }
            loadDashboardData()
        }
    }

    // ---------------- AUTH ACTIONS ----------------
    fun toggleAuthMode() {
        _uiState.update { it.copy(isSignUpMode = !it.isSignUpMode, authError = null) }
    }

    fun handleAuth(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(authError = "Please enter both email and password") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAuthLoading = true, authError = null) }
            val result = if (_uiState.value.isSignUpMode) {
                supabase.signUp(email.trim(), pass)
            } else {
                supabase.signIn(email.trim(), pass)
            }

            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isLoggedIn = true,
                        isAuthLoading = false,
                        currentUser = user,
                        favoriteCourseIds = supabase.getFavorites(user.email),
                        authError = null
                    )
                }
                loadDashboardData()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isAuthLoading = false,
                        authError = error.message ?: "Authentication failed"
                    )
                }
            }
        }
    }

    fun logout() {
        supabase.logout()
        _uiState.update {
            PortalUiState(isLoggedIn = false, currentUser = null)
        }
    }

    // ---------------- DASHBOARD DATA ----------------
    fun selectTab(tab: PortalTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == PortalTab.EXPLORE && _uiState.value.activeCourse != null) {
            closeChapterView()
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadDashboardData() {
        val email = _uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCourses = true) }
            val banners = supabase.fetchBanners()
            val courses = supabase.fetchCourses()
            val enrollments = supabase.fetchEnrollments(email)
            val favs = supabase.getFavorites(email)

            _uiState.update {
                it.copy(
                    banners = banners,
                    courses = courses,
                    enrollments = enrollments,
                    favoriteCourseIds = favs,
                    isLoadingCourses = false
                )
            }
        }
    }

    fun toggleFavorite(courseId: Long) {
        val email = _uiState.value.currentUser?.email ?: return
        val updatedFavs = supabase.toggleFavorite(email, courseId)
        _uiState.update { it.copy(favoriteCourseIds = updatedFavs) }
    }

    fun shareCourse(context: Context, course: Course) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out: ${course.title}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Check out this amazing course on Gen-Z Gravity: ${course.title}\nLearn more: https://genzgravity.com/course/${course.id}"
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Course"))
        } catch (e: Exception) {
            Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- COURSE DETAIL & CHAPTER VIEW ----------------
    fun openCourse(course: Course) {
        val email = _uiState.value.currentUser?.email ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    activeCourse = course,
                    isLoadingCourseDetail = true,
                    activeChapter = null,
                    currentQuiz = null,
                    quizResult = null,
                    selectedQuizOption = -1,
                    showPaymentModal = false,
                    showCertificateModal = false
                )
            }

            val chapters = supabase.fetchChapters(course.id)
            val progress = supabase.fetchProgress(email, course.id)
            val academy = if (!course.academyEmail.isNullOrBlank()) {
                supabase.fetchAcademyProfile(course.academyEmail)
            } else {
                AcademyProfile(email = "admin@genzgravity.com", academyName = "Gen-Z Gravity Platform", teacherName = "Lead Instructor")
            }

            // Calculate total price of paid chapters
            var totalCoursePrice = 0
            chapters.forEach { ch ->
                if (ch.type.equals("Paid", ignoreCase = true)) {
                    totalCoursePrice += ch.price?.toIntOrNull() ?: 0
                }
            }

            // Check if user is enrolled
            val isEnrolled = _uiState.value.enrollments.any {
                it.courseTitle.equals(course.title, ignoreCase = true) && it.status.equals("Active", ignoreCase = true)
            }

            if (!isEnrolled && totalCoursePrice == 0 && chapters.isNotEmpty()) {
                // Auto enroll free course
                supabase.requestEnrollment(email, course.title, "Free", "Active")
                val updatedEnrollments = supabase.fetchEnrollments(email)
                _uiState.update { it.copy(enrollments = updatedEnrollments) }
            }

            _uiState.update {
                it.copy(
                    chapters = chapters,
                    completedChapterIds = progress,
                    academyProfile = academy,
                    originalPrice = totalCoursePrice,
                    discountedPrice = totalCoursePrice,
                    isLoadingCourseDetail = false,
                    // Auto-select first chapter for convenience
                    activeChapter = chapters.firstOrNull()
                )
            }
        }
    }

    fun closeChapterView() {
        _uiState.update {
            it.copy(
                activeCourse = null,
                activeChapter = null,
                currentQuiz = null,
                quizResult = null,
                selectedQuizOption = -1
            )
        }
    }

    fun playChapter(chapter: Chapter, context: Context) {
        val course = _uiState.value.activeCourse ?: return
        val isEnrolled = _uiState.value.enrollments.any {
            it.courseTitle.equals(course.title, ignoreCase = true) && it.status.equals("Active", ignoreCase = true)
        }
        val isLocked = !isEnrolled && chapter.type.equals("Paid", ignoreCase = true)

        if (isLocked) {
            Toast.makeText(context, "🔒 This video is locked. Unlock full course access below.", Toast.LENGTH_LONG).show()
            openPaymentModal()
            return
        }

        _uiState.update {
            it.copy(
                activeChapter = chapter,
                currentQuiz = null,
                quizResult = null,
                selectedQuizOption = -1
            )
        }
    }

    fun markActiveChapterComplete(context: Context) {
        val chapter = _uiState.value.activeChapter ?: return
        val course = _uiState.value.activeCourse ?: return
        val email = _uiState.value.currentUser?.email ?: return

        val currentCompleted = _uiState.value.completedChapterIds.toMutableList()
        if (!currentCompleted.contains(chapter.id)) {
            currentCompleted.add(chapter.id)
            _uiState.update { it.copy(completedChapterIds = currentCompleted) }
            viewModelScope.launch {
                supabase.saveProgress(email, course.id, currentCompleted)
            }
            Toast.makeText(context, "Chapter marked as completed! 🎉", Toast.LENGTH_SHORT).show()
        }
    }

    // ---------------- QUIZ FLOW ----------------
    fun loadQuizForActiveChapter() {
        val chapter = _uiState.value.activeChapter ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isQuizLoading = true, selectedQuizOption = -1, quizResult = null, isQuizCorrect = null) }
            val quiz = supabase.fetchQuiz(chapter.id)
            _uiState.update { it.copy(currentQuiz = quiz, isQuizLoading = false) }
        }
    }

    fun selectQuizOption(index: Int) {
        _uiState.update { it.copy(selectedQuizOption = index) }
    }

    fun submitQuiz(context: Context) {
        val quiz = _uiState.value.currentQuiz ?: return
        val selected = _uiState.value.selectedQuizOption
        if (selected == -1) {
            Toast.makeText(context, "Select an answer first!", Toast.LENGTH_SHORT).show()
            return
        }

        val isCorrect = selected == quiz.correctOption
        _uiState.update {
            it.copy(
                isQuizCorrect = isCorrect,
                quizResult = if (isCorrect) "✅ Correct! Excellent work." else "❌ Incorrect. Try again!"
            )
        }

        if (isCorrect) {
            markActiveChapterComplete(context)
        }
    }

    // ---------------- CERTIFICATE FLOW ----------------
    fun promptCertificate() {
        val user = _uiState.value.currentUser
        val name = if (!user?.fullName.isNullOrBlank() && user?.fullName != "Student") user!!.fullName else "Student Achiever"
        val uid = "GZG-" + (100000..999999).random()

        _uiState.update {
            it.copy(
                showCertificateModal = true,
                certificateStudentName = name,
                certificateId = uid
            )
        }
    }

    fun closeCertificateModal() {
        _uiState.update { it.copy(showCertificateModal = false) }
    }

    // ---------------- PAYMENT & COUPON FLOW ----------------
    fun openPaymentModal() {
        val orig = _uiState.value.originalPrice
        _uiState.update {
            it.copy(
                showPaymentModal = true,
                discountedPrice = orig,
                couponCodeInput = "",
                couponMessage = null,
                isCouponSuccess = null
            )
        }
    }

    fun closePaymentModal() {
        _uiState.update { it.copy(showPaymentModal = false) }
    }

    fun setCouponCodeInput(code: String) {
        _uiState.update { it.copy(couponCodeInput = code) }
    }

    fun applyCoupon() {
        val code = _uiState.value.couponCodeInput.trim()
        if (code.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isApplyingCoupon = true) }
            val coupon = supabase.validateCoupon(code)
            if (coupon != null && coupon.active) {
                val orig = _uiState.value.originalPrice
                val discount = (orig * coupon.discountPercent) / 100
                val newPrice = (orig - discount).coerceAtLeast(0)
                _uiState.update {
                    it.copy(
                        isApplyingCoupon = false,
                        discountedPrice = newPrice,
                        couponMessage = "✅ ${coupon.discountPercent}% Discount Applied!",
                        isCouponSuccess = true
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isApplyingCoupon = false,
                        discountedPrice = it.originalPrice,
                        couponMessage = "❌ Invalid Code",
                        isCouponSuccess = false
                    )
                }
            }
        }
    }

    fun openUpiPayment(context: Context) {
        val amount = _uiState.value.discountedPrice
        val upiId = "genzgravity@okaxis"
        val upiUri = Uri.parse("upi://pay?pa=$upiId&pn=GenZGravity&am=$amount&cu=INR")
        val intent = Intent(Intent.ACTION_VIEW, upiUri)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Please scan the QR code to complete payment via your UPI app.", Toast.LENGTH_LONG).show()
        }
    }

    fun confirmPaymentSent(context: Context) {
        val email = _uiState.value.currentUser?.email ?: return
        val course = _uiState.value.activeCourse ?: return
        val price = _uiState.value.discountedPrice.toString()

        _uiState.update { it.copy(showPaymentModal = false) }
        Toast.makeText(context, "⏳ Request Sent! Admin will verify payment and grant full course access.", Toast.LENGTH_LONG).show()

        viewModelScope.launch {
            supabase.requestEnrollment(email, course.title, price, "Pending")
            val user = _uiState.value.currentUser
            if (user != null) {
                supabase.saveProfile(email, user.fullName, user.mobile)
            }
            val enrollments = supabase.fetchEnrollments(email)
            _uiState.update { it.copy(enrollments = enrollments) }
        }
    }

    // ---------------- TAX INVOICE FLOW ----------------
    fun openInvoiceData(enrollment: Enrollment) {
        viewModelScope.launch {
            val academy = supabase.fetchAcademyProfile("academy@genzgravity.com")
            val txnId = "TXN-${enrollment.id}${(100..999).random()}"
            _uiState.update {
                it.copy(
                    showInvoiceModal = true,
                    selectedInvoiceEnrollment = enrollment,
                    invoiceAcademyProfile = academy,
                    invoiceTxnId = txnId
                )
            }
        }
    }

    fun closeInvoiceModal() {
        _uiState.update { it.copy(showInvoiceModal = false) }
    }

    // ---------------- PROFILE UPDATE ----------------
    fun updateProfile(fullName: String, mobile: String, context: Context) {
        val email = _uiState.value.currentUser?.email ?: return
        if (fullName.isBlank() || mobile.isBlank()) {
            Toast.makeText(context, "Please enter valid name and mobile number", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val success = supabase.saveProfile(email, fullName.trim(), mobile.trim())
            if (success) {
                val updatedUser = UserProfile(email, fullName.trim(), mobile.trim())
                _uiState.update { it.copy(currentUser = updatedUser) }
                Toast.makeText(context, "Profile Updated Successfully! 💾", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
