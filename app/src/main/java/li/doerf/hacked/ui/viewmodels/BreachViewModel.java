package li.doerf.hacked.ui.viewmodels;

import android.app.Application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.BreachDao;
import li.doerf.hacked.db.entities.Breach;

public class BreachViewModel extends AndroidViewModel {

    private final BreachDao myBreachDao;
    private Map<Long, LiveData<List<Breach>>> breachListMap = new HashMap<>();

    public BreachViewModel(@NonNull Application application) {
        super(application);
        myBreachDao = AppDatabase.get(getApplication()).getBreachDao();
    }

    public LiveData<List<Breach>> getBreachList(Long accountId) {
        if (!breachListMap.containsKey(accountId)) {
            breachListMap.put(accountId, myBreachDao.findByAccountLD(accountId));
        }

        return breachListMap.get(accountId);
    }

}
