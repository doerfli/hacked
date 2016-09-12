package li.doerf.hacked.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;

/**
 * Created by moo on 29/01/15.
 */
public class HackedSQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "hacked.db";
    private static final int DATABASE_VERSION = 2;
    private static HackedSQLiteHelper myInstance;
    private final String LOGTAG = getClass().getSimpleName();

    public static HackedSQLiteHelper getInstance(Context aContext) {
        if (myInstance == null) {
            myInstance = new HackedSQLiteHelper(aContext);
        }
        return myInstance;
    }

    private HackedSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(LOGTAG, "Initializing Database: " + DATABASE_NAME);
        new Account().createTable(db);
        new Breach().createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LOGTAG, "Upgrading database from " + oldVersion + " to " + newVersion);
        Log.w(LOGTAG, "Dropping database: " + DATABASE_NAME);
        new Breach().dropTable(db);
        new Account().dropTable(db);
        Log.v(LOGTAG, "Done dropping");
        onCreate(db);
    }
}
