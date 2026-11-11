package com.example.youtubeplayer

import android.app.PictureInPictureParams
import android.content.ComponentName
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    var isInPipMode by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YouTubePlayerApp() }
    }

    fun openVideoPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }
}

@Composable
fun YouTubePlayerApp() {
    var tab by remember { mutableIntStateOf(0) }
    var videoInput by remember { mutableStateOf("") }
    var videoId by remember { mutableStateOf("M7lc1UVf-VE") }
    var playerMessage by remember { mutableStateOf("Paste a YouTube link, then tap the arrow.") }
    var audioName by remember { mutableStateOf("No audio selected") }
    var controller by remember { mutableStateOf<MediaController?>(null) }

    val context = LocalContext.current
    val activity = context as? MainActivity
    val isInPipMode = activity?.isInPipMode == true

    DisposableEffect(context) {
        val token = SessionToken(
            context,
            ComponentName(context, AudioPlaybackService::class.java)
        )
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            { controller = runCatching { future.get() }.getOrNull() },
            MoreExecutors.directExecutor()
        )
        onDispose { MediaController.releaseFuture(future) }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            audioName = selectedUri.lastPathSegment?.substringAfterLast('/') ?: "Selected audio"
            controller?.setMediaItem(MediaItem.fromUri(selectedUri))
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
                if (!isInPipMode) {
                    NavigationBar(containerColor = Panel) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Icon(Icons.Default.OndemandVideo, contentDescription = "YouTube") },
                            label = { Text("YouTube") }
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "My music") },
                            label = { Text("My music") }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isInPipMode) Modifier
                        else Modifier
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    )
            ) {
                if (!isInPipMode) {
                    Text("YOUTUBE PLAYER", color = Purple, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (tab == 0) "Watch what you love." else "Your music, your space.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("A calmer way to play.", color = Muted)
                    Spacer(Modifier.height(22.dp))
                }

                if (tab == 0) {
                    if (!isInPipMode) {
                        OutlinedTextField(
                            value = videoInput,
                            onValueChange = { videoInput = it; playerMessage = "" },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("YouTube video URL or ID") },
                            placeholder = { Text("Paste YouTube link here") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val extracted = extractYouTubeVideoId(videoInput)
                                    if (extracted != null) {
                                        videoId = extracted
                                        playerMessage = "Video loaded. Press Play in the YouTube player."
                                    } else {
                                        playerMessage = "That link was not recognised. Use a youtube.com or youtu.be video link."
                                    }
                                }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Load video")
                                }
                            }
                        )
                        if (playerMessage.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(playerMessage, color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    Card(
                        modifier = if (isInPipMode) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isInPipMode) Color.Black else Panel),
                        shape = RoundedCornerShape(if (isInPipMode) 0.dp else 22.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (isInPipMode) Modifier.fillMaxSize() else Modifier.padding(12.dp))
                        ) {
                            if (!isInPipMode) {
                                Text("YouTube video", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(10.dp))
                            }
                            YouTubeEmbed(
                                videoId = videoId,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (isInPipMode) Modifier.fillMaxSize() else Modifier.aspectRatio(16f / 9f))
                            )
                            if (!isInPipMode) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Videos play using YouTube's official embedded player. PiP availability and background playback are controlled by YouTube and Android; this app cannot bypass YouTube restrictions.",
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { activity?.openVideoPip() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Purple)
                                ) {
                                    Text("Open Picture-in-Picture", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (!isInPipMode) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Panel),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Purple, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Licensed music library", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Choose audio files you own or are licensed to play. Background playback and media controls are supported.",
                                color = Muted
                            )
                            Spacer(Modifier.height(18.dp))
                            Text(audioName, color = Muted)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { audioPicker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Choose audio file")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { controller?.seekToPreviousMediaItem() }) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                                }
                                IconButton(onClick = {
                                    controller?.let { player ->
                                        if (player.isPlaying) player.pause() else player.play()
                                    }
                                }) {
                                    Icon(
                                        if (controller?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play or pause",
                                        tint = Purple,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                                IconButton(onClick = { controller?.seekToNextMediaItem() }) {
                                    Icon(Icons.Default.SkipNext, contentDescription = "Next")
                                }
                            }
                        }
                    }
                }

                if (!isInPipMode) {
                    Spacer(Modifier.height(20.dp))
                    Text("BUILT FOR YOUR EVERYDAY LISTENING", color = Muted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text("Premium dark • Simple controls • Your library", color = Muted)
                }
            }
        }
    }
}

@Composable
private fun YouTubeEmbed(videoId: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            WebView(viewContext).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.mediaPlaybackRequiresUserGesture = true
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.javaScriptCanOpenWindowsAutomatically = false
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            }
        },
        update = { webView ->
            if (webView.tag != videoId) {
                webView.tag = videoId
                val html = """
                    <!doctype html>
                    <html>
                    <head>
                      <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
                      <style>
                        html, body { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; }
                        iframe { position:absolute; inset:0; width:100%; height:100%; border:0; }
                      </style>
                    </head>
                    <body>
                      <iframe
                        src="https://www.youtube.com/embed/$videoId?playsinline=1&enablejsapi=1&rel=0&origin=https%3A%2F%2Fappassets.androidplatform.net"
                        title="YouTube video player"
                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                        referrerpolicy="strict-origin-when-cross-origin"
                        allowfullscreen>
                      </iframe>
                    </body>
                    </html>
                """.trimIndent()
                webView.loadDataWithBaseURL(
                    "https://appassets.androidplatform.net/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

private fun extractYouTubeVideoId(input: String): String? {
    val value = input.trim()
    if (value.matches(Regex("^[A-Za-z0-9_-]{11}$"))) return value

    val uri = runCatching {
        Uri.parse(if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value")
    }.getOrNull() ?: return null

    val host = (uri.host ?: return null).lowercase().removePrefix("www.")
    val candidate = when {
        host == "youtu.be" -> uri.lastPathSegment
        host == "youtube.com" || host == "m.youtube.com" || host == "music.youtube.com" -> {
            when {
                uri.path == "/watch" -> uri.getQueryParameter("v")
                uri.pathSegments.size >= 2 && uri.pathSegments[0] in setOf("embed", "shorts", "live", "v") -> uri.pathSegments[1]
                else -> null
            }
        }
        else -> null
    }
    return candidate?.takeIf { it.matches(Regex("^[A-Za-z0-9_-]{11}$")) }
}
