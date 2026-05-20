package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Project
import com.example.data.repository.TheiaRepository
import com.example.service.BlackCloudForegroundService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    RECONNECTING
}

data class Message(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" veya "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val verified: Boolean = true,
    val issues: List<String> = emptyList()
)

data class BlackCloudUiState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val projects: List<Project> = emptyList(),
    val activeProject: Project? = null,
    val chatMessagesByProject: Map<String, List<Message>> = emptyMap(),
    val isLoadingProjects: Boolean = false,
    val isSendingMessage: Boolean = false,
    val inputText: String = "",
    val errorMessage: String? = null,
    val isSimulationMode: Boolean = false // Sunucu kapalıyken deneme yapmak için simülatör modu
)

class BlackCloudViewModel : ViewModel() {

    private val repository = TheiaRepository()
    private val _uiState = MutableStateFlow(BlackCloudUiState())
    val uiState: StateFlow<BlackCloudUiState> = _uiState.asStateFlow()

    init {
        // Arka plan servisinin liveness durumunu otomatik gözlemle
        viewModelScope.launch {
            BlackCloudForegroundService.isBackendAlive.collect { alive ->
                if (!_uiState.value.isSimulationMode) {
                    if (alive) {
                        _uiState.update { 
                            it.copy(
                                connectionStatus = ConnectionStatus.CONNECTED,
                                errorMessage = null
                            ) 
                        }
                        fetchProjects()
                    } else {
                        _uiState.update { 
                            it.copy(
                                connectionStatus = ConnectionStatus.DISCONNECTED,
                                errorMessage = "BLACKCLOUD Çekirdek bağlantısı koptu. Termux'u kontrol edin veya Simülatörü açın."
                            ) 
                        }
                    }
                }
            }
        }
    }

    /**
     * Mesaj giriş alanı değerini günceller.
     */
    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    /**
     * Simülatör (Demo) modunu açıp kapatır. Sunucu kapalıyken test imkanı sunar.
     */
    fun toggleSimulationMode(enabled: Boolean) {
        _uiState.update { it.copy(isSimulationMode = enabled) }
        if (enabled) {
            setupDemoData()
        } else {
            // Gerçek servisi tekrar kontrol et
            reconnect()
        }
    }

    /**
     * Bilişsel projeleri backend servisinden çeker.
     */
    fun fetchProjects() {
        if (_uiState.value.isSimulationMode) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProjects = true, errorMessage = null) }
            try {
                val list = repository.getProjects()
                _uiState.update { state ->
                    val nextActiveProject = state.activeProject ?: list.firstOrNull()
                    state.copy(
                        projects = list,
                        activeProject = nextActiveProject,
                        isLoadingProjects = false,
                        connectionStatus = ConnectionStatus.CONNECTED
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoadingProjects = false,
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        errorMessage = "Projeler alınamadı: Yerel API kapalı veya yanıt vermiyor."
                    ) 
                }
            }
        }
    }

    /**
     * Aktif bilişsel projeyi değiştirir.
     */
    fun selectProject(project: Project) {
        _uiState.update { it.copy(activeProject = project) }
    }

    /**
     * Sunucuya yeniden bağlanmayı dener (Manuel Reconnect butonu için).
     */
    fun reconnect() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    connectionStatus = ConnectionStatus.RECONNECTING,
                    errorMessage = null 
                ) 
            }
            delay(1200) // Gerçekçi hissettiren hafif bir ağ bekleme animasyonu gecikmesi

            val success = repository.ping()
            if (success) {
                _uiState.update { 
                    it.copy(
                        connectionStatus = ConnectionStatus.CONNECTED,
                        errorMessage = null 
                    ) 
                }
                fetchProjects()
            } else {
                _uiState.update { 
                    it.copy(
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        errorMessage = "Bağlantı kurulamadı. http://localhost:8765 adresinde çekirdeğin çalıştığından emin olun."
                    ) 
                }
            }
        }
    }

    /**
     * Aktif proje bağlamında yeni bir sohbet mesajı gönderir.
     */
    fun sendMessage() {
        val state = _uiState.value
        val messageText = state.inputText.trim()
        val currentProject = state.activeProject

        if (messageText.isEmpty() || currentProject == null) return

        // Mesaj listesine kullanıcının gönderdiğini ekle ve girdiyi sıfırla
        val userMsg = Message(sender = "user", text = messageText)
        val projectId = currentProject.id
        
        val currentMessages = state.chatMessagesByProject[projectId] ?: emptyList()
        val updatedMessages = currentMessages + userMsg
        
        _uiState.update { 
            it.copy(
                inputText = "",
                isSendingMessage = true,
                chatMessagesByProject = state.chatMessagesByProject + (projectId to updatedMessages)
            )
        }

        viewModelScope.launch {
            if (state.isSimulationMode) {
                // Simülasyon modu yapay zeka cevap üretimi
                delay(1500)
                val responseMsg = generateSimulatedResponse(messageText)
                val finalMessages = _uiState.value.chatMessagesByProject[projectId] ?: emptyList()
                _uiState.update {
                    it.copy(
                        isSendingMessage = false,
                        chatMessagesByProject = it.chatMessagesByProject + (projectId to (finalMessages + responseMsg))
                    )
                }
            } else {
                // Gerçek Çekirdek Bağlantısı
                try {
                    val response = repository.sendChat(projectId, messageText)
                    val assistantMsg = Message(
                        sender = "assistant",
                        text = response.text,
                        verified = response.kkyp.verified,
                        issues = response.kkyp.issues
                    )
                    
                    val finalMessages = _uiState.value.chatMessagesByProject[projectId] ?: emptyList()
                    _uiState.update {
                        it.copy(
                            isSendingMessage = false,
                            chatMessagesByProject = it.chatMessagesByProject + (projectId to (finalMessages + assistantMsg))
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { 
                        it.copy(
                            isSendingMessage = false,
                            connectionStatus = ConnectionStatus.DISCONNECTED,
                            errorMessage = "Mesaj gönderilemedi: Yerel çekirdek bağlantısı kaybedildi."
                        ) 
                    }
                }
            }
        }
    }

    /**
     * Simülatör Modu için başlangıç veri kurulumu.
     */
    private fun setupDemoData() {
        val demoProjects = listOf(
            Project("pt-theia", "theia-soul", listOf("biliş", "bellek", "kişisel"), "2026-05-18"),
            Project("pt-perma", "manisa-permakultur", listOf("tarım", "organik", "ekoloji"), "2026-05-15"),
            Project("pt-core", "blackcloud-core", listOf("sistem", "terminal", "yerel"), "2026-05-20")
        )

        val demoHistory = mapOf(
            "pt-theia" to listOf(
                Message(sender = "assistant", text = "Merhaba, ben senin Blackcloud bilişsel ikizin. theia-soul projesi altındaki tüm kişisel arşivlerini okudum. Nasıl yardımcı olabilirim?", verified = true)
            ),
            "pt-perma" to listOf(
                Message(sender = "assistant", text = "Manisa Permakültür projesi aktif. Toprak analizi, sulama döngüleri ve organik tarım dokümanları yüklendi. Neye odaklanalım?", verified = true)
            ),
            "pt-core" to listOf(
                Message(sender = "assistant", text = "BLACKCLOUD Çekirdek Sistemi izleme paneli. Yerel Python sunucun 8765 portunda kararlı. Dosya sistemine direkt erişimim var.", verified = true)
            )
        )

        _uiState.update {
            it.copy(
                projects = demoProjects,
                activeProject = demoProjects.first(),
                chatMessagesByProject = demoHistory,
                connectionStatus = ConnectionStatus.CONNECTED,
                errorMessage = null
            )
        }
    }

    /**
     * Simulator için zekice ve KKYP test etmeye elverişli dinamik yanıtlar üretir.
     */
    private fun generateSimulatedResponse(prompt: String): Message {
        val lowerPrompt = prompt.lowercase()
        return when {
            lowerPrompt.contains("grounding") || lowerPrompt.contains("kaynak") || lowerPrompt.contains("hata") || lowerPrompt.contains("şüphe") -> {
                Message(
                    sender = "assistant",
                    text = "Bu konuda yerel dosya sistemindeki 'perma-docs/analiz.txt' belgesinde çelişkili veya eksik bilgiler tespit ettim. Dış buluttan aldığım bilgi yerel bağlamın dışına çıkıyor olabilir.",
                    verified = false,
                    issues = listOf("Zayıf Bağlantı (Weak Grounding)", "Yerel kaynaklarda bu veriyi doğrulayan dosya bulunamadı", "Kaynak belgesinde çelişkili tarih ifadeleri (2025 vs 2026)")
                )
            }
            lowerPrompt.contains("nerede") || lowerPrompt.contains("dosya") || lowerPrompt.contains("data") -> {
                Message(
                    sender = "assistant",
                    text = "Sorguladığın veri yerel `~/.blackcloud/vault/` dizinindeki şifreli veritabanında saklanmaktadır. Bu veri dışarı hiçbir API veya sunucuya sızdırılmadan tamamen bu Android kabuğunda ve senin Termux ortamında çalışır.",
                    verified = true,
                    issues = emptyList()
                )
            }
            else -> {
                Message(
                    sender = "assistant",
                    text = "'$prompt' sorunu analiz ettim. Bilişsel ikiz bellek katmanında yaptığım taramaya göre, bu talebin yerel çekirdekteki veri yapılarıyla uyumlu ve güvenle doğrulanmıştır. İndekslenen 14 dikey doküman ile eşleşme başarıyla sağlandı.",
                    verified = true,
                    issues = emptyList()
                )
            }
        }
    }
}
