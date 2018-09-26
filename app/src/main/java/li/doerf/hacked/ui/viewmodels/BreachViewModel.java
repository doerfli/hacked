package li.doerf.hacked.ui.viewmodels;

import android.app.Application;
import android.util.LongSparseArray;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.BreachDao;
import li.doerf.hacked.db.entities.Breach;

public class BreachViewModel extends AndroidViewModel {

    private final BreachDao myBreachDao;
    private LongSparseArray<LiveData<List<Breach>>> breachListMap = new LongSparseArray<>();

    public BreachViewModel(@NonNull Application application) {
        super(application);
        myBreachDao = AppDatabase.get(getApplication()).getBreachDao();
    }

    public LiveData<List<Breach>> getBreachList(Long accountId) {
        if (breachListMap.indexOfKey(accountId) < 0) {
            breachListMap.put(accountId, myBreachDao.findByAccountLD(accountId));
        }

        return breachListMap.get(accountId);
    }

}
