package li.doerf.hacked.utils;

import android.annotation.SuppressLint;

import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class BackgroundTaskHelper<T> {

    /**
     * Run callable on background task and then the consumer in the main thread.
     *
     * @param backgroundCallable the callable to run in background thread
     * @param uiThreadConsumer the consumer to run on main thread again
     */
    @SuppressLint("CheckResult")
    public void runInBackgroundAndConsumeOnMain(Callable<T> backgroundCallable, Consumer<T> uiThreadConsumer) {
        //noinspection ResultOfMethodCallIgnored
        Single.fromCallable(backgroundCallable)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uiThreadConsumer);
    }

}
