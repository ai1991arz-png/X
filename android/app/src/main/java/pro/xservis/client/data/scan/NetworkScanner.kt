package pro.xservis.client.data.scan

import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import pro.xservis.client.ui.screens.home.BlockMethod
import pro.xservis.client.ui.screens.home.ScanResult

/**
 * Performs a battery of network probes to fingerprint how the user's ISP
 * is interfering with traffic. The result drives auto-selection of a
 * protocol (Reality / AmneziaWG / Shadowsocks-2022 / etc.).
 */
@Singleton
class NetworkScanner @Inject constructor() {

    val totalSteps: Int = STEPS.size

    suspend fun scan(onStep: suspend (Int) -> Unit): ScanResult = withContext(Dispatchers.IO) {
        val outcomes = mutableMapOf<String, ProbeOutcome>()
        STEPS.forEachIndexed { idx, step ->
            onStep(idx + 1)
            outcomes[step.id] = runCatching { step.probe() }
                .getOrElse { ProbeOutcome.Failed(it.message ?: it::class.java.simpleName) }
            // Small jitter so the UI shows progress smoothly
            delay(120)
        }
        classify(outcomes)
    }

    private fun classify(outcomes: Map<String, ProbeOutcome>): ScanResult {
        val dnsBroken = outcomes["dns_youtube"]?.isFailure() == true
        val tlsBroken = outcomes["tls_youtube"]?.isFailure() == true ||
            outcomes["tls_discord"]?.isFailure() == true
        val tcpReachable = outcomes["tcp_443"]?.isSuccess() == true
        val httpOk = outcomes["http_clean"]?.isSuccess() == true
        val sniIssue = outcomes["sni_filter"]?.isFailure() == true

        val (method, protocol) = when {
            dnsBroken && !tcpReachable -> BlockMethod.FullBlock to "AmneziaWG (anti-DPI)"
            sniIssue && tlsBroken -> BlockMethod.SniFilter to "VLESS-Reality + Vision"
            dnsBroken && httpOk -> BlockMethod.DnsPoisoning to "DoH + VLESS-Reality"
            tlsBroken && tcpReachable -> BlockMethod.TlsFingerprint to "Shadowsocks-2022"
            !tcpReachable -> BlockMethod.IpBlock to "AmneziaWG (CDN)"
            outcomes["throttle_test"]?.isFailure() == true -> BlockMethod.DpiThrottle to "VLESS-XTLS-Vision"
            else -> BlockMethod.None to "VLESS-Reality"
        }

        val readiness = when (method) {
            BlockMethod.None -> 100
            BlockMethod.DnsPoisoning, BlockMethod.SniFilter -> 92
            BlockMethod.TlsFingerprint -> 86
            BlockMethod.IpBlock -> 78
            BlockMethod.DpiThrottle -> 88
            BlockMethod.FullBlock -> 70
        }

        return ScanResult(
            blockMethod = method,
            recommendedProtocol = protocol,
            isp = outcomes["isp"]?.message(),
            geo = outcomes["geo"]?.message(),
            readinessScore = readiness,
        )
    }

    private data class Step(
        val id: String,
        val probe: () -> ProbeOutcome,
    )

    sealed class ProbeOutcome {
        data class Success(val info: String? = null) : ProbeOutcome()
        data class Failed(val reason: String) : ProbeOutcome()

        fun isSuccess() = this is Success
        fun isFailure() = this is Failed
        fun message(): String = when (this) {
            is Success -> info ?: "ok"
            is Failed -> reason
        }
    }

    companion object {
        private val STEPS = listOf(
            Step("dns_youtube") {
                runCatching {
                    val addr = InetAddress.getByName("youtube.com")
                    if (addr.hostAddress.startsWith("0.") ||
                        addr.hostAddress.startsWith("127.") ||
                        addr.hostAddress.startsWith("100.10.")
                    ) {
                        ProbeOutcome.Failed("dns hijack: ${addr.hostAddress}")
                    } else {
                        ProbeOutcome.Success(addr.hostAddress)
                    }
                }.getOrElse { ProbeOutcome.Failed("dns: ${it.message}") }
            },
            Step("tcp_443") {
                runCatching {
                    Socket("youtube.com", 443).use { s -> s.isConnected }
                    ProbeOutcome.Success()
                }.getOrElse { ProbeOutcome.Failed("tcp 443: ${it.message}") }
            },
            Step("tls_youtube") {
                tlsHandshake("youtube.com")
            },
            Step("tls_discord") {
                tlsHandshake("discord.com")
            },
            Step("sni_filter") {
                runCatching {
                    Socket("www.googlevideo.com", 443).use { s ->
                        val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                        factory.createSocket(s, "www.googlevideo.com", 443, true)
                        ProbeOutcome.Success()
                    }
                }.getOrElse { ProbeOutcome.Failed("sni: ${it.message}") }
            },
            Step("http_clean") {
                runCatching {
                    val conn = (URL("https://cloudflare.com/cdn-cgi/trace").openConnection()
                        as HttpURLConnection).apply { connectTimeout = 4000; readTimeout = 4000 }
                    val ok = conn.responseCode == 200
                    conn.disconnect()
                    if (ok) ProbeOutcome.Success() else ProbeOutcome.Failed("http ${conn.responseCode}")
                }.getOrElse { ProbeOutcome.Failed("http: ${it.message}") }
            },
            Step("throttle_test") {
                runCatching {
                    val start = System.currentTimeMillis()
                    val conn = (URL("https://speed.cloudflare.com/__down?bytes=200000").openConnection()
                        as HttpURLConnection).apply { connectTimeout = 4000; readTimeout = 6000 }
                    val bytes = conn.inputStream.use { it.readBytes().size }
                    val elapsed = System.currentTimeMillis() - start
                    val kbps = if (elapsed > 0) bytes * 8L / elapsed else 0
                    if (kbps < 200) ProbeOutcome.Failed("slow: ${kbps}kbps")
                    else ProbeOutcome.Success("${kbps}kbps")
                }.getOrElse { ProbeOutcome.Failed("throttle: ${it.message}") }
            },
            Step("isp") {
                runCatching {
                    val body = (URL("https://1.1.1.1/cdn-cgi/trace").openConnection() as HttpURLConnection)
                        .apply { connectTimeout = 4000; readTimeout = 4000 }
                        .inputStream.bufferedReader().readText()
                    val asn = Regex("colo=([A-Z]{3})").find(body)?.groupValues?.get(1)
                    ProbeOutcome.Success(asn)
                }.getOrElse { ProbeOutcome.Failed("isp: ${it.message}") }
            },
            Step("geo") {
                runCatching {
                    val body = (URL("https://1.1.1.1/cdn-cgi/trace").openConnection() as HttpURLConnection)
                        .apply { connectTimeout = 4000; readTimeout = 4000 }
                        .inputStream.bufferedReader().readText()
                    val country = Regex("loc=([A-Z]{2})").find(body)?.groupValues?.get(1)
                    ProbeOutcome.Success(country)
                }.getOrElse { ProbeOutcome.Failed("geo: ${it.message}") }
            },
        )

        private fun tlsHandshake(host: String): ProbeOutcome = runCatching {
            (SSLSocketFactory.getDefault().createSocket(host, 443) as javax.net.ssl.SSLSocket).use { sock ->
                sock.soTimeout = 4000
                sock.startHandshake()
                ProbeOutcome.Success(sock.session.cipherSuite)
            }
        }.getOrElse { e ->
            when (e) {
                is SSLHandshakeException -> ProbeOutcome.Failed("tls handshake")
                else -> ProbeOutcome.Failed("tls: ${e.message}")
            }
        }
    }
}
