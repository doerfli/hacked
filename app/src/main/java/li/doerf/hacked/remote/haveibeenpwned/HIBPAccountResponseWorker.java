package li.doerf.hacked.remote.haveibeenpwned;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Arrays;
import java.util.List;

public class HIBPAccountResponseWorker extends Worker {

    private static final String TAG = "HIBPAccountResponseWork";
    public static final String KEY_BREACHES = "Breaches";

    public HIBPAccountResponseWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        List<String> breaches = Arrays.asList(getInputData().getStringArray(KEY_BREACHES));

        for (String breachName : breaches) {
            Log.d(TAG, breachName);
        }

        // TODO v3 check if breach already exists
        // TODO v3 if not add breach

        return Result.success();
    }
}
