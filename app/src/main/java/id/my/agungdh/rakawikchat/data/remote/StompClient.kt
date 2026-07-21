package id.my.agungdh.rakawikchat.data.remote

import com.google.gson.Gson
import id.my.agungdh.rakawikchat.data.remote.dto.MessageResponse
import id.my.agungdh.rakawikchat.data.remote.dto.SendMessageRequest
import id.my.agungdh.rakawikchat.util.Constants
import okhttp3.*
import java.util.concurrent.ConcurrentHashMap

class StompClient(private val okHttpClient: OkHttpClient) {

    private var webSocket: WebSocket? = null
    private val subscriptions = ConcurrentHashMap<String, (MessageResponse) -> Unit>()
    private var onConnected: (() -> Unit)? = null
    private var onError: ((Throwable) -> Unit)? = null
    private val gson = Gson()

    private var buffer = StringBuilder()

    fun connect(
        token: String,
        onConnected: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        this.onConnected = onConnected
        this.onError = onError

        val request = Request.Builder()
            .url(Constants.WS_URL)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {

            override fun onOpen(webSocket: WebSocket, response: Response) {
                val connectFrame = "CONNECT\naccept-version:1.2\nAuthorization:Bearer $token\nhost:localhost\n\n\u0000"
                webSocket.send(connectFrame)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                buffer.append(text)
                processFrames()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onError(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                subscriptions.clear()
            }
        })
    }

    private fun processFrames() {
        while (true) {
            val nullIdx = buffer.indexOf('\u0000')
            if (nullIdx == -1) return

            val frame = buffer.substring(0, nullIdx)
            buffer.delete(0, nullIdx + 1)

            parseFrame(frame)
        }
    }

    private fun parseFrame(frame: String) {
        val lines = frame.lines()
        if (lines.isEmpty()) return

        val command = lines[0].trim()

        var blankLineIdx = -1
        for (i in 1 until lines.size) {
            if (lines[i].isEmpty()) {
                blankLineIdx = i
                break
            }
        }

        val headers = mutableMapOf<String, String>()
        val headerEnd = if (blankLineIdx != -1) blankLineIdx else lines.size
        for (i in 1 until headerEnd) {
            val colonIdx = lines[i].indexOf(':')
            if (colonIdx != -1) {
                val key = lines[i].substring(0, colonIdx).trim()
                val value = lines[i].substring(colonIdx + 1).trim()
                headers[key] = value
            }
        }

        val body = if (blankLineIdx != -1 && blankLineIdx + 1 < lines.size) {
            lines.subList(blankLineIdx + 1, lines.size).joinToString("\n")
        } else ""

        when (command) {
            "CONNECTED" -> onConnected?.invoke()
            "MESSAGE" -> {
                val destination = headers["destination"] ?: return
                val callback = subscriptions[destination] ?: return
                try {
                    val msg = gson.fromJson(body, MessageResponse::class.java)
                    callback(msg)
                } catch (_: Exception) {}
            }
            "ERROR" -> {
                onError?.invoke(RuntimeException("STOMP error: $body"))
            }
        }
    }

    fun subscribe(destination: String, callback: (MessageResponse) -> Unit) {
        subscriptions[destination] = callback
        val frame = "SUBSCRIBE\nid:sub-${destination.hashCode()}\ndestination:$destination\n\n\u0000"
        webSocket?.send(frame)
    }

    fun unsubscribe(destination: String) {
        subscriptions.remove(destination)
        val frame = "UNSUBSCRIBE\nid:sub-${destination.hashCode()}\n\n\u0000"
        webSocket?.send(frame)
    }

    fun send(destination: String, message: SendMessageRequest) {
        val body = gson.toJson(message)
        val frame = "SEND\ndestination:$destination\ncontent-type:application/json\n\n$body\u0000"
        webSocket?.send(frame)
    }

    fun disconnect() {
        try {
            webSocket?.send("DISCONNECT\n\n\u0000")
        } catch (_: Exception) {}
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        subscriptions.clear()
        buffer.clear()
    }
}
