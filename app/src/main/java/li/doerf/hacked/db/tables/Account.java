package li.doerf.hacked.db.tables;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.lang.reflect.Field;
import java.util.Map;

import li.doerf.hacked.db.annotations.Column;
import li.doerf.hacked.db.annotations.Table;

/**
 * Created by pamapa on 04.01.16.
 */
@Table(name = "accounts")
public class Account extends TableBase {
    @Column(name = "_id", type = "INTEGER", isPrimaryKey = true, isAutoincrement = true)
    private Long id;
    @Column(name = "name", type = "TEXT")
    private String name;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Override
    protected TableBase getReference(SQLiteDatabase db, String aReferenceName, Long anId) {
        return null;
    }

    public String getName() { return name; }
    public void setName(String key) { this.name = key; }

    public static Account create(String aName) {
        Account property = new Account();
        property.setName(aName);
        return property;
    }

    public static Account create(SQLiteDatabase db, Cursor aCursor) {
        Account item = new Account();
        Map<String, Field> columnNamesAndFields = item.getColumnNamesWithFields();
        item.fillFromCursor(db, aCursor, columnNamesAndFields);
        return item;
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

    public static Cursor listAll(SQLiteDatabase db) {
        Account item = new Account();
        return db.query(
                item.getTableName(),
                item.getColumnNames(),
                null,
                null,
                null,
                null,
                "name");
    }

}
