package entity;

import java.sql.Timestamp;

public class ParkingSession {
    private int sessionID;
    private Timestamp startTime;
    private Timestamp endTime;
    private double finalAmount;
    private String status;
    private int vehicleNumber;
    private int parkingLotID;
    private int parkingSpaceID;

    public ParkingSession(int sessionID, Timestamp startTime, Timestamp endTime,
                          double finalAmount, String status, int vehicleNumber, int parkingLotID) {
        this.sessionID = sessionID;
        this.startTime = startTime;
        this.endTime = endTime;
        this.finalAmount = finalAmount;
        this.status = status;
        this.vehicleNumber = vehicleNumber;
        this.parkingLotID = parkingLotID;
    }

    public int getParkingSpaceID() {
        return parkingSpaceID;
    }

    public void setParkingSpaceID(int parkingSpaceID) {
        this.parkingSpaceID = parkingSpaceID;
    }

    public int getSessionID() {
        return sessionID;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public String getStatus() {
        return status;
    }

    public int getVehicleNumber() {
        return vehicleNumber;
    }

    public int getParkingLotID() {
        return parkingLotID;
    }
}
