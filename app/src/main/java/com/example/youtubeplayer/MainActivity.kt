
package com.example.youtubeplayer

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

import com.google.common.util.concurrent.MoreExecutors

private val Ink = Color(0xFF090B12)
private val Panel = Color(0xFF171B28)
private val Purple = Color(0xFF9B8CFF)
private val Muted = Color(0xFFA6AEC2)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            YouTubePlayerApp()
        }
    }
}

@Composable
fun YouTubePlayerApp() {

    var tab by remember { mutableIntStateOf(0) }

    var videoInput by remember {
        mutableStateOf("")
    }

    var videoId by remember {
        mutableStateOf("M7lc1UVf-VE")
    }

    var audioName by remember {
        mutableStateOf("No audio selected")
    }

    var controller by remember {
        mutableStateOf<MediaController?>(null)
    }

    val context = LocalContext.current

    DisposableEffect(Unit) {

        val token = SessionToken(
            context,
            ComponentName(
                context,
                AudioPlaybackService::class.java
            )
        )

        val future = MediaController.Builder(
            context,
            token
        ).buildAsync()

        future.addListener(
            {
                controller = runCatching {
                    future.get()
                }.getOrNull()
            },
            MoreExecutors.directExecutor()
        )

        onDispose {
            MediaController.releaseFuture(future)
        }
    }

    val audioPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->

            uri?.let {

                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                }

                audioName =
                    it.lastPathSegment
                        ?.substringAfterLast('/')
                        ?: "Selected audio"

                controller?.setMediaItem(
                    MediaItem.fromUri(it)
                )

                controller?.prepare()
                controller?.play()
            }
        }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Ink,
            surface = Panel,
            primary = Purple,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {

        Scaffold(
            containerColor = Ink,

            bottomBar = {

                NavigationBar(
                    containerColor = Panel
                ) {

                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = {
                            Icon(
                                Icons.Default.OndemandVideo,
                                contentDescription = "YouTube"
                            )
                        },
                        label = { Text("YouTube") }
                    )

                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = {
                            Icon(
                                Icons.Default.LibraryMusic,
                                contentDescription = "My music"
                            )
                        },
                        label = { Text("My music") }
                    )
                }
            }

        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {

                Text(
                    "YOUTUBE PLAYER",
                    color = Purple,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    if (tab == 0)
                        "Watch what you love."
                    else
                        "Your music, your space.",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "A calmer way to play.",
                    color = Muted
                )

                Spacer(Modifier.height(22.dp))

                if (tab == 0) {

                    OutlinedTextField(
                        value = videoInput,
                        onValueChange = {
                            videoInput = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text("YouTube video URL or ID")
                        },
                        trailingIcon = {

                            IconButton(
                                onClick = {

                                    val extracted =
                                        extractVideoId(videoInput)

                                    if (extracted != null) {
                                        videoId = extracted
                                    }
                                }
                            ) {

                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Load video"
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(14.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Panel
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ) {

                        Column(
                            Modifier.padding(12.dp)
                        ) {

                            Text(
                                "Official YouTube player",
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(10.dp))

                            YouTubeEmbed(videoId)

                            Spacer(Modifier.height(8.dp))

                            Text(
                                "YouTube playback follows YouTube's player rules. Background playback is not enabled for YouTube videos.",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                } else {

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Panel
                        ),
                        shape = RoundedCornerShape(22.dp)
                    ) {

                        Column(
                            Modifier.padding(18.dp)
                        ) {

                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Purple,
                                modifier = Modifier.size(42.dp)
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Licensed music library",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Choose audio files you own or are licensed to play. Background playback and media controls are supported.",
                                color = Muted
                            )

                            Spacer(Modifier.height(18.dp))

                            Text(
                                audioName,
                                color = Muted
                            )

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    audioPicker.launch(
                                        arrayOf("audio/*")
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Icon(
                                    Icons.Default.FolderOpen,
                                    contentDescription = null
                                )

                                Spacer(Modifier.width(8.dp))

                                Text("Choose audio file")
                            }

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.Center
                            ) {

                                IconButton(
                                    onClick = {
                                        controller?.seekToPreviousMediaItem()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.SkipPrevious,
                                        "Previous"
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        controller?.let {
                                            if (it.isPlaying) {
                                                it.pause()
                                            } else {
                                                it.play()
                                            }
                                        }
                                    }
                                ) {

                                    Icon(
                                        if (controller?.isPlaying == true)
                                            Icons.Default.Pause
                                        else
                                            Icons.Default.PlayArrow,

                                        contentDescription = "Play or pause",
                                        tint = Purple,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        controller?.seekToNextMediaItem()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.SkipNext,
                                        "Next"
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "BUILT FOR YOUR EVERYDAY LISTENING",
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Premium dark • Simple controls • Your library",
                    color = Muted
                )
            }
        }
    }
}

@Composable
private fun YouTubeEmbed(videoId: String) {

    AndroidView(

        factory = { ctx ->

            WebView(ctx).apply {

                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true

                settings.mediaPlaybackRequiresUserGesture = true

                CookieManager.getInstance()
                    .setAcceptThirdPartyCookies(this, true)

                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()

                setBackgroundColor(
                    android.graphics.Color.BLACK
                )
            }
        },

        update = { webView ->

            if (webView.tag != videoId) {

                webView.tag = videoId

                webView.loadUrl(
                    "https://www.youtube.com/embed/$videoId?playsinline=1"
                )
            }
        },

        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    )
}

private fun extractVideoId(input: String): String? {

    val value = input.trim()

    if (
        value.matches(
            Regex("^[A-Za-z0-9_-]{11}$")
        )
    ) {
        return value
    }

    val url = if (value.contains("://")) {
        value
    } else {
        "https://$value"
    }

    val uri = runCatching {
        Uri.parse(url)
    }.getOrNull() ?: return null

    val host = uri.host?.lowercase() ?: return null

    val id = when {

        host == "youtu.be" -> {
            uri.pathSegments.firstOrNull()
        }

        host == "youtube.com" ||
        host.endsWith(".youtube.com") -> {

            uri.getQueryParameter("v")
                ?: uri.pathSegments.let { segments ->

                    val index = segments.indexOfFirst {
                        it in listOf(
                            "embed",
                            "shorts",
                            "live"
                        )
                    }

                    if (index >= 0) {
                        segments.getOrNull(index + 1)
                    } else {
                        null
                    }
                }
        }

        else -> null
    }

    return id?.takeIf {
        it.matches(
            Regex("^[A-Za-z0-9_-]{11}$")
        )
    }
}
