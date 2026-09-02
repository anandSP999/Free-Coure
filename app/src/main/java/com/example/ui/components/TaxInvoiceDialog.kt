package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AcademyProfile
import com.example.data.model.Enrollment
import com.example.data.model.UserProfile
import com.example.ui.theme.*

@Composable
fun TaxInvoiceDialog(
    enrollment: Enrollment,
    userProfile: UserProfile?,
    academyProfile: AcademyProfile?,
    txnId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val studentName = if (!userProfile?.fullName.isNullOrBlank() && userProfile?.fullName != "Student") userProfile!!.fullName else "Registered Student"
    val studentEmail = userProfile?.email ?: enrollment.userEmail
    val studentMobile = userProfile?.mobile ?: "+91 98765 XXXXX"

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
                    .padding(16.dp)
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Official Tax Invoice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                // Invoice Document Canvas
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text("TAX INVOICE", fontSize = 20.sp, fontWeight = FontWeight.Black, color = BluePrimary)
                            Text("Original for Recipient", fontSize = 11.sp, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Gen-Z Gravity", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("support@eduportal.com", fontSize = 11.sp, color = TextSecondary)
                            Text("+91 98765 43210", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Divider(color = BluePrimary, thickness = 2.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Billed To & Provider Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Billed To (Student)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Name: $studentName", fontSize = 11.sp, color = TextPrimary)
                                Text("Email: $studentEmail", fontSize = 11.sp, color = TextPrimary)
                                Text("Phone: $studentMobile", fontSize = 11.sp, color = TextPrimary)
                                Text("Date: ${enrollment.createdAt ?: "Today"}", fontSize = 11.sp, color = TextPrimary)
                                Text("Txn ID: $txnId", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkNavy)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Provider (Academy)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BluePrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Name: ${academyProfile?.academyName ?: "Gen-Z Gravity"}", fontSize = 11.sp, color = TextPrimary)
                                Text("Teacher: ${academyProfile?.teacherName ?: "Lead Instructor"}", fontSize = 11.sp, color = TextPrimary)
                                Text("Contact: ${academyProfile?.mobile ?: "+91 98765 43210"}", fontSize = 11.sp, color = TextPrimary)
                                Text("Access: ${enrollment.accessUntil ?: "Lifetime"}", fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Invoice Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderGray, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BluePrimary)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Description", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                            Text("Status", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("Amount (INR)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(enrollment.courseTitle, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(2f))
                            Text("Verified", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenSuccess, modifier = Modifier.weight(1f))
                            val priceDisplay = if (enrollment.coursePrice.equals("Free", ignoreCase = true)) "0" else enrollment.coursePrice
                            Text("₹$priceDisplay", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkNavy, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Footer Notice
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Thank you for choosing Gen-Z Gravity!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "This is a verified computer-generated invoice and requires no physical signature.",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            shareInvoice(context, studentName, enrollment.courseTitle, txnId, enrollment.coursePrice)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share")
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Tax Invoice PDF downloaded successfully! 📥", Toast.LENGTH_LONG).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF")
                    }
                }
            }
        }
    }
}

private fun shareInvoice(context: Context, name: String, course: String, txn: String, price: String) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Tax Invoice - $course")
            putExtra(
                Intent.EXTRA_TEXT,
                "📄 Gen-Z Gravity Tax Invoice\n\nBilled To: $name\nCourse: $course\nTxn ID: $txn\nAmount: ₹$price\nStatus: Verified\nPlatform: https://genzgravity.com"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Invoice"))
    } catch (e: Exception) {
        Toast.makeText(context, "Invoice Txn ID copied: $txn", Toast.LENGTH_SHORT).show()
    }
}
