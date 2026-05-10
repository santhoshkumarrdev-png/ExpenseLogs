package com.example.voiceexpense

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingWebPermissionRequest?.grant(pendingWebPermissionRequest?.resources)
                pendingWebPermissionRequest = null
                webView.reload()
            } else {
                Toast.makeText(
                    this,
                    "Microphone permission is required for voice logging.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private var pendingWebPermissionRequest: PermissionRequest? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = true

        webView = WebView(this).apply {
            settings.apply {
                javaScriptEnabled                = true
                domStorageEnabled                = true
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess                  = true
                setSupportZoom(false)
                builtInZoomControls              = false
                displayZoomControls              = false
                useWideViewPort                  = true
                loadWithOverviewMode             = true
                cacheMode                        = WebSettings.LOAD_DEFAULT
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    val host = request.url.host ?: return false
                    return if (host.contains("space-z.ai")) {
                        false
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url.toString().toUri()))
                        true
                    }
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    val micRequested =
                        request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)

                    if (!micRequested) { request.deny(); return }

                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        request.grant(request.resources)
                    } else {
                        pendingWebPermissionRequest = request
                        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                override fun onPermissionRequestCanceled(request: PermissionRequest) =
                    request.deny()
            }

            loadUrl("https://voicelogg.space-z.ai")
        }

        setContentView(webView)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    override fun onResume()  { super.onResume();  webView.onResume()  }
    override fun onPause()   { super.onPause();   webView.onPause()   }
    override fun onDestroy() { webView.destroy();  super.onDestroy()  }
}
