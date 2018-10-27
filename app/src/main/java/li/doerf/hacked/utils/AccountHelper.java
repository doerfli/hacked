package li.doerf.hacked.utils;

import android.content.Context;

import com.google.common.collect.Collections2;

import java.util.List;

import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.BreachDao;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.db.entities.Breach;

public class AccountHelper {

    private final BreachDao myBreachDao;

    public AccountHelper(Context context) {
        myBreachDao = AppDatabase.get(context).getBreachDao();
    }

    public void updateBreachCounts(Account account) {
        List<Breach> breaches = myBreachDao.findByAccount(account.getId());
        account.setNumBreaches(breaches.size());
        account.setNumAcknowledgedBreaches(Collections2.filter(breaches, Breach::getAcknowledged).size());
    }

}
