package com.mobeen.audioeditor

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        // Handles the file picker triggered by the app's "Attach" / "Upload" buttons.
        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data = result.data
            val results: Array<Uri>? = when {
                data == null || result.resultCode != RESULT_OK -> null
                data.clipData != null -> {
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                }
                data.data != null -> arrayOf(data.data!!)
                else -> null
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true          // needed for the Gemini API key stored in localStorage
            allowFileAccess = true
            mediaPlaybackRequiresUserGesture = false
        }

        webView.webChromeClient = object : WebChromeClient() {
            // Supports the app's file-upload inputs (Attach / Upload Audio-Video).
            override fun onShowFileChooser(
                webView: WebView,
                filePathCb: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                filePathCallback = filePathCb
                val intent = fileChooserParams.createIntent()
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*", "image/*"))
                fileChooserLauncher.launch(intent)
                return true
            }

            // Auto-grants mic/camera prompts if the page ever asks (harmless if unused).
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }

        // Keep navigation (if any) inside the WebView instead of opening an external browser.
        webView.webViewClient = android.webkit.WebViewClient()

        // IMPORTANT: this filename must match exactly what is placed in app/src/main/assets/
        webView.loadUrl("file:///android_asset/audio-editor.html")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}