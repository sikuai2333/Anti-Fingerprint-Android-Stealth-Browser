package com.phantommax.app

import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocket
import javax.net.ssl.SNIHostName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch

object ProxyManager {

    @Volatile
    var currentProxy: ProxyConfig? = null
        private set

    private var localSocksServer: ServerSocket? = null
    private var executor: ExecutorService? = null
    private val running = AtomicBoolean(false)

    @Volatile
    private var vlessActive = false

    @Volatile
    var lastPingMs: Long = -1
        private set

    @Volatile
    var detectedCountry: String = ""
        private set

    @Volatile
    var detectedIp: String = ""
        private set

    @Volatile
    private var bypassDomains: Set<String> = setOf("localhost", "127.0.0.1", ".local")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun isVlessActive(): Boolean = vlessActive

    fun setBypassRules(csv: String) {
        bypassDomains = csv.split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    fun getBypassRulesCsv(): String = bypassDomains.joinToString(",")

    fun shouldBypassHost(host: String?): Boolean {
        val h = host?.trim()?.lowercase().orEmpty()
        if (h.isBlank()) return false
        return bypassDomains.any { rule ->
            when {
                rule.startsWith(".") -> h.endsWith(rule)
                else -> h == rule
            }
        }
    }

    fun routeForUrl(url: String?): String {
        val host = runCatching { java.net.URI(url ?: "").host }.getOrNull()
        return if (shouldBypassHost(host)) "DIRECT" else "PROXY"
    }

    fun ping(config: ProxyConfig, callback: (Long) -> Unit) {
        scope.launch {
            try {
                val start = System.currentTimeMillis()
                val socket = Socket()
                socket.connect(InetSocketAddress(config.host, config.port), 3000)
                val ping = System.currentTimeMillis() - start
                socket.close()
                callback(ping)
            } catch (_: Exception) {
                callback(-1)
            }
        }
    }

    fun connect(config: ProxyConfig, callback: (Boolean, String) -> Unit) {
        disconnect()
        currentProxy = config

        when (config.type) {
            ProxyConfig.Type.SOCKS5 -> {
                scope.launch {
                    try {
                        val start = System.currentTimeMillis()
                        val testSocket = Socket()
                        testSocket.connect(InetSocketAddress(config.host, config.port), 7000)
                        lastPingMs = System.currentTimeMillis() - start
                        testSocket.close()
                        callback(true, "")
                        fetchIpInfoAsync()
                    } catch (e: Exception) {
                        currentProxy = null
                        callback(false, e.message ?: "Connection failed")
                    }
                }
            }
            ProxyConfig.Type.HTTP -> {
                scope.launch {
                    try {
                        val start = System.currentTimeMillis()
                        val jProxy = java.net.Proxy(java.net.Proxy.Type.HTTP, InetSocketAddress(config.host, config.port))
                        val conn = java.net.URL("http://www.gstatic.com/generate_204").openConnection(jProxy) as java.net.HttpURLConnection
                        conn.connectTimeout = 7000
                        conn.readTimeout = 7000
                        conn.connect()
                        lastPingMs = System.currentTimeMillis() - start
                        conn.disconnect()
                        callback(true, "")
                        fetchIpInfoAsync()
                    } catch (e: Exception) {
                        currentProxy = null
                        callback(false, e.message ?: "HTTP proxy connection failed")
                    }
                }
            }
            ProxyConfig.Type.VLESS -> {
                startVlessProxy(config, callback)
            }
        }
    }

    fun disconnect() {
        scope.coroutineContext.cancelChildren()
        running.set(false)
        try { localSocksServer?.close() } catch (_: Exception) {}
        localSocksServer = null
        executor?.shutdownNow()
        executor = null
        vlessActive = false
        currentProxy = null
        lastPingMs = -1
        detectedCountry = ""
        detectedIp = ""
    }

    fun refreshIpInfo() {
        fetchIpInfoAsync()
    }

    private fun startVlessProxy(config: ProxyConfig, callback: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                running.set(true)
                executor = Executors.newCachedThreadPool()
                localSocksServer = ServerSocket(10808, 50, java.net.InetAddress.getByName("127.0.0.1"))
                vlessActive = true

                callback(true, "")

                scope.launch {
                    try {
                        val start = System.currentTimeMillis()
                        val testSock = createVlessTunnel(config, "www.google.com", 80)
                        lastPingMs = System.currentTimeMillis() - start
                        testSock?.close()
                        fetchIpInfoAsync()
                    } catch (_: Exception) {}
                }

                while (running.get()) {
                    try {
                        val client = localSocksServer?.accept() ?: break
                        executor?.submit { handleSocks5Client(client, config) }
                    } catch (e: Exception) {
                        if (running.get()) continue else break
                    }
                }
            } catch (e: Exception) {
                vlessActive = false
                currentProxy = null
                callback(false, e.message ?: "VLESS start failed")
            }
        }
    }

    private fun handleSocks5Client(client: Socket, config: ProxyConfig) {
        try {
            val input = client.getInputStream()
            val output = client.getOutputStream()

            val version = input.read()
            if (version != 0x05) { client.close(); return }

            val nmethods = input.read()
            val methods = ByteArray(nmethods)
            input.read(methods)

            output.write(byteArrayOf(0x05, 0x00))
            output.flush()

            input.read()
            val cmd = input.read()
            input.read()
            val atyp = input.read()

            if (cmd != 0x01) {
                output.write(byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()
                client.close()
                return
            }

            val destHost: String
            val destPort: Int

            when (atyp) {
                0x01 -> {
                    val addr = ByteArray(4)
                    input.read(addr)
                    destHost = addr.joinToString(".") { (it.toInt() and 0xFF).toString() }
                }
                0x03 -> {
                    val len = input.read()
                    val domain = ByteArray(len)
                    input.read(domain)
                    destHost = String(domain)
                }
                0x04 -> {
                    val addr = ByteArray(16)
                    input.read(addr)
                    destHost = java.net.InetAddress.getByAddress(addr).hostAddress ?: "::1"
                }
                else -> { client.close(); return }
            }

            val portHigh = input.read()
            val portLow = input.read()
            destPort = (portHigh shl 8) or portLow

            try {
                val remote = createVlessTunnel(config, destHost, destPort)
                if (remote == null) {
                    output.write(byteArrayOf(0x05, 0x05, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                    output.flush()
                    client.close()
                    return
                }

                output.write(byteArrayOf(0x05, 0x00, 0x00, 0x01, 127.toByte(), 0, 0, 1, (10808 shr 8).toByte(), (10808 and 0xFF).toByte()))
                output.flush()

                val remoteIn = remote.getInputStream()
                val remoteOut = remote.getOutputStream()

                val t1 = Thread { relay(input, remoteOut) }
                val t2 = Thread { relay(remoteIn, output) }
                t1.start()
                t2.start()
                t1.join()
                t2.join()

                remote.close()
            } catch (e: Exception) {
                output.write(byteArrayOf(0x05, 0x05, 0x00, 0x01, 0, 0, 0, 0, 0, 0))
                output.flush()
            }

            client.close()
        } catch (_: Exception) {
            try { client.close() } catch (_: Exception) {}
        }
    }

    private fun createVlessTunnel(config: ProxyConfig, destHost: String, destPort: Int): Socket? {
        return try {
            val factory = javax.net.ssl.SSLSocketFactory.getDefault() as javax.net.ssl.SSLSocketFactory
            val rawSocket = Socket()
            rawSocket.connect(InetSocketAddress(config.host, config.port), 10000)

            val sslSocket = factory.createSocket(rawSocket, config.sni.ifEmpty { config.host }, config.port, true) as SSLSocket

            val params = sslSocket.sslParameters
            val targetHost = config.sni.ifEmpty { config.host }
            params.serverNames = listOf(SNIHostName(targetHost))
            params.endpointIdentificationAlgorithm = "HTTPS"

            val protocols = arrayOf("TLSv1.3", "TLSv1.2")
            params.protocols = protocols
            sslSocket.sslParameters = params

            sslSocket.startHandshake()
            val verified = HttpsURLConnection.getDefaultHostnameVerifier()
                .verify(targetHost, sslSocket.session)
            if (!verified) {
                sslSocket.close()
                return null
            }

            val sslOut = sslSocket.getOutputStream()
            val vlessHeader = buildVlessHeader(config.uuid, destHost, destPort)
            sslOut.write(vlessHeader)
            sslOut.flush()

            val sslIn = sslSocket.getInputStream()
            sslIn.read()
            val respAddonLen = sslIn.read()
            if (respAddonLen > 0) {
                val addon = ByteArray(respAddonLen)
                sslIn.read(addon)
            }

            sslSocket
        } catch (e: Exception) {
            null
        }
    }

    private fun buildVlessHeader(uuidStr: String, destHost: String, destPort: Int): ByteArray {
        val uuid = UUID.fromString(uuidStr)
        val uuidBytes = ByteBuffer.allocate(16)
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .array()

        val buf = ByteBuffer.allocate(512)
        buf.put(0x00)
        buf.put(uuidBytes)
        buf.put(0x00)
        buf.put(0x01)
        buf.putShort(destPort.toShort())

        val isIp = destHost.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))
        if (isIp) {
            buf.put(0x01)
            val parts = destHost.split(".")
            for (p in parts) buf.put(p.toInt().toByte())
        } else {
            buf.put(0x02)
            val domainBytes = destHost.toByteArray()
            buf.put(domainBytes.size.toByte())
            buf.put(domainBytes)
        }

        val result = ByteArray(buf.position())
        buf.flip()
        buf.get(result)
        return result
    }

    private fun relay(input: InputStream, output: OutputStream) {
        try {
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                output.flush()
            }
        } catch (_: Exception) {
        } finally {
            try { output.close() } catch (_: Exception) {}
        }
    }

    private fun fetchIpInfoAsync() {
        scope.launch { fetchIpInfo() }
    }

    private fun fetchIpInfo() {
        val endpoints = listOf(
            "https://ipapi.co/json/",
            "https://ipinfo.io/json",
            "https://api.myip.com",
            "https://ip.seeip.org/geoip"
        )
        for (endpoint in endpoints) {
            try {
                val conn = when {
                    vlessActive -> {
                        val socks = java.net.Proxy(java.net.Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", 10808))
                        java.net.URL(endpoint).openConnection(socks)
                    }
                    currentProxy != null -> {
                        val host = runCatching { java.net.URL(endpoint).host }.getOrNull()
                        if (shouldBypassHost(host)) {
                            java.net.URL(endpoint).openConnection()
                        } else {
                        val p = currentProxy!!
                        val proxy = when (p.type) {
                            ProxyConfig.Type.SOCKS5 -> java.net.Proxy(java.net.Proxy.Type.SOCKS, InetSocketAddress(p.host, p.port))
                            ProxyConfig.Type.HTTP   -> java.net.Proxy(java.net.Proxy.Type.HTTP,  InetSocketAddress(p.host, p.port))
                            else -> java.net.Proxy.NO_PROXY
                        }
                        java.net.URL(endpoint).openConnection(proxy)
                        }
                    }
                    else -> java.net.URL(endpoint).openConnection()
                }
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                val body = conn.getInputStream().bufferedReader().readText()
                val ipMatch = Regex("\"ip\"\\s*:\\s*\"([^\"]+)\"").find(body)
                    ?: Regex("\"IPv4\"\\s*:\\s*\"([^\"]+)\"").find(body)
                if (ipMatch != null) {
                    detectedIp = ipMatch.groupValues[1]
                    val countryMatch = Regex("\"country(?:_name)?\"\\s*:\\s*\"([^\"]+)\"").find(body)
                    detectedCountry = countryMatch?.groupValues?.get(1) ?: ""
                    return
                }
            } catch (_: Exception) {}
        }
    }
}
