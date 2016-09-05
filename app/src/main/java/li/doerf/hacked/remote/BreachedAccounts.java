package li.doerf.hacked.remote;

/**
 * Created by moo on 05/09/16.
 */
public class BreachedAccounts {

    private String Name;
    private String Title;
    private String Domain;
    private String BreachDate; // TODO date
    private String AddedDate; // TODO date
    private Long PwnCount;
    private String Description;
    private String[] DataClass;
    private Boolean isVerified;
    private Boolean isSensitive;
    private Boolean isRetired;

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getTitle() {
        return Title;
    }

    public void setTitle(String title) {
        Title = title;
    }

    public String getDomain() {
        return Domain;
    }

    public void setDomain(String domain) {
        Domain = domain;
    }

    public String getBreachDate() {
        return BreachDate;
    }

    public void setBreachDate(String breachDate) {
        BreachDate = breachDate;
    }

    public String getAddedDate() {
        return AddedDate;
    }

    public void setAddedDate(String addedDate) {
        AddedDate = addedDate;
    }

    public Long getPwnCount() {
        return PwnCount;
    }

    public void setPwnCount(Long pwnCount) {
        PwnCount = pwnCount;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String[] getDataClass() {
        return DataClass;
    }

    public void setDataClass(String[] dataClass) {
        DataClass = dataClass;
    }

    public Boolean getVerified() {
        return isVerified;
    }

    public void setVerified(Boolean verified) {
        isVerified = verified;
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


}
