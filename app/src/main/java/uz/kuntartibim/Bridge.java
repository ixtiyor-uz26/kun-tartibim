package uz.kuntartibim;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class Bridge {
    private final Context ctx;
    private final MainActivity act;

    Bridge(MainActivity act) { this.act = act; this.ctx = act.getApplicationContext(); }

    /** JSON: [{"at":millis,"title":"...","text":"...","kin":"id","tel":"+998..."}] */
    @JavascriptInterface
    public void schedule(String json) {
        Scheduler.save(ctx, json);
        Scheduler.scheduleAll(ctx);
    }

    @JavascriptInterface
    public void dial(final String tel) {
        act.runOnUiThread(() -> act.call(tel));
    }

    @JavascriptInterface
    public boolean isApp() { return true; }
}
