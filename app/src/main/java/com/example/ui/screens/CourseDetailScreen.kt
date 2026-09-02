package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AcademyProfile
import com.example.data.model.Chapter
import com.example.data.model.Course
import com.example.data.model.Quiz
import com.example.ui.components.VideoPlayerView
import com.example.ui.theme.*

@Composable
fun CourseDetailScreen(
    course: Course,
    chapters: List<Chapter>,
    activeChapter: Chapter?,
    academyProfile: AcademyProfile?,
    completedChapterIds: List<Long>,
    isEnrolled: Boolean,
    totalPrice: Int,
    watermarkEmail: String,
    currentQuiz: Quiz?,
    selectedQuizOption: Int,
    quizResult: String?,
    isQuizCorrect: Boolean?,
    isQuizLoading: Boolean,
    onBack: () -> Unit,
    onChapterClick: (Chapter) -> Unit,
    onMarkChapterComplete: () -> Unit,
    onLoadQuiz: () -> Unit,
    onSelectQuizOption: (Int) -> Unit,
    onSubmitQuiz: () -> Unit,
    onOpenPayment: () -> Unit,
    onOpenCertificate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allChaptersCompleted = chapters.isNotEmpty() && completedChapterIds.size >= chapters.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Bar (Back Button + Title)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BluePrimary)
                }

                Text(
                    text = course.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                OutlinedButton(
                    onClick = onBack,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("⬅ Back", fontSize = 12.sp, color = BluePrimary)
                }
            }
        }

        // Active Video Player
        if (activeChapter != null) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    VideoPlayerView(
                        videoUrl = activeChapter.videoUrl,
                        watermarkEmail = watermarkEmail,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Now Playing: ${activeChapter.chapterTitle}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        // About the Author / Notes Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "About the Author",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Provider: ${academyProfile?.academyName ?: "Gen-Z Gravity Platform"}",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "Instructor: ${academyProfile?.teacherName ?: "Lead Instructor"}",
                        fontSize = 12.sp,
                        color = TextPrimary
                    )

                    // Study Notes
                    if (activeChapter?.notes != null && activeChapter.notes.isNotBlank() && activeChapter.notes != "null") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = BorderGray)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val noteText = activeChapter.notes
                                    if (noteText.startsWith("http")) {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(noteText))
                                        try { context.startActivity(browserIntent) } catch (e: Exception) { /* ignore */ }
                                    }
                                }
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("📝 Study Notes: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(
                                text = if (activeChapter.notes.startsWith("http")) "View Material ↗" else activeChapter.notes,
                                fontSize = 12.sp,
                                color = BluePrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Chapter Action Buttons (Mark Complete / Take Quiz)
        if (activeChapter != null) {
            val isCurrentChapterCompleted = completedChapterIds.contains(activeChapter.id)

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isCurrentChapterCompleted) {
                        Button(
                            onClick = onMarkChapterComplete,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("✅ Mark Complete", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onLoadQuiz,
                        colors = ButtonDefaults.buttonColors(containerColor = YellowWarning),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📝 Take Chapter Quiz", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                    }
                }
            }
        }

        // Quiz Box (if active)
        if (currentQuiz != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDFDFD)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text("Skill Assessment", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BluePrimary)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = currentQuiz.question,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quiz Options
                        currentQuiz.options.forEachIndexed { index, option ->
                            val isSelected = selectedQuizOption == index
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) BlueLight else Color(0xFFF4F7F6),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) BluePrimary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onSelectQuizOption(index) }
                            ) {
                                Text(
                                    text = option,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onSubmitQuiz,
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Answer", fontWeight = FontWeight.Bold)
                        }

                        if (quizResult != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = quizResult,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isQuizCorrect == true) GreenSuccess else RedDanger,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }
        }

        // Course Completed Celebration Banner
        if (allChaptersCompleted) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, GreenSuccess, RoundedCornerShape(12.dp))
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🎉 Course Completed!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenDark
                        )
                        Text(
                            text = "Congratulations on finishing all chapters and assessments.",
                            fontSize = 12.sp,
                            color = GreenDark,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Button(
                            onClick = onOpenCertificate,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("🏆 Download Verified Certificate", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Payment Prompt Box (if not enrolled and has price)
        if (!isEnrolled && totalPrice > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = YellowLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, YellowBorder, RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Unlock Full Course Access", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = YellowText)
                        Text(
                            text = "Pay once to unlock all premium videos, assessments, and the verified certificate for this entire course.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                        Button(
                            onClick = onOpenPayment,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Pay ₹$totalPrice to Unlock All", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Course Curriculum Header
        item {
            Text(
                text = "Course Curriculum",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        // Chapter Items
        itemsIndexed(chapters, key = { index, ch -> "${ch.id}_$index" }) { index, ch ->
            val isLocked = !isEnrolled && ch.type.equals("Paid", ignoreCase = true)
            val isCompleted = completedChapterIds.contains(ch.id)
            val isCurrentActive = activeChapter?.id == ch.id

            // Youtube Thumbnail extractor
            val videoThumb = rememberThumbnail(ch.videoUrl)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = if (isCurrentActive) 2.dp else 1.dp,
                        color = if (isCurrentActive) BluePrimary else if (isCompleted) GreenSuccess else BorderGray,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onChapterClick(ch) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentActive) BlueAccent else SurfaceWhite
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Video Thumbnail
                    Box(
                        modifier = Modifier
                            .width(86.dp)
                            .height(52.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = videoThumb,
                            contentDescription = ch.chapterTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isLocked) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title & Badges
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Ep ${index + 1}: ${ch.chapterTitle}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (isCompleted) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = GreenSuccess, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = if (ch.type.equals("Free", ignoreCase = true)) GreenLight else BlueLight
                            ) {
                                Text(
                                    text = if (ch.type.equals("Free", ignoreCase = true)) "Free Preview" else "Premium Video",
                                    color = if (ch.type.equals("Free", ignoreCase = true)) GreenDark else BlueDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun rememberThumbnail(videoUrl: String): String {
    return if (videoUrl.contains("youtube.com") || videoUrl.contains("youtu.be")) {
        val videoId = if (videoUrl.contains("v=")) {
            videoUrl.substringAfter("v=").substringBefore("&").substringBefore("?")
        } else if (videoUrl.contains("youtu.be/")) {
            videoUrl.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
        } else {
            ""
        }
        "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
    } else {
        "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=300&q=80"
    }
}
