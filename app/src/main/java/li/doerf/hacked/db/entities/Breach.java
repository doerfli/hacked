package li.doerf.hacked.db.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "breaches")
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
    @ColumnInfo(name = "is_sensitive")
    private Boolean isSensitive;
    @ColumnInfo(name = "is_retired")
    private Boolean isRetired;
    @ColumnInfo(name = "is_fabricated")
    private Boolean isFabricated;
    @ColumnInfo(name = "is_spam_list")
    private Boolean isSpamList;

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

    public Boolean getSensitive() {
        return isSensitive;
    }

    public void setSensitive(Boolean sensitive) {
        isSensitive = sensitive;
    }

    public Boolean getRetired() {
        return isRetired;
    }

    public void setRetired(Boolean retired) {
        isRetired = retired;
    }

    public Boolean getFabricated() {
        return isFabricated;
    }

    public void setFabricated(Boolean fabricated) {
        isFabricated = fabricated;
    }

    public Boolean getSpamList() {
        return isSpamList;
    }

    public void setSpamList(Boolean spamList) {
        isSpamList = spamList;
    }

    public boolean hasAdditionalFlags() {
        return ! isVerified || isFabricated || isSensitive || isSpamList || isRetired;
    }
}
