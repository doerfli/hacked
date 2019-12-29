package li.doerf.hacked.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import li.doerf.hacked.R


class NavActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)
    }

//    override fun onBackPressed() {
//        Log.d("NavActivity", "c: " + supportFragmentManager.backStackEntryCount)
//        supportFragmentManager.popBackStack()
////        super.onBackPressed()
//    }

//    override fun onSupportNavigateUp(): Boolean {
//        return (Navigation.findNavController(this, R.id.overviewFragment).navigateUp()
//                || super.onSupportNavigateUp())
//    }

}
