package li.doerf.hacked.db.tables;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.common.base.Joiner;

import org.joda.time.DateTime;

import java.lang.reflect.Field;
import java.util.Map;

import li.doerf.hacked.db.annotations.Column;
import li.doerf.hacked.db.annotations.Table;
import li.doerf.hacked.utils.Identifiable;

/**
 * This table is used to store general breach information
 */
@Table(name = "breached_sites")
public class BreachedSite extends TableBase implements Identifiable {
//    private static Map<Long, Account> accountCache = new HashMap<>();

    @Column(name = "_id", type = "INTEGER", isPrimaryKey = true, isAutoincrement = true)
    private Long id;
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


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public static BreachedSite create(String name,
                                      String title,
                                      String domain,
                                      DateTime breachedDate,
                                      DateTime addedDate,
                                      Long pwnCount,
                                      String description,
                                      String[] dataClasses,
                                      Boolean isVerified) {
        BreachedSite breach = new BreachedSite();
        breach.setName(name);
        breach.setTitle(title);
        breach.setDomain(domain);
        breach.setBreachDate(breachedDate);
        breach.setAddedDate(addedDate);
        breach.setPwnCount(pwnCount);
        breach.setDescription(description);
        breach.setDataClasses(dataClasses != null ? Joiner.on(", ").join(dataClasses) : "");
        breach.setIsVerified(isVerified);
        return breach;
    }

    public static BreachedSite create(SQLiteDatabase db, Cursor aCursor) {
        BreachedSite item = new BreachedSite();
        Map<String, Field> columnNamesAndFields = item.getColumnNamesWithFields();
        item.fillFromCursor(db, aCursor, columnNamesAndFields);
        return item;
    }

    @Override
    protected TableBase getReference(SQLiteDatabase db, String aReferenceName, Long anId) {
        return null;
    }

    public static Cursor listAll(SQLiteDatabase db) {
        return listAll(db, "name");
    }

    public static Cursor listAll(SQLiteDatabase db, String order) {
        BreachedSite item = new BreachedSite();
        return db.query(
                item.getTableName(),
                item.getColumnNames(),
                null,
                null,
                null,
                null,
                order);
    }

    public static Cursor listTop20(SQLiteDatabase db) {
        BreachedSite item = new BreachedSite();
        return db.query(
                item.getTableName(),
                item.getColumnNames(),
                null,
                null,
                null,
                null,
                "pwn_count DESC",
                "20");
    }

    public static Cursor listMostRecent(SQLiteDatabase db) {
        BreachedSite item = new BreachedSite();
        return db.query(
                item.getTableName(),
                item.getColumnNames(),
                null,
                null,
                null,
                null,
                "added_date DESC",
                "20");
    }

    public static BreachedSite findById(SQLiteDatabase db, Long anId) {
        Cursor c = null;

        try {
            BreachedSite item = new BreachedSite();
            c = db.query(
                    item.getTableName(),
                    item.getColumnNames(),
                    "_id = ?",
                    new String[]{anId.toString()},
                    null,
                    null,
                    "name");

            if (c.moveToFirst()) {
                return BreachedSite.create(db, c);
            }

            return null;
        } finally {
            if ( c != null ) {
                c.close();
            }
        }
    }

    public static void deleteAll(SQLiteDatabase db) {
        BreachedSite site = new BreachedSite();
        db.delete(site.getTableName(), null, null);
    }


}
