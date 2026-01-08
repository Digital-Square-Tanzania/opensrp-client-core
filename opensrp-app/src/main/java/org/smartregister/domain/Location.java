package org.smartregister.domain;

/**
 * Created by samuelgithengi on 11/22/18.
 */
public class Location extends PhysicalLocation {

    private String syncStatus;
    private Long rowid;

//    Add operatingStatus to track whether a location is active/inactive
    private String operatingStatus;

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public Long getRowid() {
        return rowid;
    }

    public void setRowid(Long rowid) {
        this.rowid = rowid;
    }

    public String getOperatingStatus() {
        return operatingStatus;
    }

    public void setOperatingStatus(String operatingStatus) {
        this.operatingStatus = operatingStatus;
    }
}
