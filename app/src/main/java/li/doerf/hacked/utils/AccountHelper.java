package li.doerf.hacked.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import li.doerf.hacked.R;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.entities.Account;

public class AccountHelper {

    private static final String LOGTAG = AccountHelper.class.getSimpleName();

    public static Account createAccount(Context context, String name) {
        AccountDao accountDao = AppDatabase.get(context).getAccountDao();

        if (accountDao.coundByName(name) > 0) {
            Toast.makeText(context, context.getString(R.string.toast_account_exists), Toast.LENGTH_LONG).show();
            Log.w(LOGTAG, "account already exists");
            return null;
        }

        Account account = new Account();
        account.setName(name);
        List<Long> ids = accountDao.insert(account);
        Long id = ids.get(0);

        account.setId(id);

        return account;
    }
}
