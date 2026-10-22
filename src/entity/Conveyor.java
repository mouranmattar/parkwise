package entity;

public class Conveyor {
    private int id;
    private int parkingLotID;
    private int currentFloor;
    private int currentX;
    private int currentY;
    private double maxWeight;
    private ConveyorStatus status;
    private int failedCheckAttempts;
    private boolean isWeightChangePending;

    public Conveyor(int id, int parkingLotID, int currentFloor, int currentX, int currentY, double maxWeight) {
        this.id = id;
        this.parkingLotID = parkingLotID;
        this.currentFloor = currentFloor;
        this.currentX = currentX;
        this.currentY = currentY;
        this.maxWeight = maxWeight;
        this.status = ConveyorStatus.OFF;
        this.failedCheckAttempts = 0;
        this.isWeightChangePending = false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getParkingLotID() {
        return parkingLotID;
    }

    public void setParkingLotID(int parkingLotID) {
        this.parkingLotID = parkingLotID;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public void setCurrentFloor(int currentFloor) {
        this.currentFloor = currentFloor;
    }

    public int getCurrentX() {
        return currentX;
    }

    public void setCurrentX(int currentX) {
        this.currentX = currentX;
    }

    public int getCurrentY() {
        return currentY;
    }

    public void setCurrentY(int currentY) {
        this.currentY = currentY;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public boolean isWeightChangePending() {
        return isWeightChangePending;
    }

    public void setWeightChangePending(boolean weightChangePending) {
        isWeightChangePending = weightChangePending;
    }

    public void setStatus(ConveyorStatus status) {
        this.status = status;
    }

    public ConveyorStatus getStatus() {
        return status;
    }

    public void setMaxWeight(double weight) {
        this.maxWeight = weight;
    }

    public boolean runMechanicalCheck() {
        try {
            Thread.sleep(100);
            return Math.random() > 0.1;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public boolean runElectronicCheck() {
        try {
            Thread.sleep(100);
            return Math.random() > 0.1;
        } catch (InterruptedException e) {
            return false;
        }
    }

    public void resetCheckAttempts() {
        this.failedCheckAttempts = 0;
    }

    public void incrementFailedAttempts() {
        this.failedCheckAttempts++;
    }

    public int getFailedAttempts() {
        return failedCheckAttempts;
    }

    public void setFailedCheckAttempts(int attempts) {
        this.failedCheckAttempts = attempts;
    }

    public void calculateOptimalRoute(int targetX, int targetY) {
        System.out.println("Calculating optimal route from (" + currentX + "," + currentY + 
                         ") to (" + targetX + "," + targetY + ")");
    }

    @Override
    public String toString() {
        return "Conveyor #" + id + " [Status: " + status + ", Floor: " + currentFloor + 
               ", Position: (" + currentX + "," + currentY + "), Max Weight: " + maxWeight + "kg]";
    }
}
