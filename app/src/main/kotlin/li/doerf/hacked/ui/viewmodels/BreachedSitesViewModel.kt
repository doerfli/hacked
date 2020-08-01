package li.doerf.hacked.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.BreachedSiteDao
import li.doerf.hacked.db.entities.BreachedSite

class BreachedSitesViewModel(application: Application) : AndroidViewModel(application) {
    private val myBreachedSitesDao: BreachedSiteDao = AppDatabase.get(application).brachedSiteDao
    private var myBreachedSites: LiveData<List<BreachedSite>>?
    private var myBreachedSitesMostRecent: LiveData<List<BreachedSite>>? = null
    private val filterLiveData: MutableLiveData<Pair<Order, String>> = MutableLiveData(Pair(Order.NAME, ""))
    val breachesSites: LiveData<List<BreachedSite>>?
        get() {
            if (myBreachedSites == null) {
                myBreachedSites = myBreachedSitesDao.allLD
            }
            return myBreachedSites
        }

    fun setFilter(filter: String) {
        filterLiveData.value = Pair(Order.NAME, filter)
    }

    val breachesSitesMostRecent: LiveData<List<BreachedSite>>?
        get() {
            if (myBreachedSitesMostRecent == null) {
                myBreachedSitesMostRecent = myBreachedSitesDao.listMostRecent()
            }
            return myBreachedSitesMostRecent
        }

    fun orderByName() {
        filterLiveData.value = Pair(Order.NAME, "")
    }

    fun orderByCount() {
        filterLiveData.value = Pair(Order.COUNT, "")
    }
    fun orderByDate() {
        filterLiveData.value = Pair(Order.DATE, "")
    }

    private enum class Order {
        NAME, COUNT, DATE
    }

    init {
        myBreachedSites = Transformations.switchMap(filterLiveData
        ) { (o: Order, filter: String) ->
            when(o) {
                Order.NAME -> {
                    if (filter.trim { it <= ' ' } == "") {
                        return@switchMap myBreachedSitesDao.allLD
                    } else {
                        return@switchMap myBreachedSitesDao.getAllByName("%$filter%")
                    }
                }
                Order.COUNT -> {
                    return@switchMap myBreachedSitesDao.allByPwnCountLD
                }
                Order.DATE -> {
                    return@switchMap myBreachedSitesDao.allByDateAddedLD
                }
            }

        }
    }
}