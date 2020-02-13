package li.doerf.hacked.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.entities.Account;

public class AccountViewModel extends AndroidViewModel {

    private final LiveData<List<Account>> accountList;
    private final LiveData<Account> lastChecked;

    public AccountViewModel(@NonNull Application application) {
        super(application);
        accountList = AppDatabase.get(application).getAccountDao().getAllLD();
        lastChecked = AppDatabase.get(application).getAccountDao().getLastChecked();
    }

    public LiveData<List<Account>> getAccountList() {
        return accountList;
    }

    public LiveData<Account> getLastChecked() {
        return lastChecked;
    }
}
