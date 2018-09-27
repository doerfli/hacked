package li.doerf.hacked.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(tableName = "breaches",
        foreignKeys = @ForeignKey(
                entity = Account.class,
                parentColumns = "_id",
                childColumns = "account",
                onDelete = CASCADE)
)
public class Breach {
    @PrimaryKey
    @ColumnInfo(name="_id")
    private Long id;
    @ColumnInfo(name = "account")
    private Long account;
    @ColumnInfo(name = "name")
    private String name;
    @ColumnInfo(name = "title")
    private String title;
    @ColumnInfo(name = "domain")
    private String domain;
    @ColumnInfo(name = "breach_date")
    private Long breachDate;
    @ColumnInfo(name = "added_date")
    private Long addedDate;
    @ColumnInfo(name = "pwn_count")
    private Long pwnCount;
    @ColumnInfo(name = "description")
    private String description;
    @ColumnInfo(name = "data_classes")
    private String dataClasses;
    @ColumnInfo(name = "is_verified")
    private Boolean isVerified;
    @ColumnInfo(name = "is_acknowledged")
    private Boolean isAcknowledged;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccount() {
        return account;
    }

    public void setAccount(Long account) {
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

    public Long getBreachDate() {
        return breachDate;
    }

    public void setBreachDate(Long breachDate) {
        this.breachDate = breachDate;
    }

    public Long getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(Long addedDate) {
        this.addedDate = addedDate;
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

    public Boolean getVerified() {
        return isVerified;
    }

    public void setVerified(Boolean verified) {
        isVerified = verified;
    }

    public Boolean getAcknowledged() {
        return isAcknowledged;
    }

    public void setAcknowledged(Boolean acknowledged) {
        isAcknowledged = acknowledged;
    }
}
