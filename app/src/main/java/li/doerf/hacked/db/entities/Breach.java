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
    @ColumnInfo(name = "modified_date")
    private Long modifiedDate;
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
    @ColumnInfo(name = "logo_path")
    private String logoPath;
    @ColumnInfo(name = "last_checked")
    private Long lastChecked;

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

    public Long getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Long modifiedDate) {
        this.modifiedDate = modifiedDate;
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
        if (isVerified == null) {
            return false;
        }
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
        if (isSensitive == null) {
            return false;
        }
        return isSensitive;
    }

    public void setSensitive(Boolean sensitive) {
        isSensitive = sensitive;
    }

    public Boolean getRetired() {
        if (isRetired == null) {
            return false;
        }
        return isRetired;
    }

    public void setRetired(Boolean retired) {
        isRetired = retired;
    }

    public Boolean getFabricated() {
        if (isFabricated == null) {
            return false;
        }
        return isFabricated;
    }

    public void setFabricated(Boolean fabricated) {
        isFabricated = fabricated;
    }

    public Boolean getSpamList() {
        if (isSpamList == null) {
            return false;
        }
        return isSpamList;
    }

    public void setSpamList(Boolean spamList) {
        isSpamList = spamList;
    }

    public boolean hasAdditionalFlags() {
        return ! getVerified() || getFabricated() || getSensitive() || getSpamList() || getRetired();
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public Long getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(Long lastCheckDate) {
        this.lastChecked = lastCheckDate;
    }
}
