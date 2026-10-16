package uz.kuntartibim;

import android.content.Context;
import android.webkit.JavascriptInterface;

public class Bridge {
    private final Context ctx;

    Bridge(Context ctx) { this.ctx = ctx; }

    /** JSON: [{"at":millis,"title":"...","text":"..."}] */
    @JavascriptInterface
    public void schedule(String json) {
        Scheduler.save(ctx, json);
        Scheduler.scheduleAll(ctx);
    }

    @JavascriptInterface
    public boolean isApp() { return true; }
}
