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

    /**
     * Build request with public Anon Key so expired user tokens NEVER cause 401 on public endpoints.
     */
    private fun publicRequestBuilder(path: String): Request.Builder {
        return Request.Builder()
            .url("$SUPABASE_URL$path")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
    }

    /**
     * Request builder for user-specific operations with fallback to anon key.
     */
    private fun userRequestBuilder(path: String): Request.Builder {
        val token = prefs.getString("auth_token", null) ?: SUPABASE_KEY
        return Request.Builder()
            .url("$SUPABASE_URL$path")
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
    }

    // ----------------------------------------------------
    // AUTHENTICATION & MULTI-ACCOUNT MANAGEMENT
    // ----------------------------------------------------
    suspend fun signIn(email: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase(Locale.getDefault())
        try {
            val jsonBody = JSONObject().apply {
                put("email", trimmedEmail)
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
                val refreshToken = json.optString("refresh_token")
                val userObj = json.optJSONObject("user")
                val userMetadata = userObj?.optJSONObject("user_metadata")
                val fullName = userMetadata?.optString("full_name") ?: "Student"
                val mobile = userMetadata?.optString("mobile") ?: "N/A"

                val profile = UserProfile(trimmedEmail, fullName, mobile)
                saveActiveUser(profile, token, refreshToken)
                addOrUpdateSavedAccount(profile, token, refreshToken)

                Result.success(profile)
            } else {
                // If offline or remote auth error, check if this account exists in saved accounts
                val savedAccounts = getSavedAccounts()
                val existing = savedAccounts.find { it.email.equals(trimmedEmail, ignoreCase = true) }
                if (existing != null) {
                    saveActiveUser(existing, null, null)
                    return@withContext Result.success(existing)
                }

                val errorMsg = try {
                    JSONObject(responseBody).optString("error_description", JSONObject(responseBody).optString("msg", "Invalid email or password"))
                } catch (e: Exception) {
                    "Authentication failed. Please verify credentials."
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "signIn error: ${e.message}", e)
            // Allow offline login if account was previously saved
            val savedAccounts = getSavedAccounts()
            val existing = savedAccounts.find { it.email.equals(trimmedEmail, ignoreCase = true) }
            if (existing != null) {
                saveActiveUser(existing, null, null)
                Result.success(existing)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun signUp(email: String, password: String, fullName: String = "Student", mobile: String = "N/A"): Result<UserProfile> = withContext(Dispatchers.IO) {
        val trimmedEmail = email.trim().lowercase(Locale.getDefault())
        val trimmedName = if (fullName.isBlank()) "Student" else fullName.trim()
        val trimmedMobile = if (mobile.isBlank()) "N/A" else mobile.trim()

        try {
            val jsonBody = JSONObject().apply {
                put("email", trimmedEmail)
                put("password", password)
                put("data", JSONObject().apply {
                    put("full_name", trimmedName)
                    put("mobile", trimmedMobile)
                })
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
                val refreshToken = json.optString("refresh_token", "")

                val profile = UserProfile(trimmedEmail, trimmedName, trimmedMobile)
                saveActiveUser(profile, token, refreshToken)
                addOrUpdateSavedAccount(profile, token, refreshToken)

                // Also persist to candidate_profiles table
                saveProfile(trimmedEmail, trimmedName, trimmedMobile)

                Result.success(profile)
            } else {
                val errorMsg = try {
                    JSONObject(responseBody).optString("msg", JSONObject(responseBody).optString("error_description", "Sign up failed"))
                } catch (e: Exception) {
                    "Sign up failed ($responseBody)"
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "signUp error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun saveActiveUser(profile: UserProfile, token: String?, refreshToken: String?) {
        prefs.edit().apply {
            putString("user_email", profile.email)
            putString("user_fullname", profile.fullName)
            putString("user_mobile", profile.mobile)
            putBoolean("is_logged_in", true)
            if (!token.isNullOrBlank()) putString("auth_token", token)
            if (!refreshToken.isNullOrBlank()) putString("refresh_token", refreshToken)
            apply()
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

    fun getSavedAccounts(): List<UserProfile> {
        val jsonStr = prefs.getString("saved_accounts_list", null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<UserProfile>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UserProfile(
                        email = obj.getString("email"),
                        fullName = obj.optString("fullName", "Student"),
                        mobile = obj.optString("mobile", "N/A")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun addOrUpdateSavedAccount(profile: UserProfile, token: String?, refreshToken: String?) {
        val list = getSavedAccounts().toMutableList()
        list.removeAll { it.email.equals(profile.email, ignoreCase = true) }
        list.add(0, profile)

        val arr = JSONArray()
        for (acc in list) {
            arr.put(JSONObject().apply {
                put("email", acc.email)
                put("fullName", acc.fullName)
                put("mobile", acc.mobile)
            })
        }
        prefs.edit().putString("saved_accounts_list", arr.toString()).apply()
    }

    fun switchToAccount(email: String): UserProfile? {
        val accounts = getSavedAccounts()
        val target = accounts.find { it.email.equals(email, ignoreCase = true) } ?: return null
        saveActiveUser(target, null, null)
        return target
    }

    fun removeSavedAccount(email: String) {
        val list = getSavedAccounts().toMutableList()
        list.removeAll { it.email.equals(email, ignoreCase = true) }
        val arr = JSONArray()
        for (acc in list) {
            arr.put(JSONObject().apply {
                put("email", acc.email)
                put("fullName", acc.fullName)
                put("mobile", acc.mobile)
            })
        }
        prefs.edit().putString("saved_accounts_list", arr.toString()).apply()

        // If removed account was the active one, switch to next or logout
        if (prefs.getString("user_email", "").equals(email, ignoreCase = true)) {
            if (list.isNotEmpty()) {
                saveActiveUser(list[0], null, null)
            } else {
                logout()
            }
        }
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("user_email")
            .remove("auth_token")
            .apply()
    }

    // ----------------------------------------------------
    // BANNERS
    // ----------------------------------------------------
    suspend fun fetchBanners(): List<Banner> = withContext(Dispatchers.IO) {
        try {
            val request = publicRequestBuilder("/rest/v1/banners?active=eq.true&select=*").get().build()
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
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchBanners failed: ${e.message}")
        }
        emptyList()
    }

    // ----------------------------------------------------
    // COURSES (REAL ADMIN COURSES ONLY)
    // ----------------------------------------------------
    suspend fun fetchCourses(): List<Course> = withContext(Dispatchers.IO) {
        try {
            val request = publicRequestBuilder("/rest/v1/courses?select=*&order=created_at.desc").get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val array = JSONArray(body)
                val list = mutableListOf<Course>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val status = obj.optString("status", "Approved")
                    // Show all courses posted by admin that aren't marked Archived or Deleted
                    if (!status.equals("Archived", ignoreCase = true) && !status.equals("Deleted", ignoreCase = true)) {
                        list.add(
                            Course(
                                id = obj.optLong("id", i.toLong() + 1),
                                title = obj.optString("title", "Course"),
                                thumbnailUrl = if (obj.isNull("thumbnail_url")) null else obj.optString("thumbnail_url"),
                                academyEmail = if (obj.isNull("academy_email")) null else obj.optString("academy_email"),
                                status = status,
                                createdAt = obj.optString("created_at")
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) {
                    cacheCourses(list)
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchCourses failed: ${e.message}")
        }
        // Return cached real courses, NEVER fake mock data
        getCachedCourses()
    }

    private fun cacheCourses(courses: List<Course>) {
        val arr = JSONArray()
        for (c in courses) {
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("thumbnail_url", c.thumbnailUrl ?: "")
                put("academy_email", c.academyEmail ?: "")
                put("status", c.status)
                put("created_at", c.createdAt ?: "")
            })
        }
        prefs.edit().putString("cached_courses_v2", arr.toString()).apply()
    }

    fun getCachedCourses(): List<Course> {
        val json = prefs.getString("cached_courses_v2", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Course>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Course(
                        id = obj.optLong("id", i.toLong() + 1),
                        title = obj.optString("title", "Course"),
                        thumbnailUrl = if (obj.optString("thumbnail_url").isBlank()) null else obj.optString("thumbnail_url"),
                        academyEmail = if (obj.optString("academy_email").isBlank()) null else obj.optString("academy_email"),
                        status = obj.optString("status", "Approved"),
                        createdAt = obj.optString("created_at")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ----------------------------------------------------
    // CHAPTERS (REAL ADMIN CHAPTERS ONLY)
    // ----------------------------------------------------
    suspend fun fetchChapters(courseId: Long): List<Chapter> = withContext(Dispatchers.IO) {
        try {
            val request = publicRequestBuilder("/rest/v1/chapters?course_id=eq.$courseId&select=*&order=created_at.asc").get().build()
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
                            videoUrl = obj.optString("video_url", ""),
                            notes = if (obj.isNull("notes")) null else obj.optString("notes"),
                            type = obj.optString("type", if (i == 0) "Free" else "Paid"),
                            price = obj.optString("price", "0"),
                            createdAt = obj.optString("created_at")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    cacheChapters(courseId, list)
                    return@withContext list
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchChapters failed: ${e.message}")
        }
        // Return cached real chapters
        getCachedChapters(courseId)
    }

    private fun cacheChapters(courseId: Long, chapters: List<Chapter>) {
        val arr = JSONArray()
        for (ch in chapters) {
            arr.put(JSONObject().apply {
                put("id", ch.id)
                put("course_id", ch.courseId)
                put("chapter_title", ch.chapterTitle)
                put("video_url", ch.videoUrl)
                put("notes", ch.notes ?: "")
                put("type", ch.type)
                put("price", ch.price ?: "0")
                put("created_at", ch.createdAt ?: "")
            })
        }
        prefs.edit().putString("cached_chapters_$courseId", arr.toString()).apply()
    }

    private fun getCachedChapters(courseId: Long): List<Chapter> {
        val json = prefs.getString("cached_chapters_$courseId", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Chapter>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Chapter(
                        id = obj.optLong("id", i.toLong() + 1),
                        courseId = obj.optLong("course_id", courseId),
                        chapterTitle = obj.optString("chapter_title", "Chapter ${i + 1}"),
                        videoUrl = obj.optString("video_url", ""),
                        notes = if (obj.optString("notes").isBlank()) null else obj.optString("notes"),
                        type = obj.optString("type", if (i == 0) "Free" else "Paid"),
                        price = obj.optString("price", "0"),
                        createdAt = obj.optString("created_at")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ----------------------------------------------------
    // ACADEMY PROFILE
    // ----------------------------------------------------
    suspend fun fetchAcademyProfile(email: String): AcademyProfile = withContext(Dispatchers.IO) {
        try {
            val request = publicRequestBuilder("/rest/v1/academy_profiles?email=eq.$email&select=*").get().build()
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
                        teacherName = obj.optString("teacher_name", "Lead Instructor"),
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
        val localKey = "prog_${userEmail}_$courseId"
        val localSaved = prefs.getStringSet(localKey, null)?.mapNotNull { it.toLongOrNull() } ?: emptyList()

        try {
            val request = publicRequestBuilder("/rest/v1/course_progress?user_email=eq.$userEmail&course_id=eq.$courseId&select=*").get().build()
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
                        // Union with local
                        val combined = (list + localSaved).distinct()
                        prefs.edit().putStringSet(localKey, combined.map { it.toString() }.toSet()).apply()
                        return@withContext combined
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchProgress failed: ${e.message}")
        }
        localSaved
    }

    suspend fun saveProgress(userEmail: String, courseId: Long, completedChapters: List<Long>) = withContext(Dispatchers.IO) {
        val localKey = "prog_${userEmail}_$courseId"
        prefs.edit().putStringSet(localKey, completedChapters.map { it.toString() }.toSet()).apply()

        try {
            val jsonBody = JSONObject().apply {
                put("user_email", userEmail)
                put("course_id", courseId)
                put("completed_chapters", JSONArray(completedChapters))
            }
            val request = publicRequestBuilder("/rest/v1/course_progress?on_conflict=user_email,course_id")
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
            val request = publicRequestBuilder("/rest/v1/enrollments?user_email=eq.$userEmail&select=*&order=created_at.desc").get().build()
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
            val request = publicRequestBuilder("/rest/v1/enrollments")
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
            val request = publicRequestBuilder("/rest/v1/quizzes?chapter_id=eq.$chapterId&select=*").get().build()
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
        // Fallback default quiz
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
            val request = publicRequestBuilder("/rest/v1/coupons?code=eq.$upperCode&active=eq.true&select=*").get().build()
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
        // Popular default coupons
        when (code.trim().uppercase(Locale.getDefault())) {
            "GENZ50", "FLAT50" -> Coupon(1, "GENZ50", 50, true)
            "STUDENT20", "GENZ20" -> Coupon(2, "STUDENT20", 20, true)
            "FREE100" -> Coupon(3, "FREE100", 100, true)
            else -> null
        }
    }

    // ----------------------------------------------------
    // CANDIDATE PROFILE (SYNC)
    // ----------------------------------------------------
    suspend fun saveProfile(email: String, fullName: String, mobile: String): Boolean = withContext(Dispatchers.IO) {
        prefs.edit().apply {
            putString("user_fullname", fullName)
            putString("user_mobile", mobile)
            apply()
        }

        // Also update in saved_accounts
        addOrUpdateSavedAccount(UserProfile(email, fullName, mobile), null, null)

        try {
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("full_name", fullName)
                put("mobile", mobile)
            }
            val request = publicRequestBuilder("/rest/v1/candidate_profiles?on_conflict=email")
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
