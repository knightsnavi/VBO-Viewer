package com.knightsnavi.vboviewer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.webkit.WebViewAssetLoader;

public class MainActivity extends Activity {

    private static final int FILE_REQUEST = 1;

    // Assets are served over https from this reserved host rather than from file://,
    // giving the page a real secure origin: file access can then be switched off,
    // and the page's Content-Security-Policy 'self' has an origin to match.
    private static final String APP_HOST = WebViewAssetLoader.DEFAULT_DOMAIN;
    private static final String START_URL = "https://" + APP_HOST + "/assets/index.html";

    private WebView web;
    private ValueCallback<Uri[]> pendingFiles;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // Raw insets have to reach the content view; otherwise the decor reports them
        // as already consumed and the listener below sees zeroes.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        web = new WebView(this);
        web.setBackgroundColor(0xFF0F1115);

        // The WebView draws into its own surface and does not reliably honour its own
        // padding, so the insets are applied to a container around it instead.
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF0F1115);
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // Android 15 onwards draws apps edge to edge, putting the page under the status
        // bar and the navigation bar. Inset by the bars and cutout instead; this
        // re-applies on fold and unfold, where the geometry differs.
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);          // the app stores unit and colour preferences
        s.setAllowFileAccess(false);           // assets come through the loader, not file://
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);

        WebViewAssetLoader assets = new WebViewAssetLoader.Builder()
                .setDomain(APP_HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assets.shouldInterceptRequest(request.getUrl());
            }

            // The app is a single page. Anything that would navigate away, such as the
            // map's attribution links, opens in the browser rather than turning this
            // JavaScript-enabled WebView into a browser for arbitrary sites.
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (APP_HOST.equals(uri.getHost())) {
                    return false;
                }
                String scheme = uri.getScheme();
                if ("https".equals(scheme) || "http".equals(scheme)) {
                    Intent open = new Intent(Intent.ACTION_VIEW, uri);
                    open.addCategory(Intent.CATEGORY_BROWSABLE);
                    try {
                        startActivity(open);
                    } catch (ActivityNotFoundException ignored) {
                        // no browser installed; the link simply does nothing
                    }
                }
                return true;
            }

            // A large video can exhaust the renderer's memory. Without this, losing the
            // renderer takes the whole app down; recreate the activity instead.
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                ((ViewGroup) view.getParent()).removeView(view);
                view.destroy();
                web = null;                    // already destroyed; onDestroy must not repeat it
                recreate();
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (pendingFiles != null) {
                    pendingFiles.onReceiveValue(null);
                }
                pendingFiles = callback;

                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                // .vbo has no registered MIME type, so a narrower filter hides the very
                // files this app exists to open.
                pick.setType("*/*");
                if (params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE) {
                    pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }

                try {
                    startActivityForResult(Intent.createChooser(pick, "Select file"), FILE_REQUEST);
                } catch (Exception e) {
                    pendingFiles = null;
                    return false;
                }
                return true;
            }
        });

        web.loadUrl(START_URL);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request != FILE_REQUEST) {
            super.onActivityResult(request, result, data);
            return;
        }
        if (pendingFiles == null) {
            return;
        }
        pendingFiles.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
        pendingFiles = null;
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.destroy();
        }
        super.onDestroy();
    }
}
