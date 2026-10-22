package entity;

import java.time.LocalDateTime;

/**
 * Entity class representing the PreferredSelection table in the database.
 */
public class PreferredSelection {
    private int preferredSelectionID;
    private LocalDateTime selectionDate;
    private int parkingLotID;
    private int clientID;

    public PreferredSelection() {}

    public PreferredSelection(int preferredSelectionID, LocalDateTime selectionDate, int parkingLotID, int clientID) {
        this.preferredSelectionID = preferredSelectionID;
        this.selectionDate = selectionDate;
        this.parkingLotID = parkingLotID;
        this.clientID = clientID;
    }

    public int getPreferredSelectionID() { return preferredSelectionID; }
    public void setPreferredSelectionID(int preferredSelectionID) { this.preferredSelectionID = preferredSelectionID; }

    public LocalDateTime getSelectionDate() { return selectionDate; }
    public void setSelectionDate(LocalDateTime selectionDate) { this.selectionDate = selectionDate; }

    public int getParkingLotID() { return parkingLotID; }
    public void setParkingLotID(int parkingLotID) { this.parkingLotID = parkingLotID; }

    public int getClientID() { return clientID; }
    public void setClientID(int clientID) { this.clientID = clientID; }
}
