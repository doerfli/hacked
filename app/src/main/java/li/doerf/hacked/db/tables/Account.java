package li.doerf.hacked.db.tables;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.base.Joiner;

import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.util.Map;

import li.doerf.hacked.db.annotations.Column;
import li.doerf.hacked.db.annotations.Table;

@Table(name = "accounts")
public class Account extends TableBase {
    @Column(name = "_id", type = "INTEGER", isPrimaryKey = true, isAutoincrement = true)
    private Long id;
    @Column(name = "name", type = "TEXT")
    private String name;
    @Column(name = "last_checked", type = "INTEGER")
    private Long lastChecked;
    @Column(name = "is_hacked", type = "INTEGER")
    private Boolean isHacked;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    protected TableBase getReference(SQLiteDatabase db, String aReferenceName, Long anId) {
        return null;
    }

    public String getName() { return name; }
    public void setName(String key) { this.name = key; }

    public DateTime getLastChecked() {
        return lastChecked <= 0 ? null : new DateTime(lastChecked);
    }

    public void setLastChecked(DateTime lastChecked) {
        this.lastChecked = lastChecked != null ? lastChecked.getMillis() : null;
    }

    public Boolean isHacked() {
        return isHacked;
    }

    public void setHacked(Boolean hacked) {
        isHacked = hacked;
    }

    public static Account create(String aName) {
        Account acc = new Account();
        acc.setName(aName);
        acc.setHacked(false);
        acc.setLastChecked(null);
        return acc;
    }

    public static Account create(SQLiteDatabase db, Cursor aCursor) {
        Account item = new Account();
        Map<String, Field> columnNamesAndFields = item.getColumnNamesWithFields();
        item.fillFromCursor(db, aCursor, columnNamesAndFields);
        return item;
    }

    public static Account findById(SQLiteDatabase db, Long anId) {
        Cursor c = null;

        try {
            c = findCursorById(db, anId);

            if (c.moveToFirst()) {
                return Account.create(db, c);
            }

            return null;
        } finally {
            if ( c != null ) {
                c.close();
            }
        }
    }

    public static Cursor findCursorById(SQLiteDatabase db, Long id) {
        Account item = new Account();
        return db.query(
                item.getTableName(),
                item.getColumnNames(),
                "_id = ?",
                new String[]{id.toString()},
                null,
                null,
                "name");
    }

    public static Cursor findCursorByIds(SQLiteDatabase db, Long[] ids) {
        Account item = new Account();
        return db.query(
                item.getTableName(),
                item.getColumnNames(),
                "_id IN (?)",
                new String[]{Joiner.on(',').join(ids)},
                null,
                null,
                "name");
    }

    public static Account findByName(SQLiteDatabase db, String aName) {
        Cursor c = null;

        try {
            Account item = new Account();
            c = db.query(
                    item.getTableName(),
                    item.getColumnNames(),
                    "name = ?",
                    new String[]{aName},
                    null,
                    null,
                    "name");

            if (c.moveToFirst()) {
                return Account.create(db, c);
            }

            return null;
        } finally {
            if ( c != null ) {
                c.close();
            }
        }
    }

    public boolean exists(SQLiteDatabase db) {
        return findByName(db, getName()) != null;
    }

    public static Cursor listAll(SQLiteDatabase db) {
        Account item = new Account();
        return db.query(
                item.getTableName(),
                item.getColumnNames(),
                null,
                null,
                null,
                null,
                "is_hacked DESC, name");
    }

    public void updateIsHacked(SQLiteDatabase db) {
        if ( ! isHacked() ) {
            return;
        }

        if ( Breach.countUnacknowledged( db, this) > 0 ) {
            return;
        }

        setHacked(false);
        update(db);
    }
}
