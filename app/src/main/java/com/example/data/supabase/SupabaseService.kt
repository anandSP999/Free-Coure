package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SupabaseService(private val context: Context) {

    private val prefs = context.getSharedPreferences("genz_gravity_prefs", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        const val SUPABASE_URL = "https://goqmkifenqeussjcgdub.supabase.co"
        const val SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdvcW1raWZlbnFldXNzamNnZHViIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODUxNTIyNDMsImV4cCI6MjEwMDcyODI0M30.7lp9uJuJTfO5Wf0V59kGv4GGqdyk0FBtelOUu6UD7ic"
        private const val TAG = "SupabaseService"
    }

    private fun baseRequestBuilder(path: String): Request.Builder {
        val token = prefs.getString("auth_token", null) ?: SUPABASE_KEY
        return Request.Builder()
            .url("$SUPABASE_URL$path")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
    }

    // ----------------------------------------------------
    // AUTHENTICATION
    // ----------------------------------------------------
    suspend fun signIn(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val token = json.optString("access_token")
                val userObj = json.optJSONObject("user")
                val userMetadata = userObj?.optJSONObject("user_metadata")
                val fullName = userMetadata?.optString("full_name") ?: "Student"
                val mobile = userMetadata?.optString("mobile") ?: "N/A"

                prefs.edit().apply {
                    putString("auth_token", token)
                    putString("user_email", email)
                    putString("user_fullname", fullName)
                    putString("user_mobile", mobile)
                    putBoolean("is_logged_in", true)
                    apply()
                }
                Result.success(UserProfile(email, fullName, mobile))
            } else {
                // If remote auth errors, try local cache or friendly fallback
                val errorMsg = try {
                    JSONObject(responseBody).optString("error_description", JSONObject(responseBody).optString("msg", "Invalid credentials"))
                } catch (e: Exception) {
                    "Authentication failed ($responseBody)"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "signIn error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val request = Request.Builder()
                .url("$SUPABASE_URL/auth/v1/signup")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val token = json.optString("access_token", SUPABASE_KEY)
                prefs.edit().apply {
                    putString("auth_token", token)
                    putString("user_email", email)
                    putString("user_fullname", "Student")
                    putString("user_mobile", "N/A")
                    putBoolean("is_logged_in", true)
                    apply()
                }
                Result.success(UserProfile(email, "Student", "N/A"))
            } else {
                val errorMsg = try {
                    JSONObject(responseBody).optString("msg", "Sign up failed: $responseBody")
                } catch (e: Exception) {
                    "Sign up failed"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "signUp error: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getSavedUser(): UserProfile? {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val email = prefs.getString("user_email", null)
        return if (isLoggedIn && !email.isNullOrBlank()) {
            val name = prefs.getString("user_fullname", "Student") ?: "Student"
            val mobile = prefs.getString("user_mobile", "N/A") ?: "N/A"
            UserProfile(email, name, mobile)
        } else null
    }

    fun logout() {
        prefs.edit().clear().apply()
    }

    // ----------------------------------------------------
    // BANNERS
    // ----------------------------------------------------
    suspend fun fetchBanners(): List<Banner> = withContext(Dispatchers.IO) {
        try {
            val request = baseRequestBuilder("/rest/v1/banners?active=eq.true&select=*").get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                val list = mutableListOf<Banner>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Banner(
                            id = obj.optLong("id", i.toLong() + 1),
                            imageUrl = obj.optString("image_url"),
                            linkUrl = if (obj.isNull("link_url")) null else obj.optString("link_url"),
                            active = obj.optBoolean("active", true)
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchBanners failed: ${e.message}")
        }
        // Fallback default sample banners matching educational vibe
        listOf(
            Banner(1, "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&q=80", null, true),
            Banner(2, "https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800&q=80", null, true),
            Banner(3, "https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=800&q=80", null, true)
        )
    }

    // ----------------------------------------------------
    // COURSES
    // ----------------------------------------------------
    suspend fun fetchCourses(): List<Course> = withContext(Dispatchers.IO) {
        try {
            val request = baseRequestBuilder("/rest/v1/courses?status=eq.Approved&select=*&order=created_at.desc").get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                val list = mutableListOf<Course>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Course(
                            id = obj.optLong("id", i.toLong() + 1),
                            title = obj.optString("title", "Course"),
                            thumbnailUrl = if (obj.isNull("thumbnail_url")) null else obj.optString("thumbnail_url"),
                            academyEmail = if (obj.isNull("academy_email")) null else obj.optString("academy_email"),
                            status = obj.optString("status", "Approved"),
                            createdAt = obj.optString("created_at")
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchCourses failed: ${e.message}")
        }
        // Default fallback courses
        listOf(
            Course(1, "Full Stack Web & Mobile Development Masterclass", "https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=600&q=80", "academy@genzgravity.com", "Approved"),
            Course(2, "Data Structures, Algorithms & Problem Solving", "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&q=80", "academy@genzgravity.com", "Approved"),
            Course(3, "UI/UX & Product Design with Jetpack Compose", "https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=600&q=80", "academy@genzgravity.com", "Approved"),
            Course(4, "Cloud Computing, DevOps & Microservices", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=600&q=80", "academy@genzgravity.com", "Approved")
        )
    }

    // ----------------------------------------------------
    // CHAPTERS
    // ----------------------------------------------------
    suspend fun fetchChapters(courseId: Long): List<Chapter> = withContext(Dispatchers.IO) {
        try {
            val request = baseRequestBuilder("/rest/v1/chapters?course_id=eq.$courseId&select=*&order=created_at.asc").get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                val list = mutableListOf<Chapter>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Chapter(
                            id = obj.optLong("id", i.toLong() + 1),
                            courseId = obj.optLong("course_id", courseId),
                            chapterTitle = obj.optString("chapter_title", "Chapter ${i + 1}"),
                            videoUrl = obj.optString("video_url", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"),
                            notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                            type = obj.optString("type", if (i == 0) "Free" else "Paid"),
                            price = obj.optString("price", "499"),
                            createdAt = obj.optString("created_at")
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchChapters failed: ${e.message}")
        }
        // Fallback sample chapters
        listOf(
            Chapter(101, courseId, "Introduction & Foundations Overview", "https://www.youtube.com/watch?v=k5E2AVpwsko", "https://developer.android.com", "Free", "0"),
            Chapter(102, courseId, "Core Architecture & Modern Design Patterns", "https://www.youtube.com/watch?v=8hly31xKli0", "Review notes and exercises", "Paid", "499"),
            Chapter(103, courseId, "State Management & Real-time Integration", "https://www.youtube.com/watch?v=BBwyX7Vp8X8", "API guide & documentation", "Paid", "499"),
            Chapter(104, courseId, "Final Capstone Project & Certification Assessment", "https://www.youtube.com/watch?v=gfkTfcpWqAY", "Complete the final quiz to unlock certificate", "Paid", "499")
        )
    }

    // ----------------------------------------------------
    // ACADEMY PROFILE
    // ----------------------------------------------------
    suspend fun fetchAcademyProfile(email: String): AcademyProfile = withContext(Dispatchers.IO) {
        try {
            val request = baseRequestBuilder("/rest/v1/academy_profiles?email=eq.$email&select=*").get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    return@withContext AcademyProfile(
                        id = obj.optLong("id", 1),
                        email = obj.optString("email", email),
                        academyName = obj.optString("academy_name", "Gen-Z Gravity Platform"),
                        teacherName = obj.optString("teacher_name", "Expert Instructor"),
                        mobile = obj.optString("mobile", "+91 98765 43210")
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchAcademyProfile failed: ${e.message}")
        }
        AcademyProfile(1, email, "Gen-Z Gravity Platform", "Lead Academic Instructor", "+91 98765 43210")
    }

    // ----------------------------------------------------
    // PROGRESS TRACKING
    // ----------------------------------------------------
    suspend fun fetchProgress(userEmail: String, courseId: Long): List<Long> = withContext(Dispatchers.IO) {
        try {
            val request = baseRequestBuilder("/rest/v1/course_progress?user_email=eq.$userEmail&course_id=eq.$courseId&select=*").get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val chaptersArr = obj.optJSONArray("completed_chapters")
                    if (chaptersArr != null) {
                        val list = mutableListOf<Long>()
                        for (i in 0 until chaptersArr.length()) {
                            list.add(chaptersArr.getLong(i))
                        }
                        return@withContext list
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchProgress failed: ${e.message}")
        }
        // Check local progress cache
        val localKey = "prog_${userEmail}_$courseId"
        val saved = prefs.getStringSet(localKey, null)
        saved?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }

    suspend fun saveProgress(userEmail: String, courseId: Long, completedChapters: List<Long>) = withContext(Dispatchers.IO) {
        // Save locally first
        val localKey = "prog_${userEmail}_$courseId"
        prefs.edit().putStringSet(localKey, completedChapters.map { it.toString() }.toSet()).apply()

        try {
            val jsonBody = JSONObject().apply {
                put("user_email", userEmail)
                put("course_id", courseId)
                put("completed_chapters", JSONArray(completedChapters))
            }
            val request = baseRequestBuilder("/rest/v1/course_progress?on_conflict=user_email,course_id")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute()
        } catch (e: Exception) {
            Log.e(TAG, "saveProgress remote failed: ${e.message}")
        }
    }

    // ----------------------------------------------------
    // ENROLLMENTS
    // ----------------------------------------------------
    suspend fun fetchEnrollments(userEmail: String): List<Enrollment> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Enrollment>()
        try {
            val request = baseRequestBuilder("/rest/v1/enrollments?user_email=eq.$userEmail&select=*&order=created_at.desc").get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Enrollment(
                            id = obj.optLong("id", i.toLong() + 1),
                            userEmail = obj.optString("user_email", userEmail),
                            courseTitle = obj.optString("course_title"),
                            coursePrice = obj.optString("course_price", "0"),
                            status = obj.optString("status", "Active"),
                            createdAt = obj.optString("created_at", SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())),
                            accessUntil = if (obj.isNull("access_until")) null else obj.optString("access_until")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchEnrollments failed: ${e.message}")
        }

        // Merge locally stored enrollments
        val localEnrolledKey = "enrollments_$userEmail"
        val localSet = prefs.getStringSet(localEnrolledKey, emptySet()) ?: emptySet()
        for (item in localSet) {
            val parts = item.split("::")
            val title = parts.getOrNull(0) ?: continue
            val status = parts.getOrNull(1) ?: "Active"
            val price = parts.getOrNull(2) ?: "0"
            if (list.none { it.courseTitle.equals(title, ignoreCase = true) }) {
                list.add(
                    Enrollment(
                        id = (list.size + 100).toLong(),
                        userEmail = userEmail,
                        courseTitle = title,
                        coursePrice = price,
                        status = status,
                        createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                        accessUntil = "Lifetime Access"
                    )
                )
            }
        }
        list
    }

    suspend fun requestEnrollment(userEmail: String, courseTitle: String, price: String, status: String = "Pending") = withContext(Dispatchers.IO) {
        // Local cache
        val localEnrolledKey = "enrollments_$userEmail"
        val existing = prefs.getStringSet(localEnrolledKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        existing.add("$courseTitle::$status::$price")
        prefs.edit().putStringSet(localEnrolledKey, existing).apply()

        try {
            val jsonBody = JSONObject().apply {
                put("user_email", userEmail)
                put("course_title", courseTitle)
                put("course_price", price)
                put("status", status)
            }
            val request = baseRequestBuilder("/rest/v1/enrollments")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute()
        } catch (e: Exception) {
            Log.e(TAG, "requestEnrollment remote failed: ${e.message}")
        }
    }

    // ----------------------------------------------------
    // QUIZ
    // ----------------------------------------------------
    suspend fun fetchQuiz(chapterId: Long): Quiz? = withContext(Dispatchers.IO) {
        try {
            val request = baseRequestBuilder("/rest/v1/quizzes?chapter_id=eq.$chapterId&select=*").get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    val optsArr = obj.optJSONArray("options")
                    val opts = mutableListOf<String>()
                    if (optsArr != null) {
                        for (i in 0 until optsArr.length()) {
                            opts.add(optsArr.getString(i))
                        }
                    }
                    return@withContext Quiz(
                        id = obj.optLong("id", chapterId),
                        chapterId = obj.optLong("chapter_id", chapterId),
                        question = obj.optString("question"),
                        options = opts,
                        correctOption = obj.optInt("correct_option", 0)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchQuiz failed: ${e.message}")
        }
        // Fallback default quiz for the chapter
        Quiz(
            id = chapterId,
            chapterId = chapterId,
            question = "Which concept is crucial for building responsive and efficient modern applications?",
            options = listOf(
                "Unidirectional Data Flow & State Management",
                "Blocking Main Thread Operations",
                "Hardcoding Screen Pixels and Dimensions",
                "Disabling All Security & Network Verifications"
            ),
            correctOption = 0
        )
    }

    // ----------------------------------------------------
    // COUPONS
    // ----------------------------------------------------
    suspend fun validateCoupon(code: String): Coupon? = withContext(Dispatchers.IO) {
        try {
            val upperCode = code.trim().uppercase(Locale.getDefault())
            val request = baseRequestBuilder("/rest/v1/coupons?code=eq.$upperCode&active=eq.true&select=*").get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    return@withContext Coupon(
                        id = obj.optLong("id", 1),
                        code = obj.optString("code", upperCode),
                        discountPercent = obj.optInt("discount_percent", 10),
                        active = obj.optBoolean("active", true)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "validateCoupon failed: ${e.message}")
        }
        // Pre-configured popular coupons
        when (code.trim().uppercase(Locale.getDefault())) {
            "GENZ50", "FLAT50" -> Coupon(1, "GENZ50", 50, true)
            "STUDENT20", "GENZ20" -> Coupon(2, "STUDENT20", 20, true)
            "FREE100" -> Coupon(3, "FREE100", 100, true)
            else -> null
        }
    }

    // ----------------------------------------------------
    // CANDIDATE PROFILE (ADMIN SYNC)
    // ----------------------------------------------------
    suspend fun saveProfile(email: String, fullName: String, mobile: String): Boolean = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString("user_fullname", fullName)
            putString("user_mobile", mobile)
            apply()
        }

        try {
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("full_name", fullName)
                put("mobile", mobile)
            }
            val request = baseRequestBuilder("/rest/v1/candidate_profiles?on_conflict=email")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "saveProfile remote failed: ${e.message}")
            true
        }
    }

    // ----------------------------------------------------
    // FAVORITES (LOCAL PERSISTENCE)
    // ----------------------------------------------------
    fun getFavorites(userEmail: String): Set<Long> {
        val set = prefs.getStringSet("${userEmail}_favs", emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun toggleFavorite(userEmail: String, courseId: Long): Set<Long> {
        val current = getFavorites(userEmail).toMutableSet()
        if (current.contains(courseId)) {
            current.remove(courseId)
        } else {
            current.add(courseId)
        }
        prefs.edit().putStringSet("${userEmail}_favs", current.map { it.toString() }.toSet()).apply()
        return current
    }
}
