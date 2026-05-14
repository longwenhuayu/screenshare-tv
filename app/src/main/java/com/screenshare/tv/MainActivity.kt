package com.screenshare.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.LinearLayout

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private val prefs by lazy { getPreferences(Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterFullscreen()

        webView = WebView(this).also {
            configureWebView(it)
            setContentView(it)
        }

        val ip = prefs.getString("server_ip", null)
        if (ip.isNullOrBlank()) showIpDialog() else loadReceiver(ip)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(wv: WebView) {
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
        }

        // 授予 WebRTC 所需的权限请求（录音/摄像头——接收端仅需录音用于音频流）
        wv.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                runOnUiThread { request.grant(request.resources) }
            }
        }

        // 接受自签名证书（仅局域网内使用，安全可控）
        wv.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.proceed()
            }
        }
    }

    private fun loadReceiver(ip: String) {
        webView.loadUrl("https://$ip:8765/receiver")
    }

    private fun showIpDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(80, 48, 80, 24)
        }
        val input = EditText(this).apply {
            hint = "192.168.1.x"
            textSize = 22f
            // 预填上次保存的 IP（如果有）
            prefs.getString("server_ip", "")?.let { if (it.isNotBlank()) setText(it) }
        }
        layout.addView(input)

        AlertDialog.Builder(this)
            .setTitle("输入服务器 IP")
            .setMessage("运行 node server.js 的电脑 IP 地址（端口固定 8765）")
            .setView(layout)
            .setPositiveButton("确认") { _, _ ->
                val ip = input.text.toString().trim()
                if (ip.isNotBlank()) {
                    prefs.edit().putString("server_ip", ip).apply()
                    loadReceiver(ip)
                }
            }
            .setCancelable(false)
            .show()
    }

    // 按遥控器返回键 → 重新设置 IP
    @Deprecated("Required override")
    override fun onBackPressed() {
        showIpDialog()
    }

    private fun enterFullscreen() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }
}
