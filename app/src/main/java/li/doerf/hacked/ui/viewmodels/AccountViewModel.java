package li.doerf.hacked.ui.viewmodels;

import android.app.Application;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.entities.Account;

public class AccountViewModel extends AndroidViewModel {

    private final LiveData<List<Account>> accountList;

    public AccountViewModel(@NonNull Application application) {
        super(application);
        accountList = AppDatabase.get(application).getAccountDao().getAllLD();
    }

    public LiveData<List<Account>> getAccountList() {
        return accountList;
    }
}
