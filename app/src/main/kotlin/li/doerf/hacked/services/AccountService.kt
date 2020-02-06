package li.doerf.hacked.services

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.AccountDao
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountCheckerWorker
import li.doerf.hacked.ui.fragments.AccountsFragment
import li.doerf.hacked.util.createCoroutingExceptionHandler

class AccountService(private val application: Application) {

    private var context: Context = application.applicationContext

    fun addAccount(aName: String) {
        if ( aName.trim { it <= ' ' } == "") {
            Toast.makeText(context, context.getString(R.string.toast_enter_valid_name), Toast.LENGTH_LONG).show()
            Log.w(AccountsFragment.LOGTAG, "account name not valid")
            return
        }
        val name = aName.trim { it <= ' ' }

        runBlocking(context = Dispatchers.IO) {
            launch(createCoroutingExceptionHandler(AccountsFragment.LOGTAG)) {
                addNewAccount(name)
            }
        }
    }

    private fun addNewAccount(name: String) {
        val accountDao = AppDatabase.get(context).accountDao
        val count = accountDao.countByName(name)
        if (count > 0) {
            return
        }
        insertAccount(accountDao, createNewAccount(name), application)
    }

    private fun createNewAccount(name: String): Account {
        val account = Account()
        account.name = name
        account.numBreaches = 0
        account.numAcknowledgedBreaches = 0
        return account
    }

    private fun insertAccount(accountDao: AccountDao, account: Account, application: Application) {
        val ids = accountDao.insert(account)
        (application as HackedApplication).trackCustomEvent(CustomEvent.ACCOUNT_ADDED)
        checkNewAccount(ids)
    }

    private fun checkNewAccount(ids: MutableList<Long>) {
        val inputData = Data.Builder()
                .putLong(HIBPAccountCheckerWorker.KEY_ID, ids[0])
                .build()
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .build()
        val checker = OneTimeWorkRequest.Builder(HIBPAccountCheckerWorker::class.java)
                .setInputData(inputData)
                .setConstraints(constraints)
                .build()
        WorkManager.getInstance(context!!).enqueue(checker)
    }

}