package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.ui.theme.*
import com.example.ui.viewmodel.BlackCloudUiState
import com.example.ui.viewmodel.BlackCloudViewModel
import com.example.ui.viewmodel.ConnectionStatus
import com.example.ui.viewmodel.Message
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlackCloudMainScreen(
    viewModel: BlackCloudViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonBlack),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Terminal Simgesi",
                            tint = NeonCyan,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "BLACKCLOUD",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = NeonCyan
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.connectionStatus == ConnectionStatus.CONNECTED || uiState.isSimulationMode) NeonGreen else GlowRed)
                                )
                                Text(
                                    text = if (uiState.connectionStatus == ConnectionStatus.CONNECTED || uiState.isSimulationMode) "LOCALKABUK AKTİF" else "ÇEKİRDEK BAĞLANTISI YOK",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (uiState.connectionStatus == ConnectionStatus.CONNECTED || uiState.isSimulationMode) NeonGreen.copy(alpha = 0.8f) else GlowRed
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Simülatör (Demo) Modu Togglesı
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = "Simülatör",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (uiState.isSimulationMode) NeonCyan else TextSecondary
                        )
                        Switch(
                            checked = uiState.isSimulationMode,
                            onCheckedChange = { viewModel.toggleSimulationMode(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CarbonBlack,
                                checkedTrackColor = NeonCyan,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SlateSurface
                            ),
                            modifier = Modifier
                                .scale(0.8f)
                                .testTag("simulation_switch")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CarbonBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = CarbonBlack
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(CarbonBlack)
        ) {
            // 1. Bağlantı Durumu ve Yeniden Bağlan Butonu
            ConnectionStatusBar(
                status = uiState.connectionStatus,
                isSimulationMode = uiState.isSimulationMode,
                errorMessage = uiState.errorMessage,
                onReconnectClick = { viewModel.reconnect() }
            )

            // 2. Proje Seçici Bölümü (Yatay Kartlar)
            ProjectSwitcherSection(
                projects = uiState.projects,
                activeProject = uiState.activeProject,
                isLoading = uiState.isLoadingProjects,
                onProjectSelected = { viewModel.selectProject(it) }
            )

            // 3. Sohbet Bölümü
            val activeProjId = uiState.activeProject?.id ?: ""
            val currentMessages = uiState.chatMessagesByProject[activeProjId] ?: emptyList()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.activeProject == null) {
                    EmptyStateView(
                        message = "Bilişsel ikiz bağlantısı kuruldu fakat yüklü proje bulunamadı.\nLütfen yerel çekirdeği başlatın veya Simülatörü aktif edin."
                    )
                } else if (currentMessages.isEmpty()) {
                    EmptyStateView(
                        message = "[${uiState.activeProject?.name}] projesi seçildi.\nBu siber bellekle ilgili sorularınızı yöneltebilirsiniz.\nKKYP doğrulama sistemi aktiftir."
                    )
                } else {
                    ChatWorkspace(
                        messages = currentMessages,
                        isSending = uiState.isSendingMessage,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // 4. Yazıyor/Düşünüyor Göstergesi ve Mesaj Giriş Alanı
            InputSection(
                inputText = uiState.inputText,
                isSending = uiState.isSendingMessage,
                activeProject = uiState.activeProject,
                connectionStatus = uiState.connectionStatus,
                onTextChange = { viewModel.onInputTextChanged(it) },
                onSendClick = {
                    viewModel.sendMessage()
                    focusManager.clearFocus()
                }
            )
        }
    }
}

/**
 * Modern siber bağlantı durum göstergesi. Blinking animasyonu ile canlandırılmıştır.
 */
@Composable
fun ConnectionStatusBar(
    status: ConnectionStatus,
    isSimulationMode: Boolean,
    errorMessage: String?,
    onReconnectClick: () -> Unit
) {
    // Nabız atışı gibi yanıp sönen animasyon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val (bgColor, textColor, statusText, statusDotColor) = when {
        isSimulationMode -> listOf(
            SlateCard,
            NeonCyan,
            "SİMÜLASYON MODU (AKTİF)",
            NeonCyan
        )
        status == ConnectionStatus.CONNECTED -> listOf(
            SlateCard,
            NeonGreen,
            "ÇEKİRDEK BAĞLANTISI: AKTİF",
            NeonGreen
        )
        status == ConnectionStatus.RECONNECTING -> listOf(
            SlateCard,
            WarnOrange,
            "BAĞLANTI DENENİYOR...",
            WarnOrange
        )
        else -> listOf(
            SlateCard,
            GlowRed,
            "ÇEKİRDEK BAĞLANTISI KESİLDİ",
            GlowRed
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CarbonBlack)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bgColor as Color),
            border = BorderStroke(1.dp, (textColor as Color).copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("connect_status_row")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Yanıp sönen ışık
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background((statusDotColor as Color).copy(alpha = alphaAnim))
                    )
                    Text(
                        text = statusText as String,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = textColor
                    )
                }

                if (status == ConnectionStatus.DISCONNECTED && !isSimulationMode) {
                    Button(
                        onClick = onReconnectClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlowRed.copy(alpha = 0.2f),
                            contentColor = PureWhite
                        ),
                        border = BorderStroke(1.dp, GlowRed),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("reconnect_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Yeniden Bağlan",
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "BAĞLAN",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Hata açıklaması
        if (status == ConnectionStatus.DISCONNECTED && !isSimulationMode && errorMessage != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                color = GlowRed.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, GlowRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Hata Uyarı",
                        tint = GlowRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = errorMessage,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Aktif projeleri listelediğimiz modern yatay siber seçici.
 */
@Composable
fun ProjectSwitcherSection(
    projects: List<Project>,
    activeProject: Project?,
    isLoading: Boolean,
    onProjectSelected: (Project) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "KİŞİSEL COGNITIVE PROJELER",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = NeonCyan,
                    strokeWidth = 2.dp
                )
            }
        } else if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(CarbonBlack)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "[Bağlantı algılanmadığı için proje listesi boş. Lütfen simülasyonu açın veya yerel API'yi çalıştırın.]",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                    textAlign = TextAlign.Start
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("project_switcher_deck")
            ) {
                items(projects) { project ->
                    val isActive = project.id == activeProject?.id
                    ProjectCard(
                        project = project,
                        isActive = isActive,
                        onClick = { onProjectSelected(project) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isActive) NeonCyan else TextMuted
    val bgGrad = if (isActive) {
        Brush.verticalGradient(listOf(NeonCyan, NeonCyan))
    } else {
        Brush.verticalGradient(listOf(SlateCard, CarbonBlack))
    }

    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}"),
        border = BorderStroke(1.5.dp, borderColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .background(bgGrad)
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Default.FolderOpen else Icons.Default.Folder,
                        contentDescription = "Proje Klasörü",
                        tint = if (isActive) DeepPurple else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(DeepPurple)
                        )
                    }
                }

                Text(
                    text = project.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isActive) DeepPurple else TextPrimary,
                    maxLines = 1
                )

                // Etiketler (Tags)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    project.tags.take(2).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isActive) DeepPurple.copy(alpha = 0.15f) else SlateSurface,
                            modifier = Modifier.padding(0.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 9.sp,
                                color = if (isActive) DeepPurple else TextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Oluşturulma: ${project.createdAt}",
                    fontSize = 9.sp,
                    color = if (isActive) DeepPurple.copy(alpha = 0.8f) else TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/**
 * Sohbet akış alanı.
 */
@Composable
fun ChatWorkspace(
    messages: List<Message>,
    isSending: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Liste güncellendikçe otomatik olarak en aşağı kaydırır
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.testTag("chat_lazy_column")
    ) {
        items(messages) { message ->
            ChatItem(message = message)
        }

        if (isSending) {
            item {
                AssistantThinkingProgress()
            }
        }
    }
}

@Composable
fun ChatItem(message: Message) {
    val isUser = message.sender == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "Bilişsel İkiz",
                tint = NeonCyan,
                modifier = Modifier
                    .padding(end = 8.dp, top = 4.dp)
                    .size(28.dp)
            )
        }

        Column(
            modifier = Modifier.widthIn(max = 290.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            // Mesaj Baloncuğu
            val bubbleBg = if (isUser) SlateSurface else SlateCard
            val bubbleBorder = if (isUser) NeonCyan.copy(alpha = 0.2f) else TextMuted.copy(alpha = 0.3f)
            val bubbleShape = if (isUser) {
                RoundedCornerShape(16.dp, 16.dp, 2.dp, 16.dp)
            } else {
                RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
            }

            Surface(
                color = bubbleBg,
                shape = bubbleShape,
                border = BorderStroke(1.dp, bubbleBorder),
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = message.text,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            // KKYP Metadatası (Sadece Yapay Zekadan gelenler için)
            if (!isUser) {
                Spacer(modifier = Modifier.height(4.dp))
                KkypIndicatorBadge(verified = message.verified, issues = message.issues)
            }
        }
    }
}

/**
 * Kaynak-Kilitli Yanıt Protokolü (KKYP) gösterge rozeti ve paneli.
 * Eğer doğrulanmadıysa sohbet akışını aksatmadan hemen altında uyarı bayrağı gösterir.
 */
@Composable
fun KkypIndicatorBadge(
    verified: Boolean,
    issues: List<String>
) {
    if (verified) {
        // Doğrulanmış Kaynak Rozeti
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = NeonGreen.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
            modifier = Modifier.testTag("kkyp_verified_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                )
                Text(
                    text = "✓ KKYP DOĞRULANDI (Yerel Kaynak)",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = NeonGreen
                )
            }
        }
    } else {
        // Zayıf Bağlam / Sapma Uyarı Paneli
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = WarnOrange.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, WarnOrange.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("kkyp_warning_panel")
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Kaynak Sapma Uyarısı",
                        tint = WarnOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "KAYNAKTAN SAPILDI / ZAYIF BAĞLAM",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = WarnOrange
                    )
                }
                
                Text(
                    text = "Açıklama: Model çıktısı yerel arşivlerin dışından alındı veya kaynak belgelenmedi.",
                    fontSize = 11.sp,
                    color = TextPrimary
                )

                if (issues.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(WarnOrange.copy(alpha = 0.2f))
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = "Algılanan Çelişkiler/Eksikler:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                    
                    issues.forEach { issue ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        ) {
                            Text("•", color = WarnOrange, fontSize = 11.sp)
                            Text(
                                text = issue,
                                fontSize = 10.sp,
                                color = TextPrimary,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Yapay Zekadan yanıt beklenirken canlanan modern düşünen terminal göstergesi.
 */
@Composable
fun AssistantThinkingProgress() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinking_scale"
    )

    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = "Bilişsel Düşünme",
            tint = NeonCyan,
            modifier = Modifier.size(28.dp)
        )
        Surface(
            color = SlateCard,
            shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 2.dp),
            border = BorderStroke(1.dp, TextMuted)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "ÇEKİRDEK DÜŞÜNÜYOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
                // Yanıp sönen noktalar yerine küçük yeşil terminal gözü
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
            }
        }
    }
}

/**
 * Giriş alanı ve gönder kontrol şeridi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputSection(
    inputText: String,
    isSending: Boolean,
    activeProject: Project?,
    connectionStatus: ConnectionStatus,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    val isConnected = connectionStatus == ConnectionStatus.CONNECTED
    // Girdi aktiflik izni
    val isEnabled = activeProject != null && !isSending && isConnected

    val searchContainerColor = ElegantInputBg

    Surface(
        color = CarbonBlack,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // Sistem navigasyon tuşları ile çakışmayı önler
            .imePadding() // Klavye yüksekliğine duyarlı kaydırma sağlar
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(
                    value = inputText,
                    onValueChange = onTextChange,
                    placeholder = {
                        Text(
                            text = if (activeProject == null) {
                                "Lütfen bir proje seçin..."
                            } else if (!isConnected) {
                                "Bağlantı bekleniyor..."
                            } else {
                                "${activeProject.name} projesine yazın..."
                            },
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("msg_input_field"),
                    enabled = isEnabled,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = searchContainerColor,
                        unfocusedContainerColor = searchContainerColor,
                        disabledContainerColor = SlateCard.copy(alpha = 0.5f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        disabledTextColor = TextSecondary,
                        cursorColor = NeonCyan,
                        focusedIndicatorColor = NeonCyan,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )

                val showSendGlow = inputText.trim().isNotEmpty() && isEnabled
                IconButton(
                    onClick = onSendClick,
                    enabled = showSendGlow,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (showSendGlow) NeonCyan else SlateSurface)
                        .size(48.dp)
                        .testTag("send_msg_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Mesajı Gönder",
                        tint = if (showSendGlow) CarbonBlack else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = "Boş Durum",
                tint = TextMuted,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = message,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}


