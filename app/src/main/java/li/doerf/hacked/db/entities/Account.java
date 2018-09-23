package li.doerf.hacked.db.entities;

import org.joda.time.DateTime;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "accounts")
public class Account {

    @PrimaryKey
    @ColumnInfo(name="_id")
    private Long id;
    @ColumnInfo(name="name")
    private String name;
    @ColumnInfo(name="last_checked")
    private Long lastChecked;
    @ColumnInfo(name="is_hacked")
    private Boolean isHacked = false;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLastChecked() {
        return lastChecked;
    }

    public DateTime getLastCheckedDateTime() {
        return lastChecked <= 0 ? null : new DateTime(lastChecked);
    }

    public void setLastChecked(DateTime lastChecked) {
        this.lastChecked = lastChecked != null ? lastChecked.getMillis() : null;
    }
    public void setLastChecked(Long lastChecked) {
        this.lastChecked = lastChecked;
    }

    public Boolean getHacked() {
        return isHacked;
    }

    public void setHacked(Boolean hacked) {
        isHacked = hacked;
    }


}
