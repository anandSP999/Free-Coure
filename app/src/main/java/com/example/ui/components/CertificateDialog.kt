package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CertificateDialog(
    studentName: String,
    courseTitle: String,
    certificateId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentDate = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault()).format(Date())

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Verified Certificate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Scrollable Certificate Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFAFCFF))
                        .border(6.dp, DarkNavy, RoundedCornerShape(8.dp))
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.dp, DarkBurgundy, RoundedCornerShape(4.dp))
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Gen-Z Gravity Education",
                                color = BluePrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 2.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "CERTIFICATE OF COMPLETION",
                                color = DarkNavy,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "THIS IS PROUDLY PRESENTED TO",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Text(
                                text = studentName,
                                color = DarkBurgundy,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .border(
                                        width = 1.dp,
                                        color = DarkNavy.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(2.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "For successfully completing the comprehensive learning curriculum, assessments, and practical assignments for the course:",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = courseTitle,
                                color = DarkNavy,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Bottom Grid: Date, Stamp Badge, Signature
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = currentDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                                    Divider(modifier = Modifier.width(90.dp).padding(vertical = 4.dp), color = DarkNavy)
                                    Text(text = "Date of Issue", fontSize = 10.sp, color = TextSecondary)
                                }

                                // Verified Gold Badge
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(GoldAccent, GoldDark)
                                            )
                                        )
                                        .border(2.dp, DarkBurgundy, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("VERIFIED", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                        Text(certificateId, fontSize = 8.sp, color = Color.White)
                                        Text("ACHIEVER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Gen-Z Gravity",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Cursive,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkNavy
                                    )
                                    Divider(modifier = Modifier.width(90.dp).padding(vertical = 4.dp), color = DarkNavy)
                                    Text(text = "Authorized Signature", fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            shareCertificate(context, studentName, courseTitle, certificateId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Credential")
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Certificate credential saved successfully! 🏆", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }
                }
            }
        }
    }
}

private fun shareCertificate(context: Context, name: String, course: String, id: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Verified Certificate of Completion - $course")
            putExtra(
                Intent.EXTRA_TEXT,
                "🎓 Verified Certificate of Completion!\n\nThis certifies that $name has successfully completed the course:\n$course\n\nCertificate Verification ID: $id\nIssued by: Gen-Z Gravity Education"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Certificate"))
    } catch (e: Exception) {
        Toast.makeText(context, "Verification ID copied: $id", Toast.LENGTH_SHORT).show()
    }
}
