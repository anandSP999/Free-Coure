package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoPlayerView(
    videoUrl: String,
    watermarkEmail: String,
    modifier: Modifier = Modifier
) {
    val embedUrl = rememberEmbedUrl(videoUrl)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mediaPlaybackRequiresUserGesture = false
                        allowFileAccess = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                        }
                    }
                    loadDataWithBaseURL(
                        "https://www.youtube.com",
                        """
                        <!DOCTYPE html>
                        <html>
                        <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
                        <style>
                            * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000; }
                            html, body { width: 100%; height: 100%; overflow: hidden; }
                            iframe { width: 100%; height: 100%; border: 0; }
                        </style>
                        </head>
                        <body>
                            <iframe 
                                src="$embedUrl" 
                                frameborder="0" 
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                                allowfullscreen>
                            </iframe>
                        </body>
                        </html>
                        """.trimIndent(),
                        "text/html",
                        "utf-8",
                        null
                    )
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(
                    "https://www.youtube.com",
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; background-color: #000; }
                        html, body { width: 100%; height: 100%; overflow: hidden; }
                        iframe { width: 100%; height: 100%; border: 0; }
                    </style>
                    </head>
                    <body>
                        <iframe 
                            src="$embedUrl" 
                            frameborder="0" 
                            allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" 
                            allowfullscreen>
                        </iframe>
                    </body>
                    </html>
                    """.trimIndent(),
                    "text/html",
                    "utf-8",
                    null
                )
            },
            modifier = Modifier.fillMaxSize()
        )

        // Security Watermark floating across the video player
        Text(
            text = watermarkEmail,
            color = Color.White.copy(alpha = 0.35f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Center)
                .rotate(-22f)
        )
    }
}

private fun rememberEmbedUrl(url: String): String {
    return when {
        url.contains("youtube.com") || url.contains("youtu.be") -> {
            val videoId = if (url.contains("v=")) {
                url.substringAfter("v=").substringBefore("&").substringBefore("?")
            } else if (url.contains("youtu.be/")) {
                url.substringAfter("youtu.be/").substringBefore("?").substringBefore("&")
            } else {
                url
            }
            "https://www.youtube.com/embed/$videoId?autoplay=1&rel=0&modestbranding=1"
        }
        else -> url
    }
}
