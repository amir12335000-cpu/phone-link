package ir.phonelink

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PhoneNotificationListener : NotificationListenerService() {
    private val client = OkHttpClient.Builder()
        .pingInterval(25, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    private var socket: WebSocket? = null
    private val handler = Handler(Looper.getMainLooper())
    private var reconnectQueued = false

    override fun onListenerConnected() {
        super.onListenerConnected()
        connect()
    }

    override fun onListenerDisconnected() {
        socket?.close(1000, "listener disconnected")
        socket = null
        super.onListenerDisconnected()
    }

    private fun connect() {
        val prefs = getSharedPreferences("phone_link", MODE_PRIVATE)
        val server = prefs.getString("server", "") ?: ""
        val code = prefs.getString("code", "") ?: ""
        if (server.isBlank() || code.length != 12) return
        socket?.cancel()
        socket = null
        val request = Request.Builder().url(server).build()
        socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                webSocket.send(JSONObject().put("type", "join")
                    .put("role", "phone").put("code", code).toString())
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                scheduleReconnect()
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectQueued) return
        reconnectQueued = true
        handler.postDelayed({
            reconnectQueued = false
            if (isListenerConnectedCompat()) connect()
        }, 8000)
    }

    private fun isListenerConnectedCompat(): Boolean = socket != null

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        if (n.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0) return
        if (n.flags and android.app.Notification.FLAG_GROUP_SUMMARY != 0) return
        val extras = n.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (title.isBlank() && text.isBlank()) return
        val packet = JSONObject()
            .put("type", "event")
            .put("app", sbn.packageName)
            .put("title", title)
            .put("body", text)
        socket?.send(packet.toString())
    }

    override fun onDestroy() {
        socket?.close(1000, "service destroyed")
        client.dispatcher.executorService.shutdown()
        super.onDestroy()
    }
}