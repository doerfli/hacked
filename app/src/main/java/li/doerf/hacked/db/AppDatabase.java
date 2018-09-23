package li.doerf.hacked.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.entities.Account;

@Database(entities = {Account.class}, version = 4)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(
                SupportSQLiteDatabase database) {
            // Since we didn’t alter the table, there’s nothing else
            // to do here.
        }
    };

    public static AppDatabase get(Context context) {
        synchronized (AppDatabase.class) {
            if (instance != null) {
                return instance;
            }

            instance = Room.databaseBuilder(context, AppDatabase.class, "hacked.db")
                .addMigrations(MIGRATION_3_4)
                    .build();
            return instance;
        }
    }

    public abstract AccountDao getAccountDao();
}
