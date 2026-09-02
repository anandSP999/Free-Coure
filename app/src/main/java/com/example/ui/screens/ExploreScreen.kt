package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Banner
import com.example.data.model.Course
import com.example.data.model.Enrollment
import com.example.ui.components.BannerCarousel
import com.example.ui.components.CourseCardItem
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BorderGray
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ExploreScreen(
    banners: List<Banner>,
    courses: List<Course>,
    enrollments: List<Enrollment>,
    favoriteCourseIds: Set<Long>,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onCourseClick: (Course) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onShareCourse: (Context, Course) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredCourses = courses.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner Slider Span
        if (banners.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BannerCarousel(
                    banners = banners,
                    onBannerClick = { /* Banner click action */ },
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        // Section Title
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "Explore Courses",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
            )
        }

        // Search Bar Span
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("🔍 Search courses...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BluePrimary) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("course_search_bar"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = BorderGray
                )
            )
        }

        if (isLoading && courses.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            }
        } else if (filteredCourses.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No courses found matching '$searchQuery'",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(filteredCourses, key = { it.id }) { course ->
                val isEnrolled = enrollments.any {
                    it.courseTitle.equals(course.title, ignoreCase = true) && it.status.equals("Active", ignoreCase = true)
                }
                val isFav = favoriteCourseIds.contains(course.id)

                CourseCardItem(
                    course = course,
                    isFavorite = isFav,
                    isEnrolled = isEnrolled,
                    onCourseClick = { onCourseClick(course) },
                    onToggleFavorite = { onToggleFavorite(course.id) },
                    onShare = { onShareCourse(context, course) }
                )
            }
        }
    }
}
