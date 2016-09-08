package li.doerf.hacked.services;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

/**
 * Created by moo on 08/09/16.
 */
public class ScheduledCheckService extends IntentService {
    private final String LOGTAG = getClass().getSimpleName();


    public ScheduledCheckService() {
        super("ScheduledCheckService");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ScheduledCheckService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(LOGTAG, "onHandleIntent");
    }
}
