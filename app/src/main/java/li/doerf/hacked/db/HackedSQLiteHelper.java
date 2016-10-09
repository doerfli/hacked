package li.doerf.hacked.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import li.doerf.hacked.db.tables.Account;
import li.doerf.hacked.db.tables.Breach;
import li.doerf.hacked.db.tables.BreachedSite;

/**
 * Created by moo on 29/01/15.
 */
public class HackedSQLiteHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "hacked.db";
    private static final int DATABASE_VERSION = 3;
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
        new BreachedSite().createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LOGTAG, "Upgrading database from " + oldVersion + " to " + newVersion);

        if ( oldVersion < 3 && newVersion >= 3 ) {
            Log.i(LOGTAG, "Creating table for db version 3+");
            new BreachedSite().createTable(db);
        }
    }
}
