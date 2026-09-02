package com.example.data.model

data class Banner(
    val id: Long = 0,
    val imageUrl: String = "",
    val linkUrl: String? = null,
    val active: Boolean = true
)

data class Course(
    val id: Long = 0,
    val title: String = "",
    val thumbnailUrl: String? = null,
    val academyEmail: String? = null,
    val status: String = "Approved",
    val createdAt: String? = null
)

data class Chapter(
    val id: Long = 0,
    val courseId: Long = 0,
    val chapterTitle: String = "",
    val videoUrl: String = "",
    val notes: String? = null,
    val type: String = "Free", // "Free" or "Paid"
    val price: String? = "0",
    val createdAt: String? = null
)

data class AcademyProfile(
    val id: Long = 0,
    val email: String = "",
    val academyName: String = "Gen-Z Gravity Platform",
    val teacherName: String = "Expert Instructor",
    val mobile: String = "+91 98765 43210"
)

data class CourseProgress(
    val id: Long = 0,
    val userEmail: String = "",
    val courseId: Long = 0,
    val completedChapters: List<Long> = emptyList()
)

data class Enrollment(
    val id: Long = 0,
    val userEmail: String = "",
    val courseTitle: String = "",
    val coursePrice: String = "0",
    val status: String = "Active", // "Active" or "Pending"
    val createdAt: String? = null,
    val accessUntil: String? = null
)

data class Quiz(
    val id: Long = 0,
    val chapterId: Long = 0,
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctOption: Int = 0
)

data class Coupon(
    val id: Long = 0,
    val code: String = "",
    val discountPercent: Int = 0,
    val active: Boolean = true
)

data class CandidateProfile(
    val id: Long = 0,
    val email: String = "",
    val fullName: String = "",
    val mobile: String = ""
)

data class UserProfile(
    val email: String,
    val fullName: String = "Student",
    val mobile: String = "N/A"
)
