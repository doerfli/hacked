package li.doerf.hacked.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.db.entities.Breach
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.utils.AccountHelper

class AccountDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val accountDao = AppDatabase.get(application).accountDao
    private val breachDao = AppDatabase.get(application).breachDao

    fun account(accountId: Long): LiveData<Account> = accountDao.findByIdLD(accountId)

    fun breaches(accountId: Long): LiveData<List<Breach>> = breachDao.findByAccountLD(accountId)

    fun acknowledge(breach: Breach, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            breach.acknowledged = true
            breachDao.update(breach)
            Analytics.trackCustomEvent(CustomEvent.BREACH_ACKNOWLEDGED)
            updateAccountIsHacked(breach.account)
            withContext(Dispatchers.Main) { onDone() }
        }
    }

    private fun updateAccountIsHacked(accountId: Long) {
        for (account in accountDao.findById(accountId)) {
            if (!account.hacked) continue
            AccountHelper(getApplication()).updateBreachCounts(account)
            if (breachDao.countUnacknowledged(accountId) == 0L) {
                account.hacked = false
            }
            accountDao.update(account)
        }
    }

    fun resetAcknowledged(accountId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            for (breach in breachDao.findByAccount(accountId)) {
                breach.acknowledged = false
                breachDao.update(breach)
            }
        }
    }

    fun deleteAccount(accountId: Long, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val account = accountDao.findById(accountId).firstOrNull()
            val breaches = breachDao.findByAccount(accountId)
            try {
                for (breach in breaches) breachDao.delete(breach)
                account?.let { accountDao.delete(it) }
            } finally {
                Analytics.trackCustomEvent(CustomEvent.ACCOUNT_DELETED)
            }
            withContext(Dispatchers.Main) { onDone() }
        }
    }
}
