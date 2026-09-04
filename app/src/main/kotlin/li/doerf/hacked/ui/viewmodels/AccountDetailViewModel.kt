package li.doerf.hacked.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.db.entities.Breach
import li.doerf.hacked.ui.Routes
import li.doerf.hacked.util.Analytics
import li.doerf.hacked.utils.AccountHelper

class AccountDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val accountDao = AppDatabase.get(application).accountDao
    private val breachDao = AppDatabase.get(application).breachDao
    private val accountId: Long = checkNotNull(savedStateHandle[Routes.ACCOUNT_DETAIL_ARG])

    val account: StateFlow<Account?> = accountDao.findByIdFlow(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val breaches: StateFlow<List<Breach>> = breachDao.findByAccountFlow(accountId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun resetAcknowledged() {
        viewModelScope.launch(Dispatchers.IO) {
            for (breach in breachDao.findByAccount(accountId)) {
                breach.acknowledged = false
                breachDao.update(breach)
            }
        }
    }

    fun deleteAccount(onDone: () -> Unit) {
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
