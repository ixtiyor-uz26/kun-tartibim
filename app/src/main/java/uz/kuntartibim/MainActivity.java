package uz.kuntartibim;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private WebView web;
    private boolean pageReady = false;
    private String pendingKin = null;
    private String pendingTel = null;
    private static final int REQ_CALL = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.setStatusBarColor(Color.parseColor("#0d6b56"));

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setTextZoom(100);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.addJavascriptInterface(new Bridge(this), "AndroidBridge");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String sc = u.getScheme();
                if ("file".equals(sc)) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                flushKin();
            }
        });
        setContentView(web);
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/index.html");

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        Scheduler.ensureChannel(this);
        handleCall(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleCall(intent);
    }

    /** Bildirishnomadagi "Qo'ng'iroq" bosilganda */
    private void handleCall(Intent i) {
        if (i == null) return;
        String kin = i.getStringExtra("call_kin");
        if (kin == null || kin.isEmpty()) return;
        String tel = i.getStringExtra("call_tel");
        int nid = i.getIntExtra("notif_id", 0);
        i.removeExtra("call_kin");
        i.removeExtra("call_tel");
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null && nid != 0) nm.cancel(nid);
        pendingKin = kin;
        flushKin();
        if (tel != null && !tel.isEmpty()) call(tel);
    }

    private void flushKin() {
        if (!pageReady || web == null || pendingKin == null) return;
        String k = pendingKin.replaceAll("[^A-Za-z0-9_-]", "");
        pendingKin = null;
        web.evaluateJavascript("window.kinCalled&&kinCalled('" + k + "')", null);
    }

    /** Ruxsat bo'lsa darhol qo'ng'iroq, bo'lmasa raqam terilgan oyna ochiladi */
    void call(String tel) {
        if (tel == null) return;
        String num = tel.replaceAll("[^0-9+]", "");
        if (num.isEmpty()) return;
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            try {
                startActivity(new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", num, null)));
                return;
            } catch (Exception ignored) { }
        } else if (!getSharedPreferences("kun", MODE_PRIVATE).getBoolean("callAsked", false)) {
            getSharedPreferences("kun", MODE_PRIVATE).edit().putBoolean("callAsked", true).apply();
            pendingTel = num;
            requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQ_CALL);
            return;
        }
        dialOnly(num);
    }

    private void dialOnly(String num) {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", num, null)));
        } catch (Exception ignored) { }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code != REQ_CALL || pendingTel == null) return;
        String num = pendingTel;
        pendingTel = null;
        if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) call(num);
        else dialOnly(num);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.evaluateJavascript("window.pushSchedule&&pushSchedule()", null);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}
