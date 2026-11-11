
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

// -------------------------------------
// APP COLORS
// -------------------------------------

private val Ink = Color(0xFF090B12)
private val Panel = Color(0xFF171B28)
private val Purple = Color(0xFF9B8CFF)
private val Muted = Color(0xFFA6AEC2)

// -------------------------------------
// MAIN ACTIVITY + PICTURE IN PICTURE
// -------------------------------------

class MainActivity : ComponentActivity() {

    var isInPipMode by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            YouTubePlayerApp()
        }
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
        super.onPictureInPictureModeChanged(
            isInPictureInPictureMode,
            newConfig
        )

        isInPipMode = isInPictureInPictureMode
    }
}

// -------------------------------------
// MAIN APP SCREEN
// -------------------------------------

@Composable
fun YouTubePlayerApp() {

    var tab by remember {
        mutableIntStateOf(0)
    }

    var videoInput by remember {
        mutableStateOf("")
    }

    var videoId by remember {
        mutableStateOf("M7lc1UVf-VE")
    }

    var playerMessage by remember {
        mutableStateOf("")
    }

    var audioName by remember {
        mutableStateOf("No audio selected")
    }

    var controller by remember {
        mutableStateOf<MediaController?>(null)
    }

    val context = LocalContext.current

    val activity = context as? MainActivity

    val isInPipMode = activity?.isInPipMode == true

    // ---------------------------------
    // MEDIA SESSION CONNECTION
    // ---------------------------------

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

    // ---------------------------------
    // LICENSED MUSIC FILE PICKER
    // ---------------------------------

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

    // ---------------------------------
    // APP THEME
    // ---------------------------------

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

            // Hide navigation bar in PiP
            bottomBar = {

                if (!isInPipMode) {

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
                            label = {
                                Text("YouTube")
                            }
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
                            label = {
                                Text("My music")
                            }
                        )
                    }
                }
            }

        ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isInPipMode) {
                            Modifier
                        } else {
                            Modifier
                                .padding(padding)
                                .verticalScroll(
                                    rememberScrollState()
                                )
                                .padding(20.dp)
                        }
                    )
            ) {

                // ---------------------------------
                // NORMAL APP HEADER
                // ---------------------------------

                if (!isInPipMode) {

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
                }

                // ---------------------------------
                // YOUTUBE TAB
                // ---------------------------------

                if (tab == 0) {

                    if (!isInPipMode) {

                        OutlinedTextField(
                            value = videoInput,

                            onValueChange = {
                                videoInput = it
                                playerMessage = ""
                            },

                            modifier = Modifier.fillMaxWidth(),

                            singleLine = true,

                            label = {
                                Text("YouTube video URL or ID")
                            },

                            placeholder = {
                                Text("Paste YouTube link here")
                            },

                            trailingIcon = {

                                IconButton(
                                    onClick = {

                                        val extracted =
                                            extractVideoId(videoInput)

                                        if (extracted != null) {

                                            videoId = extracted

                                            playerMessage =
                                                "Video loaded. Use YouTube's play button."

                                        } else {

                                            playerMessage =
                                                "Invalid YouTube link. Check the URL."
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

                        if (playerMessage.isNotEmpty()) {

                            Text(
                                playerMessage,
                                color = Purple,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                    }

                    // ---------------------------------
                    // VIDEO PLAYER CARD
                    // Keep the player in the same
                    // composition when entering PiP.
                    // ---------------------------------

                    Card(
                        modifier = if (isInPipMode) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier.fillMaxWidth()
                        },

                        colors = CardDefaults.cardColors(
                            containerColor = if (isInPipMode) {
                                Color.Black
                            } else {
                                Panel
                            }
                        ),

                        shape = RoundedCornerShape(
                            if (isInPipMode) 0.dp else 22.dp
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isInPipMode) {
                                        Modifier
                                    } else {
                                        Modifier.padding(12.dp)
                                    }
                                )
                        ) {

                            if (!isInPipMode) {

                                Text(
                                    "Official YouTube player",
                                    fontWeight = FontWeight.SemiBold
                                )

                                Spacer(Modifier.height(10.dp))
                            }

                            YouTubeEmbed(
                                videoId = videoId,
                                isInPipMode = isInPipMode
                            )

                            if (!isInPipMode) {

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    "YouTube playback follows YouTube's player rules. Background playback is not enabled for YouTube videos.",
                                    color = Muted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // ---------------------------------
                    // MINI PLAYER BUTTON
                    // ---------------------------------

                    if (!isInPipMode) {

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                activity?.openVideoPip()
                            },

                            modifier = Modifier.fillMaxWidth(),

                            colors = ButtonDefaults.buttonColors(
                                containerColor = Purple
                            )
                        ) {

                            Text(
                                "Open Mini Player",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ---------------------------------
                // LICENSED MUSIC TAB
                // ---------------------------------

                else if (!isInPipMode) {

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

                // ---------------------------------
                // FOOTER
                // ---------------------------------

                if (!isInPipMode) {

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
}

// -------------------------------------
// YOUTUBE WEBVIEW
// ------------------
