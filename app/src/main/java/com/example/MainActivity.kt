package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CertificateDialog
import com.example.ui.components.PaymentDialog
import com.example.ui.components.TaxInvoiceDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.PortalTab
import com.example.viewmodel.PortalViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PortalViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PortalApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalApp(viewModel: PortalViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (!uiState.isLoggedIn) {
        AuthScreen(
            isSignUpMode = uiState.isSignUpMode,
            isLoading = uiState.isAuthLoading,
            errorMessage = uiState.authError,
            onToggleAuthMode = { viewModel.toggleAuthMode() },
            onSubmitAuth = { email, pass -> viewModel.handleAuth(email, pass) },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Gen-Z Gravity",
                            fontWeight = FontWeight.ExtraBold,
                            color = BluePrimary,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        val email = uiState.currentUser?.email
                        if (!email.isNullOrBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = BlueLight,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Text(
                                    text = email.substringBefore("@"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BlueDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = SurfaceWhite,
                    tonalElevation = 6.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavigationBarItem(
                        selected = uiState.activeTab == PortalTab.EXPLORE && uiState.activeCourse == null,
                        onClick = { viewModel.selectTab(PortalTab.EXPLORE) },
                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explore") },
                        label = { Text("Explore") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = BlueLight
                        ),
                        modifier = Modifier.testTag("nav_explore")
                    )

                    NavigationBarItem(
                        selected = uiState.activeTab == PortalTab.FAVORITES,
                        onClick = { viewModel.selectTab(PortalTab.FAVORITES) },
                        icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                        label = { Text("Favorites") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = BlueLight
                        ),
                        modifier = Modifier.testTag("nav_favorites")
                    )

                    NavigationBarItem(
                        selected = uiState.activeTab == PortalTab.SUBSCRIPTIONS,
                        onClick = { viewModel.selectTab(PortalTab.SUBSCRIPTIONS) },
                        icon = { Icon(Icons.Default.CheckCircle, contentDescription = "My Learning") },
                        label = { Text("My Learning") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = BlueLight
                        ),
                        modifier = Modifier.testTag("nav_subscriptions")
                    )

                    NavigationBarItem(
                        selected = uiState.activeTab == PortalTab.PROFILE,
                        onClick = { viewModel.selectTab(PortalTab.PROFILE) },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            indicatorColor = BlueLight
                        ),
                        modifier = Modifier.testTag("nav_profile")
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BackgroundLight)
            ) {
                // If a course detail is opened
                if (uiState.activeCourse != null) {
                    val course = uiState.activeCourse!!
                    val isEnrolled = uiState.enrollments.any {
                        it.courseTitle.equals(course.title, ignoreCase = true) && it.status.equals("Active", ignoreCase = true)
                    }

                    CourseDetailScreen(
                        course = course,
                        chapters = uiState.chapters,
                        activeChapter = uiState.activeChapter,
                        academyProfile = uiState.academyProfile,
                        completedChapterIds = uiState.completedChapterIds,
                        isEnrolled = isEnrolled,
                        totalPrice = uiState.originalPrice,
                        watermarkEmail = uiState.currentUser?.email ?: "user@example.com",
                        currentQuiz = uiState.currentQuiz,
                        selectedQuizOption = uiState.selectedQuizOption,
                        quizResult = uiState.quizResult,
                        isQuizCorrect = uiState.isQuizCorrect,
                        isQuizLoading = uiState.isQuizLoading,
                        onBack = { viewModel.closeChapterView() },
                        onChapterClick = { ch -> viewModel.playChapter(ch, context) },
                        onMarkChapterComplete = { viewModel.markActiveChapterComplete(context) },
                        onLoadQuiz = { viewModel.loadQuizForActiveChapter() },
                        onSelectQuizOption = { idx -> viewModel.selectQuizOption(idx) },
                        onSubmitQuiz = { viewModel.submitQuiz(context) },
                        onOpenPayment = { viewModel.openPaymentModal() },
                        onOpenCertificate = { viewModel.promptCertificate() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Main Tabs
                    when (uiState.activeTab) {
                        PortalTab.EXPLORE -> {
                            ExploreScreen(
                                banners = uiState.banners,
                                courses = uiState.courses,
                                enrollments = uiState.enrollments,
                                favoriteCourseIds = uiState.favoriteCourseIds,
                                searchQuery = uiState.searchQuery,
                                isLoading = uiState.isLoadingCourses,
                                onSearchQueryChange = { q -> viewModel.setSearchQuery(q) },
                                onCourseClick = { c -> viewModel.openCourse(c) },
                                onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                onShareCourse = { ctx, c -> viewModel.shareCourse(ctx, c) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        PortalTab.FAVORITES -> {
                            FavoritesScreen(
                                allCourses = uiState.courses,
                                favoriteCourseIds = uiState.favoriteCourseIds,
                                enrollments = uiState.enrollments,
                                onCourseClick = { c -> viewModel.openCourse(c) },
                                onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                onShareCourse = { ctx, c -> viewModel.shareCourse(ctx, c) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        PortalTab.SUBSCRIPTIONS -> {
                            SubscriptionsScreen(
                                enrollments = uiState.enrollments,
                                allCourses = uiState.courses,
                                onCourseClick = { c -> viewModel.openCourse(c) },
                                onOpenInvoice = { en -> viewModel.openInvoiceData(en) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        PortalTab.PROFILE -> {
                            ProfileScreen(
                                userProfile = uiState.currentUser,
                                onUpdateProfile = { name, mob, ctx -> viewModel.updateProfile(name, mob, ctx) },
                                onLogout = { viewModel.logout() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // ---------------- MODAL OVERLAYS ----------------

                // Secure Payment Dialog
                if (uiState.showPaymentModal) {
                    PaymentDialog(
                        originalPrice = uiState.originalPrice,
                        discountedPrice = uiState.discountedPrice,
                        couponCode = uiState.couponCodeInput,
                        couponMessage = uiState.couponMessage,
                        isCouponSuccess = uiState.isCouponSuccess,
                        isApplyingCoupon = uiState.isApplyingCoupon,
                        onCouponCodeChanged = { viewModel.setCouponCodeInput(it) },
                        onApplyCoupon = { viewModel.applyCoupon() },
                        onPayViaUpi = { viewModel.openUpiPayment(context) },
                        onConfirmPaid = { viewModel.confirmPaymentSent(context) },
                        onDismiss = { viewModel.closePaymentModal() }
                    )
                }

                // Verified Certificate Dialog
                if (uiState.showCertificateModal && uiState.activeCourse != null) {
                    CertificateDialog(
                        studentName = uiState.certificateStudentName,
                        courseTitle = uiState.activeCourse!!.title,
                        certificateId = uiState.certificateId,
                        onDismiss = { viewModel.closeCertificateModal() }
                    )
                }

                // Tax Invoice Dialog
                if (uiState.showInvoiceModal && uiState.selectedInvoiceEnrollment != null) {
                    TaxInvoiceDialog(
                        enrollment = uiState.selectedInvoiceEnrollment!!,
                        userProfile = uiState.currentUser,
                        academyProfile = uiState.invoiceAcademyProfile,
                        txnId = uiState.invoiceTxnId,
                        onDismiss = { viewModel.closeInvoiceModal() }
                    )
                }
            }
        }
    }
}
