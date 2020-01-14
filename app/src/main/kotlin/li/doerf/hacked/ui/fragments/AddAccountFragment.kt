package li.doerf.hacked.ui.fragments


import android.app.Application
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.work.*
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import li.doerf.hacked.CustomEvent
import li.doerf.hacked.HackedApplication
import li.doerf.hacked.R
import li.doerf.hacked.db.AppDatabase
import li.doerf.hacked.db.daos.AccountDao
import li.doerf.hacked.db.entities.Account
import li.doerf.hacked.remote.haveibeenpwned.HIBPAccountCheckerWorker

/**
 * A simple [Fragment] subclass.
 */
class AddAccountFragment : Fragment() {

    companion object {
        const val LOGTAG = "AddAccountFragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_add_account, container, false)

        val account = view.findViewById<EditText>(R.id.account)
        
        val addButton = view.findViewById<Button>(R.id.add)
        addButton.setOnClickListener {
            val accountName = account.text
            addAccount(accountName.toString())
            findNavController().popBackStack()
        }

        val cancelButton = view.findViewById<Button>(R.id.cancel)
        cancelButton.setOnClickListener {
            findNavController().popBackStack()
        }
        
        return view
    }

    private fun addAccount(aName: String) {
        if (aName == null || aName.trim { it <= ' ' } == "") {
            Toast.makeText(context, getString(R.string.toast_enter_valid_name), Toast.LENGTH_LONG).show()
            Log.w(LOGTAG, "account name not valid")
            return
        }
        val name = aName.trim { it <= ' ' }
        val accountDao = AppDatabase.get(context).accountDao
        getAccountCount(name, accountDao)
                .subscribe(Consumer { count: Int ->
                    if (count == 0) {
                        val account = Account()
                        account.name = name
                        account.numBreaches = 0
                        account.numAcknowledgedBreaches = 0
                        insertAccount(accountDao, account, activity!!.application)
                    }

                })
    }

    private fun getAccountCount(name: String, accountDao: AccountDao): Single<Int> {
        return Single.fromCallable { accountDao.countByName(name) }
                .subscribeOn(Schedulers.io())
    }

    private fun insertAccount(accountDao: AccountDao, account: Account, application: Application) {
        Single.fromCallable { accountDao.insert(account) }
                .subscribeOn(Schedulers.io())
                .subscribe({ ids: List<Long> ->
                    (application as HackedApplication).trackCustomEvent(CustomEvent.ACCOUNT_ADDED)
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
                    WorkManager.getInstance().enqueue(checker)
                }) { throwable: Throwable? -> Log.e(ContentValues.TAG, "Error msg", throwable) }
    }
}
