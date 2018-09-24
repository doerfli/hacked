package li.doerf.hacked.remote.haveibeenpwned;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.common.collect.Lists;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import li.doerf.hacked.R;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.daos.BreachDao;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.db.entities.Breach;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static li.doerf.hacked.R.id.account;

/**
 * Created by moo on 26.03.17.
 */

public class HIBPAccountChecker {
    private final String LOGTAG = getClass().getSimpleName();
    private static long noReqBefore = 0;

    private final Context myContext;
    private final IProgressUpdater myProgressUpdater;
    private final AccountDao myAccountDao;
    private final BreachDao myBreachDao;
    // TODO what about this?
    private boolean abort = false;

    public HIBPAccountChecker(Context aContext, IProgressUpdater aProgressUpdates) {
        myContext = aContext;
        myProgressUpdater = aProgressUpdates;
        myAccountDao = AppDatabase.get(aContext).getAccountDao();
        myBreachDao = AppDatabase.get(aContext).getBreachDao();
    }

    public Boolean check(Long id) {
        Log.d(LOGTAG, "starting check for breaches");
        boolean newBreachFound = false;
        abort = false;

        List<Account> accountsToCheck = new ArrayList<>();

        if ( id == null ) {
            accountsToCheck.addAll(myAccountDao.getAll());
        } else {
            Log.d(LOGTAG, "only id " + id);
            accountsToCheck.add(myAccountDao.findById(id));
        }

        for (Account account : accountsToCheck) {
            Log.d(LOGTAG, "Checking for account: " + account.getName());

            try {
                List<BreachedAccount> breachedAccounts = doCheck(account.getName());
                newBreachFound |= processBreachedAccounts( account, breachedAccounts);
            } finally {
                myProgressUpdater.updateProgress( account);
            }
        }

        Log.d(LOGTAG, "finished checking for breaches");

        return newBreachFound;
    }

    private boolean processBreachedAccounts(Account account, List<BreachedAccount> breachedAccounts) {
        boolean isNewBreachFound = false;

        for (BreachedAccount ba : breachedAccounts) {
            Breach existing = myBreachDao.findByAccountAndName(account.getId(), ba.getName());

            if (existing != null) {
                Log.d(LOGTAG, "breach already existing: " + ba.getName());
                continue;
            }

            Log.d(LOGTAG, "new breach: " + ba.getName());
            Breach newBreach = new Breach();
            newBreach.setAccount(account.getId());
            newBreach.setName(ba.getName());
            newBreach.setTitle(ba.getTitle());
            newBreach.setDomain(ba.getDomain());
            newBreach.setBreachDate(DateTime.parse(ba.getBreachDate()).getMillis());
            newBreach.setAddedDate(DateTime.parse(ba.getAddedDate()).getMillis());
            newBreach.setPwnCount(ba.getPwnCount());
            newBreach.setDescription(ba.getDescription());
            newBreach.setDataClasses(ba.getAddedDate());
            newBreach.setVerified(ba.getIsVerified());
            newBreach.setAcknowledged(false);
            // TODO process in other thread?
            myBreachDao.insert(newBreach);
            Log.i(LOGTAG, "breach inserted into db");
            isNewBreachFound |= true;
        }

        account.setLastChecked(DateTime.now());
        if (isNewBreachFound && ! account.getHacked() ) {
            account.setHacked(true);
        }
        myAccountDao.update(account);

        return isNewBreachFound;
    }
        
    private List<BreachedAccount> doCheck(String anAccount) {
        try {
            Response<List<BreachedAccount>> response = retrieveBreaches(anAccount);

            if (response.isSuccessful()) {
                List<BreachedAccount> breachedAccounts = response.body();
                return breachedAccounts;
            } else {
                if (response.code() == 404) {
                    Log.i(LOGTAG, "no breach found: " + account);
                    return Lists.newArrayList();
                } else {
                    Log.w(LOGTAG, "unexpected response code: " + response.code());
                }
            }

        } catch (IOException e) {
            Log.e(LOGTAG, "caughtIOException while contacting www.haveibeenpwned.com - " + e.getMessage(), e);
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(myContext, myContext.getString(R.string.toast_error_error_during_check), Toast.LENGTH_LONG).show();
                }
            });
        }
        return Lists.newArrayList();
    }

    @NonNull
    private synchronized Response<List<BreachedAccount>> retrieveBreaches(String account) throws IOException {
        Log.d(LOGTAG, "retrieving breaches: " + account);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://haveibeenpwned.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        HaveIBeenPwned service = retrofit.create(HaveIBeenPwned.class);
        Call<List<BreachedAccount>> breachedAccountsList = service.listBreachedAccounts(account);
        long timeDelta = noReqBefore - System.currentTimeMillis();

        if (timeDelta > 0) {
            try {
                Log.d(LOGTAG, "waiting for " + timeDelta + "ms before next request");
                Thread.sleep(timeDelta);
            } catch (InterruptedException e) {
                Log.e(LOGTAG, "caught InterruptedException while waiting for next request slot", e);
            }
        }

        Response<List<BreachedAccount>> response = breachedAccountsList.execute();

        // check next request timeout
        String retryAfter = response.headers().get("Retry-After");
        Random random = new Random();
        if (retryAfter != null) {
            noReqBefore = System.currentTimeMillis() + (Integer.parseInt(retryAfter) * 1000) + random.nextInt(100);
        } else {
            noReqBefore = System.currentTimeMillis() + (1500 + random.nextInt(100));
        }

        return response;
    }

    public void abort() {
        abort = true;
    }
}
