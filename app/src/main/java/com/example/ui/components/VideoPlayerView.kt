package com.example.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import java.util.Locale

enum class VideoType {
    YOUTUBE, DIRECT_MP4, GOOGLE_DRIVE, GENERIC_WEB
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerView(
    videoUrl: String,
    watermarkEmail: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val videoType = remember(videoUrl) { detectVideoType(videoUrl) }
    val htmlData = remember(videoUrl) { generatePlayerHtml(videoUrl, videoType) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkNavy)
    ) {
        // Top Player Action Bar (Type badge, Reload, External Player launcher)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Video Type Indicator Badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when (videoType) {
                    VideoType.YOUTUBE -> Color(0xFFDC2626)
                    VideoType.DIRECT_MP4 -> Color(0xFF7C3AED)
                    VideoType.GOOGLE_DRIVE -> Color(0xFF059669)
                    VideoType.GENERIC_WEB -> Color(0xFF2563EB)
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = when (videoType) {
                            VideoType.YOUTUBE -> Icons.Default.PlayCircle
                            VideoType.DIRECT_MP4 -> Icons.Default.Movie
                            VideoType.GOOGLE_DRIVE -> Icons.Default.Cloud
                            VideoType.GENERIC_WEB -> Icons.Default.Language
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (videoType) {
                            VideoType.YOUTUBE -> "YouTube HD"
                            VideoType.DIRECT_MP4 -> "MP4 Player"
                            VideoType.GOOGLE_DRIVE -> "Drive Stream"
                            VideoType.GENERIC_WEB -> "Web Player"
                        },
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Action Buttons (Reload, Open in External Player / YouTube App)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Reload Button
                IconButton(
                    onClick = {
                        webViewInstance?.reload()
                        Toast.makeText(context, "Reloading video...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reload",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // External Player Button
                IconButton(
                    onClick = {
                        openInExternalApp(context, videoUrl)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Open in External App",
                        tint = YellowWarning,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // The Video Display Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            cacheMode = WebSettings.LOAD_DEFAULT
                            // Modern mobile user agent to prevent YouTube embed restrictions
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customView = view
                                customViewCallback = callback
                                isFullscreen = true
                                (ctx as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }

                            override fun onHideCustomView() {
                                customView = null
                                customViewCallback?.onCustomViewHidden()
                                isFullscreen = false
                                (ctx as? Activity)?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val targetUrl = request?.url?.toString() ?: ""
                                // Keep player URLs inside the webview
                                if (targetUrl.contains("youtube.com/embed") || targetUrl.contains("youtube-nocookie.com")) {
                                    return false
                                }
                                return false
                            }
                        }

                        loadDataWithBaseURL("https://genzgravity.com", htmlData, "text/html", "utf-8", null)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                    webView.loadDataWithBaseURL("https://genzgravity.com", htmlData, "text/html", "utf-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )

            // Security Watermark floating across the video player
            Text(
                text = watermarkEmail,
                color = Color.White.copy(alpha = 0.28f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .rotate(-22f)
            )
        }
    }
}

private fun detectVideoType(url: String): VideoType {
    val lower = url.lowercase(Locale.getDefault())
    return when {
        lower.contains("youtube.com") || lower.contains("youtu.be") -> VideoType.YOUTUBE
        lower.contains("drive.google.com") -> VideoType.GOOGLE_DRIVE
        lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".m3u8") ||
        lower.endsWith(".mov") || lower.endsWith(".mkv") || lower.contains(".mp4?") ||
        lower.contains("/video/") || lower.contains("/storage/v1/object/") -> VideoType.DIRECT_MP4
        else -> VideoType.GENERIC_WEB
    }
}

private fun extractYouTubeId(url: String): String {
    val trimmed = url.trim()
    val pattern = "(?:youtu\\.be\\/|youtube\\.com\\/(?:embed\\/|v\\/|watch\\?v=|watch\\?.+&v=|shorts\\/|live\\/))([a-zA-Z0-9_-]{11})".toRegex()
    val match = pattern.find(trimmed)
    if (match != null) {
        return match.groupValues[1]
    }

    return when {
        trimmed.contains("youtu.be/") -> {
            trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
        }
        trimmed.contains("v=") -> {
            trimmed.substringAfter("v=").substringBefore("&").substringBefore("?")
        }
        trimmed.contains("/embed/") -> {
            trimmed.substringAfter("/embed/").substringBefore("?").substringBefore("&")
        }
        trimmed.contains("/shorts/") -> {
            trimmed.substringAfter("/shorts/").substringBefore("?").substringBefore("&")
        }
        else -> trimmed
    }
}

private fun generatePlayerHtml(url: String, type: VideoType): String {
    return when (type) {
        VideoType.YOUTUBE -> {
            val videoId = extractYouTubeId(url)
            """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000000; }
                html, body { width: 100%; height: 100%; overflow: hidden; background: #000000; }
                iframe { width: 100%; height: 100%; border: none; }
            </style>
            </head>
            <body>
                <iframe 
                    id="player"
                    src="https://www.youtube-nocookie.com/embed/$videoId?enablejsapi=1&autoplay=1&playsinline=1&rel=0&modestbranding=1&controls=1&fs=1&origin=https://genzgravity.com" 
                    frameborder="0" 
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                    allowfullscreen>
                </iframe>
            </body>
            </html>
            """.trimIndent()
        }
        VideoType.DIRECT_MP4 -> {
            """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000000; }
                html, body { width: 100%; height: 100%; overflow: hidden; display: flex; justify-content: center; align-items: center; background: #000000; }
                video { width: 100%; height: 100%; max-height: 100%; object-fit: contain; }
            </style>
            </head>
            <body>
                <video id="vPlayer" controls autoplay playsinline controlslist="nodownload" preload="auto">
                    <source src="$url" type="video/mp4">
                    <source src="$url" type="video/webm">
                    <source src="$url">
                    Your device does not support HTML5 video playback.
                </video>
                <script>
                    var v = document.getElementById('vPlayer');
                    v.play().catch(function(e) { console.log('Autoplay handled', e); });
                </script>
            </body>
            </html>
            """.trimIndent()
        }
        VideoType.GOOGLE_DRIVE -> {
            val previewUrl = if (url.contains("/file/d/")) {
                val fileId = url.substringAfter("/file/d/").substringBefore("/")
                "https://drive.google.com/file/d/$fileId/preview"
            } else url

            """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000000; }
                html, body { width: 100%; height: 100%; overflow: hidden; background: #000000; }
                iframe { width: 100%; height: 100%; border: none; }
            </style>
            </head>
            <body>
                <iframe src="$previewUrl" frameborder="0" allow="autoplay; fullscreen" allowfullscreen></iframe>
            </body>
            </html>
            """.trimIndent()
        }
        VideoType.GENERIC_WEB -> {
            """
            <!DOCTYPE html>
            <html>
            <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000000; }
                html, body { width: 100%; height: 100%; overflow: hidden; background: #000000; }
                iframe, video { width: 100%; height: 100%; border: none; }
            </style>
            </head>
            <body>
                <video id="vPlayer" controls autoplay playsinline preload="auto">
                    <source src="$url">
                    <iframe src="$url" frameborder="0" allowfullscreen></iframe>
                </video>
            </body>
            </html>
            """.trimIndent()
        }
    }
}

private fun openInExternalApp(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Unable to open external player", Toast.LENGTH_SHORT).show()
    }
}
