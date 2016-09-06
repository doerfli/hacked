package li.doerf.hacked.db.tables;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.base.Joiner;

import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import li.doerf.hacked.db.annotations.Column;
import li.doerf.hacked.db.annotations.Table;

@Table(name = "breaches")
public class Breach extends TableBase {
    private static Map<Long, Account> accountCache = new HashMap<>();

    @Column(name = "_id", type = "INTEGER", isPrimaryKey = true, isAutoincrement = true)
    private Long id;
    @Column(name = "account", type = "INTEGER", isReference = true)
    private Account account;
    @Column(name = "name", type = "TEXT")
    private String name;
    @Column(name = "title", type = "TEXT")
    private String title;
    @Column(name = "domain", type = "TEXT")
    private String domain;
    @Column(name = "breach_date", type = "INTEGER")
    private Long breachDate;
    @Column(name = "added_date", type = "INTEGER")
    private Long addedDate;
    @Column(name = "pwn_count", type = "INTEGER")
    private Long pwnCount;
    @Column(name = "description", type = "TEXT")
    private String description;
    @Column(name = "data_classes", type = "TEXT")
    private String dataClasses;
    @Column(name = "is_verified", type = "INTEGER")
    private Boolean isVerified;
    @Column(name = "is_acknowledged", type = "INTEGER")
    private Boolean isAcknowledged;


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public DateTime getBreachDate() {
        return breachDate != null ? new DateTime(breachDate) : null;
    }

    public void setBreachDate(DateTime breachDate) {
        this.breachDate = breachDate != null ? breachDate.getMillis() : null;
    }

    public DateTime getAddedDate() {
        return addedDate != null ? new DateTime(addedDate) : null;
    }

    public void setAddedDate(DateTime addedDate) {
        this.addedDate = addedDate != null ? addedDate.getMillis() : null;
    }

    public Long getPwnCount() {
        return pwnCount;
    }

    public void setPwnCount(Long pwnCount) {
        this.pwnCount = pwnCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDataClasses() {
        return dataClasses;
    }

    public void setDataClasses(String dataClasses) {
        this.dataClasses = dataClasses;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean verified) {
        isVerified = verified;
    }

    public Boolean getIsAcknowledged() {
        return isAcknowledged;
    }

    public void setIsAcknowledged(Boolean isAcknowledged) {
        this.isAcknowledged = isAcknowledged;
    }

    public static Breach create( Account account,
                                 String name,
                                 String title,
                                 String domain,
                                 DateTime breachedDate,
                                 DateTime addedDate,
                                 Long pwnCount,
                                 String description,
                                 String[] dataClasses,
                                 Boolean isVerified,
                                 Boolean isAcknowledged) {
        Breach breach = new Breach();
        breach.setAccount(account);
        breach.setName(name);
        breach.setTitle(title);
        breach.setDomain(domain);
        breach.setBreachDate(breachedDate);
        breach.setAddedDate(addedDate);
        breach.setPwnCount(pwnCount);
        breach.setDescription(description);
        breach.setDataClasses(dataClasses != null ? Joiner.on(",").join(dataClasses) : "");
        breach.setIsVerified(isVerified);
        breach.setIsAcknowledged(isAcknowledged);
        return breach;
    }

    public static Breach create(SQLiteDatabase db, Cursor aCursor) {
        Breach item = new Breach();
        Map<String, Field> columnNamesAndFields = item.getColumnNamesWithFields();
        item.fillFromCursor(db, aCursor, columnNamesAndFields);
        return item;
    }

    @Override
    protected TableBase getReference(SQLiteDatabase db, String aReferenceName, Long anId) {
        Cursor c = null;
        try {
            if ("account".equals(aReferenceName)) {
                Account item = accountCache.get(anId);
                if (item != null) {
                    return item;
                }
                item = new Account();
                c = db.query(
                        item.getTableName(),
                        item.getColumnNames(),
                        "_id = ?",
                        new String[]{anId.toString()},
                        null,
                        null,
                        "_id");

                if (c.moveToFirst()) {
                    item = Account.create(db, c);
                    accountCache.put(anId, item);
                    return item;
                }
            }
            return null;
        } finally {
            if ( c != null ) {
                c.close();
            }
        }
    }

    public static Cursor listAll(SQLiteDatabase db) {
        Breach item = new Breach();
        return db.query(
                item.getTableName(),
                item.getColumnNames(),
                null,
                null,
                null,
                null,
                "name");
    }

    public static Cursor findByAccount(SQLiteDatabase db, Account account) {
        Breach item = new Breach();
        return db.query(
                item.getTableName(),
                item.getColumnNames(),
                "account == ?",
                new String[]{account.getId().toString()},
                null,
                null,
                "name");
    }

    public static Breach findByAccountAndName(SQLiteDatabase db, Account account, String aName) {
        Cursor c = null;

        try {
            Breach item = new Breach();
            c = db.query(
                    item.getTableName(),
                    item.getColumnNames(),
                    "account == ? AND name == ?",
                    new String[]{account.getId().toString(), aName},
                    null,
                    null,
                    "name");

            if (c.moveToFirst()) {
                return Breach.create(db, c);
            }

            return null;
        } finally {
            if ( c != null ) {
                c.close();
            }
        }
    }
}
