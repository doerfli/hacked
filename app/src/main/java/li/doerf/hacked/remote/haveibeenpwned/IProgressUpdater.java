package li.doerf.hacked.remote.haveibeenpwned;

import li.doerf.hacked.db.entities.Account;

/**
 * Created by moo on 26.03.17.
 */

public interface IProgressUpdater {

    void updateProgress(Account account);

}
