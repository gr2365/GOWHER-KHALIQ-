package tech.gowherkhaliqspace.app;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Iterator;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String HOME_URL = "https://www.gowherkhaliqspace.tech/";
    private static final String SITE_HOST = "www.gowherkhaliqspace.tech";
    private static final int WEB_PERMISSION_REQUEST = 41;

    private WebView webView;
    private final ArrayDeque<PendingWebPermission> permissionQueue = new ArrayDeque<>();
    private PendingWebPermission activePermission;

    private static final class PendingWebPermission {
        final PermissionRequest request;
        final String origin;
        final GeolocationPermissions.Callback geoCallback;
        final Set<String> webResources;
        final String[] androidPermissions;

        PendingWebPermission(PermissionRequest request, String origin,
                            GeolocationPermissions.Callback geoCallback,
                            Set<String> webResources, String[] androidPermissions) {
            this.request = request;
            this.origin = origin;
            this.geoCallback = geoCallback;
            this.webResources = webResources;
            this.androidPermissions = androidPermissions;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (isTrustedOrigin(uri.toString())) return false;
                openExternal(uri);
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> enqueueCameraPermission(request));
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                runOnUiThread(() -> removePermissionRequest(request));
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin,
                    GeolocationPermissions.Callback callback) {
                runOnUiThread(() -> enqueueLocationPermission(origin, callback));
            }
        });
        webView.loadUrl(HOME_URL);
    }

    private boolean isTrustedOrigin(String value) {
        Uri uri = Uri.parse(value);
        return "https".equalsIgnoreCase(uri.getScheme())
                && SITE_HOST.equalsIgnoreCase(uri.getHost());
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, "No app can open this link.", Toast.LENGTH_SHORT).show();
        }
    }

    private void enqueueCameraPermission(PermissionRequest request) {
        if (!isTrustedOrigin(request.getOrigin())) {
            request.deny();
            return;
        }
        Set<String> resources = new LinkedHashSet<>();
        List<String> needed = new ArrayList<>();
        for (String resource : request.getResources()) {
            if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                resources.add(resource);
                needed.add(Manifest.permission.CAMERA);
            }
        }
        if (resources.isEmpty()) {
            request.deny();
            return;
        }
        permissionQueue.add(new PendingWebPermission(request, request.getOrigin(), null,
                resources, needed.toArray(new String[0])));
        processPermissionQueue();
    }

    private void enqueueLocationPermission(String origin,
            GeolocationPermissions.Callback callback) {
        if (!isTrustedOrigin(origin)) {
            callback.invoke(origin, false, false);
            return;
        }
        permissionQueue.add(new PendingWebPermission(null, origin, callback,
                new LinkedHashSet<>(), new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }));
        processPermissionQueue();
    }

    private void processPermissionQueue() {
        if (activePermission != null || permissionQueue.isEmpty()) return;
        activePermission = permissionQueue.remove();
        List<String> missing = new ArrayList<>();
        for (String permission : activePermission.androidPermissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        if (missing.isEmpty()) {
            resolveActivePermission(true);
            return;
        }
        requestPermissions(missing.toArray(new String[0]), WEB_PERMISSION_REQUEST);
    }

    private void resolveActivePermission(boolean granted) {
        PendingWebPermission pending = activePermission;
        activePermission = null;
        if (pending != null) {
            if (pending.request != null) {
                if (granted) pending.request.grant(pending.webResources.toArray(new String[0]));
                else pending.request.deny();
            } else if (pending.geoCallback != null) {
                boolean locationGranted = granted
                        && (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);
                pending.geoCallback.invoke(pending.origin, locationGranted, false);
            }
        }
        processPermissionQueue();
    }

    private void removePermissionRequest(PermissionRequest request) {
        if (activePermission != null && activePermission.request == request) {
            activePermission = null;
            processPermissionQueue();
            return;
        }
        Iterator<PendingWebPermission> iterator = permissionQueue.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().request == request) iterator.remove();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != WEB_PERMISSION_REQUEST || activePermission == null) return;
        boolean granted;
        if (activePermission.request == null) {
            granted = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            granted = true;
            for (String permission : activePermission.androidPermissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
        }
        resolveActivePermission(granted);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
