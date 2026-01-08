package org.smartregister.domain;

/**
 * Created by samuelgithengi on 11/22/18.
 */
public class Location extends PhysicalLocation {

    private String syncStatus;
    private Long rowid;

//    Add status to track whether a location is active/inactive
    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
