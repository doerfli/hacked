package li.doerf.hacked.utils;

/**
 * Created by moo on 12/09/16.
 */
public interface IServiceRunningListener {

    void notifyListener(Event anEvent);

    enum Event {
        STARTED,
        STOPPED
    }
}
