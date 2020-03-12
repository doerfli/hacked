package li.doerf.hacked.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;

import li.doerf.hacked.db.AppDatabase;
import li.doerf.hacked.db.daos.BreachedSiteDao;
import li.doerf.hacked.db.entities.BreachedSite;

public class BreachedSitesViewModel extends AndroidViewModel {

    private final BreachedSiteDao myBreachedSitesDao;
    private LiveData<List<BreachedSite>> myBreachedSites;
    private LiveData<List<BreachedSite>> myBreachedSitesTop20;
    private LiveData<List<BreachedSite>> myBreachedSitesMostRecent;
    private MutableLiveData<String> filterLiveData = new MutableLiveData<>("");

    public BreachedSitesViewModel(@NonNull Application application) {
        super(application);
        myBreachedSitesDao = AppDatabase.get(application).getBrachedSiteDao();
        myBreachedSites = Transformations.switchMap(filterLiveData,
                v -> {
                    if (v == null || v.trim().equals("")) {
                        return myBreachedSitesDao.getAllLD();
                    } else {
                        return myBreachedSitesDao.getAllByName("%" + v + "%");
                    }
                });
    }

    public LiveData<List<BreachedSite>> getBreachesSites() {
        if (myBreachedSites == null) {
            myBreachedSites = myBreachedSitesDao.getAllLD();
        }
        return myBreachedSites;
    }

    public void setFilter(String filter) {
        filterLiveData.setValue(filter);
    }

    public LiveData<List<BreachedSite>> getBreachesSitesTop20() {
        if (myBreachedSitesTop20 == null) {
            myBreachedSitesTop20 = myBreachedSitesDao.listTop20();
        }
        return myBreachedSitesTop20;
    }

    public LiveData<List<BreachedSite>> getBreachesSitesMostRecent() {
        if (myBreachedSitesMostRecent == null) {
            myBreachedSitesMostRecent = myBreachedSitesDao.listMostRecent();
        }
        return myBreachedSitesMostRecent;
    }

}
