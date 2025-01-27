package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.VoiceMessage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceChatScreen(
    viewModel: VoiceChatViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val motherName by viewModel.motherName.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val ttsVoice by viewModel.ttsVoice.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var showArchiveSheet by remember { mutableStateOf(false) }
    val recordPermissionState = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    // Layout direction set to Right-To-Left (RTL) for Persian language compatibility
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Vibrant animated glowing background decoration
                GlowingBackgroundDecorator()

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    
                    // 1. Header (Profile info & Greeting & Theme Switcher on top left)
                    HeaderSection(
                        motherName = motherName,
                        isDark = darkMode,
                        onThemeToggle = { viewModel.updateDarkMode(!darkMode) }
                    )

                    // 2. Interactive Speaking Stage (Massive Button, Status, Waves)
                    InteractiveStage(
                        isRecording = isRecording,
                        isProcessing = isProcessing,
                        onToggleSpeaking = {
                            if (!recordPermissionState.status.isGranted) {
                                recordPermissionState.launchPermissionRequest()
                            } else {
                                if (isRecording) {
                                    viewModel.stopRecording()
                                } else {
                                    viewModel.startRecording()
                                }
                            }
                        }
                    )

                    // 3. Persistent Navigation & Bottom Sheet Panel
                    BottomNavigationPanel(
                        onArchiveClick = { showArchiveSheet = true },
                        onSettingsClick = { showSettings = true }
                    )
                }

                // Friendly Alert Error popup
                errorMessage?.let { error ->
                    Dialog(onDismissRequest = { viewModel.clearError() }) {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F1)),
                            elevation = CardDefaults.cardElevation(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .border(1.5.dp, Color(0xFFFFB2B6), RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Error,
                                    contentDescription = "خطا",
                                    tint = Color(0xFFBA1A1A),
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "مامان عزیز، خطایی رخ داد:",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF410002),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    fontSize = 15.sp,
                                    color = Color(0xFF410002).copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { viewModel.clearError() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text("متوجه شدم", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Slide-up Archive Conversations Sheet (keeps main screen incredibly clean)
            if (showArchiveSheet) {
                ArchiveSheetDialog(
                    messages = messages,
                    motherName = motherName,
                    isProcessing = isProcessing,
                    viewModel = viewModel,
                    onDismiss = { showArchiveSheet = false }
                )
            }

            // Advanced configuration settings popup
            if (showSettings) {
                SettingsDialog(
                    viewModel = viewModel,
                    onDismiss = { showSettings = false }
                )
            }
        }
    }
}

/**
 * Modern Profile Avatar Header with soft colors and theme switcher.
 */
@Composable
fun HeaderSection(
    motherName: String,
    isDark: Boolean,
    onThemeToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Elegant round letter avatar with soft purple/lilac gradient
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                val initialChar = if (motherName.isNotEmpty()) motherName.take(1) else "ح"
                Text(
                    text = initialChar,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column {
                Text(
                    text = "سلام مامان $motherName عزیز",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "خوش آمدید",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Lightweight light/dark theme switch action on top left
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onThemeToggle() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = "تغییر پوسته",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Centered speak button stage with glowing background, wave animations, and simple instructions.
 */
@Composable
fun InteractiveStage(
    isRecording: Boolean,
    isProcessing: Boolean,
    onToggleSpeaking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        // Massive, ultra-legible Speak button with strong drop shadows
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(260.dp)
        ) {
            // Pulsing background decorative aura
            val transition = rememberInfiniteTransition()
            val auraScale by transition.animateFloat(
                initialValue = 0.98f,
                targetValue = if (isRecording) 1.25f else 1.08f,
                animationSpec = infiniteRepeatable(
                    animation = tween(if (isRecording) 800 else 2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            val spinRotation by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            // Blur aura simulation using drawBehind
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .drawBehind {
                        drawCircle(
                            color = when {
                                isRecording -> Color(0xFFFFD8E4).copy(alpha = 0.55f * auraScale)
                                isProcessing -> Color(0xFFEADDFF).copy(alpha = 0.45f)
                                else -> Color(0xFFD0BCFF).copy(alpha = 0.35f * auraScale)
                            },
                            radius = (size.width / 2f) * auraScale
                        )
                    }
            )

            // The main purple CTA button
            Box(
                modifier = Modifier
                    .size(210.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = when {
                                isRecording -> listOf(Color(0xFFBA2252), Color(0xFF881337))
                                isProcessing -> listOf(Color(0xFFE0A000), Color(0xFFB07000))
                                else -> listOf(Color(0xFF6750A4), Color(0xFF4F378B))
                            }
                        )
                    )
                    .border(8.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .drawBehind {
                        if (isProcessing) {
                            // Spinning glowing edge arc while processing voice responses
                            drawArc(
                                color = Color(0xFFFFD54F),
                                startAngle = spinRotation,
                                sweepAngle = 100f,
                                useCenter = false,
                                style = Stroke(6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        enabled = !isProcessing,
                        onClick = onToggleSpeaking
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isRecording -> Icons.Filled.Stop
                        isProcessing -> Icons.Filled.HourglassEmpty
                        else -> Icons.Filled.Mic
                    },
                    contentDescription = "دکمه اصلی صحبت صوتی",
                    tint = Color.White,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // State headings (Persian)
        Text(
            text = when {
                isRecording -> "در حال شنیدن صدای شما..."
                isProcessing -> "همدم در حال تفکر..."
                else -> "بزن و حرف بزن"
            },
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = when {
                isRecording -> "وقتی صحبتت تمام شد، دوباره این دکمه را بزن"
                isProcessing -> "در حال ایجاد صدای گرم برای پاسخ..."
                else -> "من سراپا گوشم، راحت با من دردودل کن"
            },
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Harmonic speaking sound bars (symmetric elegant layout)
        VoicePulseWaveform(isActive = isRecording || isProcessing)
    }
}

/**
 * Animated soft glowing background blobs.
 */
@Composable
fun GlowingBackgroundDecorator() {
    val transition = rememberInfiniteTransition()
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height

                // Warm lilac/rose ambient glowing blobs in corners
                drawCircle(
                    color = Color(0xFFD0BCFF).copy(alpha = pulseAlpha),
                    center = Offset(width * 0.15f, height * 0.35f),
                    radius = width * 0.45f
                )
                drawCircle(
                    color = Color(0xFFFFD8E4).copy(alpha = pulseAlpha),
                    center = Offset(width * 0.85f, height * 0.65f),
                    radius = width * 0.5f
                )
            }
    )
}

/**
 * Symmetrical beautiful purple sound bars representing waves.
 */
@Composable
fun VoicePulseWaveform(isActive: Boolean) {
    Row(
        modifier = Modifier
            .width(260.dp)
            .height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val count = 13
        val baseHeights = listOf(14, 24, 18, 36, 12, 44, 28, 44, 12, 36, 18, 24, 14)

        for (i in 0 until count) {
            val h = baseHeights[i % baseHeights.size]
            val heightPercent by animateFloatAsState(
                targetValue = if (isActive) 1f else 0.25f,
                animationSpec = if (isActive) {
                    infiniteRepeatable(
                        animation = tween(400 + (i * 45), easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                } else {
                    tween(300)
                }
            )

            // Set opacity based on position for extra artistic premium depth
            val opacity = when (i) {
                0, 12 -> 0.35f
                1, 11 -> 0.55f
                2, 10 -> 0.80f
                else -> 1f
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((h * heightPercent).dp)
                    .clip(CircleShape)
                    .background(
                        color = if (i == 4 || i == 8) Color(0xFFD0BCFF).copy(alpha = opacity)
                        else Color(0xFF6750A4).copy(alpha = opacity)
                    )
            )
        }
    }
}

/**
 * Beautiful, rounded Bottom Card sheet for History & settings trigger.
 */
@Composable
fun BottomNavigationPanel(
    onArchiveClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Top ambient light shadow to separate gracefully
                drawRect(
                    color = Color.Black.copy(alpha = 0.03f),
                    topLeft = Offset(0f, -6.dp.toPx()),
                    size = size.copy(height = 6.dp.toPx())
                )
            },
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // "آرشیو گفتگوها" (History) box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onArchiveClick() }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "تاریخچه صداها",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "آرشیو صداها",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // "تنظیمات" (Settings) box
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSettingsClick() }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "تنظیمات سیستم",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "تنظیمات",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Slide-up archive dialog showing previous voice playbacks.
 */
@Composable
fun ArchiveSheetDialog(
    messages: List<VoiceMessage>,
    motherName: String,
    isProcessing: Boolean,
    viewModel: VoiceChatViewModel,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(28.dp)),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of Dialog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آرشیو گفتگوهای شما",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // List content
                if (messages.isEmpty() && !isProcessing) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "هنوز گفتگویی ضبط نشده است.",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (isProcessing) {
                            item {
                                ProcessingVoicePlaceholder()
                            }
                        }

                        items(messages, key = { it.id }) { msg ->
                            VoiceMessageCard(
                                message = msg,
                                motherName = motherName,
                                viewModel = viewModel
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("برگشت به صفحه اصلی", color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Beautiful Glassmorphic Pastel Voice Bubble Card representing archived voices.
 */
@Composable
fun VoiceMessageCard(
    message: VoiceMessage,
    motherName: String,
    viewModel: VoiceChatViewModel
) {
    val playingPath by viewModel.playingPath.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val progress by viewModel.playbackProgress.collectAsState()

    val isThisPlaying = playingPath == message.audioPath
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .border(
                    width = 1.2.dp,
                    color = if (isUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause button
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                        .clickable { viewModel.playVoice(message.audioPath) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isThisPlaying && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "پخش",
                        tint = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Custom Voice wave visualizer & Label info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUser) "صدای شما" else "پاسخ همدم",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = toPersianNumbers(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sound Wave Visualizer
                    SoundWaveProgressBar(
                        isPlaying = isThisPlaying && isPlaying,
                        progress = if (isThisPlaying) progress else 0f
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Voice length duration tag
                Text(
                    text = formatDuration(message.durationMs),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Soundwave bar list for message boxes.
 */
@Composable
fun SoundWaveProgressBar(
    isPlaying: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val count = 15
        val heights = listOf(8, 18, 12, 24, 10, 20, 14, 26, 12, 16, 22, 12, 18, 8, 6)

        for (i in 0 until count) {
            val isPlayed = (i.toFloat() / count.toFloat()) <= progress
            val baseHeight = heights[i % heights.size].dp

            val pulseMultiplier = if (isPlaying && isPlayed) {
                val transition = rememberInfiniteTransition()
                val scale by transition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.25f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400 + (i * 25), easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                scale
            } else 1.0f

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(baseHeight * pulseMultiplier)
                    .clip(CircleShape)
                    .background(
                        if (isPlayed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
            )
        }
    }
}

/**
 * Beautiful thinking/processing placeholder card while response is being generated.
 */
@Composable
fun ProcessingVoicePlaceholder() {
    val transition = rememberInfiniteTransition()
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = pulseAlpha)),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.tertiary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "همدم در حال ضبط و ایجاد پاسخ مهربان...",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Elegant Persian Settings popup dialogue.
 */
@Composable
fun SettingsDialog(
    viewModel: VoiceChatViewModel,
    onDismiss: () -> Unit
) {
    val motherName by viewModel.motherName.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val baseUrl by viewModel.baseUrl.collectAsState()
    val ttsVoice by viewModel.ttsVoice.collectAsState()
    val autoSilenceStop by viewModel.autoSilenceStop.collectAsState()
    val silenceDelaySeconds by viewModel.silenceDelaySeconds.collectAsState()

    val playingPath by viewModel.playingPath.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    var nameInput by remember { mutableStateOf(motherName) }
    var apiKeyInput by remember { mutableStateOf(apiKey) }
    var urlInput by remember { mutableStateOf(baseUrl) }
    var selectedVoice by remember { mutableStateOf(ttsVoice) }
    var autoSilenceInput by remember { mutableStateOf(autoSilenceStop) }
    var silenceDelayInput by remember { mutableStateOf(silenceDelaySeconds) }

    var confirmDelete by remember { mutableStateOf(false) }
    var loadingSampleVoice by remember { mutableStateOf<String?>(null) }

    val voicesList = listOf(
        Pair("nova", "اختر (پیش‌فرض - بسیار شفاف و صمیمی)"),
        Pair("shimmer", "شیما (شاداب و باانرژی)"),
        Pair("echo", "سهراب (مردانه و آرام)"),
        Pair("onyx", "قباد (مردانه و باصلابت)"),
        Pair("fable", "افسانه (قصه‌گو و با احساس)"),
        Pair("alloy", "نیکو (شمرده و باوقار)")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "تنظیمات همدم صوتی",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Mother's name field
                item {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("نام مادرم", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // API Key field (Unmasked, clearly visible)
                item {
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("کلید API (GapGPT)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Base URL field
                item {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("آدرس سرور اصلی (Base URL)", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Automatic Silence Stop Switch
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "تشخیص سکوت و پایان خودکار ضبط:",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { autoSilenceInput = !autoSilenceInput }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "قطع خودکار ضبط هنگام سکوت",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "اگر مادر صحبت نکند، ضبط خودکار متوقف می‌شود.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoSilenceInput,
                            onCheckedChange = { autoSilenceInput = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                if (autoSilenceInput) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "تاخیر تشخیص سکوت: ${toPersianNumbers(silenceDelayInput.toString())} ثانیه",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = silenceDelayInput.toFloat(),
                                onValueChange = { silenceDelayInput = it.toInt() },
                                valueRange = 2f..7f,
                                steps = 4, // 2, 3, 4, 5, 6, 7
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }

                // Voice selection options
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "صدای دستیار صوتی (لحن پاسخگو):",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(voicesList) { item ->
                    val isSelected = selectedVoice == item.first
                    val isThisVoicePlaying = isPlaying && playingPath?.contains("sample_voice_${item.first}") == true
                    val isLoadingThisSample = loadingSampleVoice == item.first

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedVoice = item.first }
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedVoice = item.first },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item.second,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            // Sample play button or spinner
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                    .clickable {
                                        if (isThisVoicePlaying) {
                                            viewModel.stopVoice()
                                        } else {
                                            loadingSampleVoice = item.first
                                            viewModel.playSampleVoice(
                                                voice = item.first,
                                                text = "سلام مامان عزیز، من صدای دستیار صوتی شما هستم. خوشحالم که می‌توانم با شما صحبت کنم."
                                            ) {
                                                loadingSampleVoice = null
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingThisSample) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isThisVoicePlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                        contentDescription = "شنیدن نمونه صدا",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Reset Archive
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (!confirmDelete) {
                        Button(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "پاک کردن", tint = MaterialTheme.colorScheme.onError)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("پاک کردن آرشیو تمام گفتگوها", color = MaterialTheme.colorScheme.onError, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                                .border(1.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "آیا مطمئن هستید که می‌خواهید تمام آرشیو گفتگوها و فایل‌های صوتی را پاک کنید؟",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = {
                                        viewModel.clearHistory()
                                        confirmDelete = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text("بله، پاک شود", color = MaterialTheme.colorScheme.onError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { confirmDelete = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(100.dp)
                                ) {
                                    Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Control buttons (Save & Cancel)
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateMotherName(nameInput)
                                viewModel.updateApiKey(apiKeyInput)
                                viewModel.updateBaseUrl(urlInput)
                                viewModel.updateTtsVoice(selectedVoice)
                                viewModel.updateAutoSilenceStop(autoSilenceInput)
                                viewModel.updateSilenceDelaySeconds(silenceDelayInput)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("ذخیره", color = MaterialTheme.colorScheme.onPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text("انصراف", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Simple voice message length formatter.
 */
fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return toPersianNumbers(String.format("%d:%02d", min, sec))
}

/**
 * Localized English to Persian numeral converter.
 */
fun toPersianNumbers(str: String): String {
    return str
        .replace('0', '۰')
        .replace('1', '۱')
        .replace('2', '۲')
        .replace('3', '۳')
        .replace('4', '۴')
        .replace('5', '۵')
        .replace('6', '۶')
        .replace('7', '۷')
        .replace('8', '۸')
        .replace('9', '۹')
}
