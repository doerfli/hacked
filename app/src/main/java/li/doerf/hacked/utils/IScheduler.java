package li.doerf.hacked.utils;

/**
 * Created by moo on 26.03.17.
 */

public interface IScheduler {
    void scheduleCheckService(long interval);

    void cancelCheckService();
}
