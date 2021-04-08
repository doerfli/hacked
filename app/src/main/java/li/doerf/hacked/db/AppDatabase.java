package li.doerf.hacked.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import li.doerf.hacked.db.daos.AccountDao;
import li.doerf.hacked.db.daos.BreachDao;
import li.doerf.hacked.db.daos.BreachedSiteDao;
import li.doerf.hacked.db.entities.Account;
import li.doerf.hacked.db.entities.Breach;
import li.doerf.hacked.db.entities.BreachedSite;

@Database(entities = {Account.class, Breach.class, BreachedSite.class}, version = 7)
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

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(
                SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `accounts` ADD COLUMN `num_breaches` INTEGER;");
            database.execSQL("ALTER TABLE `accounts` ADD COLUMN `num_acknowledged_breaches` INTEGER;");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(
                SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `breaches` ADD COLUMN `is_sensitive` INTEGER DEFAULT 0;");
            database.execSQL("ALTER TABLE `breaches` ADD COLUMN `is_retired` INTEGER DEFAULT 0;");
            database.execSQL("ALTER TABLE `breaches` ADD COLUMN `is_fabricated` INTEGER DEFAULT 0;");
            database.execSQL("ALTER TABLE `breaches` ADD COLUMN `is_spam_list` INTEGER DEFAULT 0;");
            database.execSQL("ALTER TABLE `breached_sites` ADD COLUMN `is_sensitive` INTEGER DEFAULT 0;");
            database.execSQL("ALTER TABLE `breached_sites` ADD COLUMN `is_retired` INTEGER DEFAULT 0;");
            database.execSQL("ALTER TABLE `breached_sites` ADD COLUMN `is_fabricated` INTEGER DEFAULT 0;");
            database.execSQL("ALTER TABLE `breached_sites` ADD COLUMN `is_spam_list` INTEGER DEFAULT 0;");
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(
                SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `breaches` ADD COLUMN `logo_path` TEXT DEFAULT null;");
            database.execSQL("ALTER TABLE `breached_sites` ADD COLUMN `logo_path` TEXT DEFAULT null;");
        }
    };

    public static AppDatabase get(Context context) {
        synchronized (AppDatabase.class) {
            if (instance != null) {
                return instance;
            }

            instance = Room.databaseBuilder(context, AppDatabase.class, "hacked.db")
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .build();
            return instance;
        }
    }

    public abstract AccountDao getAccountDao();

    public abstract BreachDao getBreachDao();

    public abstract BreachedSiteDao getBrachedSiteDao();
}
