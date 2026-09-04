package li.doerf.hacked.ui.viewmodels

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.BreachedSiteDao
import li.doerf.hacked.db.entities.BreachedSite
import li.doerf.hacked.remote.hibp.BreachedSitesWorker

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

    fun reloadIfStale(prefs: SharedPreferences, context: Context) {
        val lastSync = prefs.getLong(PREF_KEY_LAST_BREACHED_SITES_SYNC, 0)
        if (System.currentTimeMillis() - lastSync <= SIX_HOURS_MILLIS) {
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()
        val checker = OneTimeWorkRequest.Builder(BreachedSitesWorker::class.java)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueue(checker)
        prefs.edit { putLong(PREF_KEY_LAST_BREACHED_SITES_SYNC, System.currentTimeMillis()) }
    }

    init {
        myBreachedSites = filterLiveData.switchMap { (o: Order, filter: String) ->
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

    companion object {
        private const val PREF_KEY_LAST_BREACHED_SITES_SYNC = "PREF_KEY_LAST_BREACHED_SITES_SYNC"
        private const val SIX_HOURS_MILLIS = 6 * 60 * 60 * 1000L
    }
}