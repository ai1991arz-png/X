package pro.xservis.client.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pro.xservis.client.data.scan.NetworkScanner

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scanner: NetworkScanner,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    suspend fun toggleConnection() {
        when (_state.value.connectionState) {
            ConnectionState.Disconnected, ConnectionState.Error -> {
                _state.update { it.copy(connectionState = ConnectionState.Connecting) }
                delay(1200)
                _state.update {
                    it.copy(
                        connectionState = ConnectionState.Connected,
                        subtitle = "Соединение защищено",
                    )
                }
            }
            ConnectionState.Connected -> {
                _state.update { it.copy(connectionState = ConnectionState.Disconnected, subtitle = "Готов к подключению") }
            }
            ConnectionState.Connecting -> Unit
        }
    }

    suspend fun scanNetwork() {
        _state.update { it.copy(scanState = ScanState.Running(0, scanner.totalSteps)) }
        try {
            scanner.scan { step ->
                _state.update {
                    it.copy(scanState = ScanState.Running(step, scanner.totalSteps))
                }
            }.let { result ->
                _state.update {
                    it.copy(
                        scanState = ScanState.Idle,
                        lastScanResult = result,
                    )
                }
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(scanState = ScanState.Failed(e.message ?: "unknown"))
            }
        }
    }

    init {
        viewModelScope.launch {
            // Auto-scan on first open (background)
        }
    }
}

data class HomeUiState(
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val scanState: ScanState = ScanState.Idle,
    val lastScanResult: ScanResult? = null,
    val subtitle: String = "Готов к подключению",
    val subscriptionLabel: String = "Демо · 7 дней",
    val activeServer: String = "Авто (RU)",
)

enum class ConnectionState { Disconnected, Connecting, Connected, Error }

sealed interface ScanState {
    data object Idle : ScanState
    data class Running(val step: Int, val totalSteps: Int) : ScanState
    data class Failed(val message: String) : ScanState
}

data class ScanResult(
    val blockMethod: BlockMethod,
    val recommendedProtocol: String,
    val isp: String?,
    val geo: String?,
    val readinessScore: Int,
)

enum class BlockMethod(val label: String) {
    None("Блокировок не обнаружено"),
    DnsPoisoning("DNS-подмена"),
    SniFilter("SNI-фильтр"),
    IpBlock("Блок по IP"),
    TlsFingerprint("TLS-фингерпринт"),
    DpiThrottle("DPI-троттлинг"),
    FullBlock("Полная блокировка / TSPU"),
}
