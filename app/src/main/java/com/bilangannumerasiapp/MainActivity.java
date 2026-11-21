package com.bilangannumerasiapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private static final String BLOGGER_GAME_URL = "https://operasihitung17.blogspot.com/?m=1";

    private WebView webView;
    private FrameLayout nativeAdContainer;
    private AdManager adManager;

    private boolean isInGameSession = false;
    private boolean hasPageError = false;
    private boolean hasJsSignaledReady = false;
    private final Handler fallbackHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        enableImmersiveFullscreen();

        webView = findViewById(R.id.game_webview);
        nativeAdContainer = findViewById(R.id.native_ad_container);

        adManager = new AdManager(this, webView, nativeAdContainer);

        setupWebView();
        setupBackNavigation();

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(BLOGGER_GAME_URL);
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setBackgroundColor(Color.parseColor("#F0F9FF"));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Jembatan JavaScript untuk komunikasi Blogger/Apps Script <-> Android AdMob
        WebAppAdMobInterface jsBridge = new WebAppAdMobInterface();
        webView.addJavascriptInterface(jsBridge, "AndroidAdMob");
        webView.addJavascriptInterface(jsBridge, "AndroidApp");

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (url != null && url.startsWith("http")) {
                    hasPageError = false;
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String host = uri.getHost();
                if (host != null && (host.contains("operasihitung17.blogspot.com")
                        || host.contains("blogspot.com")
                        || host.contains("script.google.com")
                        || host.contains("googleusercontent.com"))) {
                    return false;
                }
                // Buka tautan eksternal (misal kebijakan privasi) di browser eksternal
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!hasPageError && url != null && url.startsWith("http")) {
                    // Fallback aman jika skrip web belum memanggil onGameContentReady() dalam 1.5 detik
                    fallbackHandler.postDelayed(() -> {
                        if (!hasPageError && !hasJsSignaledReady) {
                            adManager.onContentReady();
                        }
                    }, 1500L);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    hasPageError = true;
                    hasJsSignaledReady = false;
                    adManager.setOfflineState(true);
                    showOfflineScreen();
                }
            }
        });
    }

    /**
     * Menampilkan layar offline bersih memakai aset no_internet.png (tanpa iklan).
     */
    private void showOfflineScreen() {
        String offlineHtml = "<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<style>body{margin:0;height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;"
                + "background:#F0F9FF;font-family:sans-serif;text-align:center;padding:24px;color:#0F172A;}"
                + "img{max-width:220px;height:auto;margin-bottom:16px;}"
                + "h2{margin:0 0 8px;font-size:20px;}p{margin:0 0 20px;color:#475569;font-size:14px;}"
                + "button{background:#0284C7;color:#fff;border:none;padding:12px 24px;border-radius:10px;font-size:15px;font-weight:bold;}</style>"
                + "</head><body>"
                + "<img src='file:///android_asset/no_internet.png' alt='Tidak Ada Koneksi'/>"
                + "<h2>Koneksi Internet Terputus</h2>"
                + "<p>Periksa jaringan Wi-Fi atau data seluler Anda untuk memuat permainan.</p>"
                + "<button onclick=\"AndroidApp.reloadGame()\">Coba Lagi</button>"
                + "</body></html>";
        webView.loadDataWithBaseURL("file:///android_asset/", offlineHtml, "text/html", "UTF-8", null);
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isInGameSession && webView != null) {
                    webView.evaluateJavascript("if(typeof confirmQuitLevel==='function'){confirmQuitLevel();}", null);
                } else if (webView != null && webView.canGoBack() && !hasPageError) {
                    webView.goBack();
                } else {
                    finish();
                }
            }
        });
    }

    private void enableImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveFullscreen();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) {
            webView.saveState(outState);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        fallbackHandler.removeCallbacksAndMessages(null);
        if (adManager != null) adManager.onDestroy();
        if (webView != null) {
            webView.removeAllViews();
            webView.destroy();
        }
        super.onDestroy();
    }

    /**
     * Jembatan metode JavaScript: Fokus pada App Open, Native, dan Interstitial.
     */
    private class WebAppAdMobInterface {

        @JavascriptInterface
        public void reloadGame() {
            runOnUiThread(() -> {
                hasPageError = false;
                hasJsSignaledReady = false;
                webView.loadUrl(BLOGGER_GAME_URL);
            });
        }

        @JavascriptInterface
        public void configureFamiliesPolicy(boolean childDirected, String maxAdContentRating) {
            String rating = (maxAdContentRating != null && !maxAdContentRating.isEmpty())
                    ? maxAdContentRating : "G";
            adManager.applyFamiliesPolicy(childDirected, rating);
        }

        @JavascriptInterface
        public void onGameContentReady() {
            hasJsSignaledReady = true;
            adManager.onContentReady();
        }

        @JavascriptInterface
        public void onContentLoaded() {
            hasJsSignaledReady = true;
            adManager.onContentReady();
        }

        @JavascriptInterface
        public void setGameplayState(boolean active) {
            isInGameSession = active;
            adManager.setGameplayActive(active);
        }

        @JavascriptInterface
        public void setNativeAdVisible(boolean visible) {
            adManager.setNativeAdVisible(visible);
        }

        @JavascriptInterface
        public void showInterstitial(String placement) {
            adManager.showInterstitialAd(placement != null ? placement : "level_transition");
        }

        @JavascriptInterface
        public void panggilIklanInterstisial() {
            adManager.showInterstitialAd("level_transition");
        }

        @JavascriptInterface
        public void onNativeAdClicked() {
            // Ditangani otomatis oleh NativeAdView SDK
        }

        @JavascriptInterface
        public void onScoreUpdated(String jsonPayload) {
            // Hook opsional sinkronisasi skor
        }
    }
}
