package entity;

/**
 * Entity class representing the ParkingSpace/ParkingSpot table in the database.
 * Note: Database table is named 'ParkingSpace' but entity uses 'ParkingSpot' naming.
 */
public class ParkingSpot {
    private int parkingSpaceID;
    private int floorNumber;
    private int xPosition;
    private int yPosition;
    private String size;
    private boolean isOccupied;
    private int parkingLotID;

    public ParkingSpot() {}

    public ParkingSpot(int parkingSpaceID, int floorNumber, int xPosition, int yPosition, 
                       String size, boolean isOccupied, int parkingLotID) {
        this.parkingSpaceID = parkingSpaceID;
        this.floorNumber = floorNumber;
        this.xPosition = xPosition;
        this.yPosition = yPosition;
        this.size = size;
        this.isOccupied = isOccupied;
        this.parkingLotID = parkingLotID;
    }

    // Getters and Setters
    public int getParkingSpaceID() { return parkingSpaceID; }
    public void setParkingSpaceID(int parkingSpaceID) { this.parkingSpaceID = parkingSpaceID; }
    
    // Alias for compatibility with existing code
    public int getId() { return parkingSpaceID; }

    public int getFloorNumber() { return floorNumber; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }

    public int getXPosition() { return xPosition; }
    public void setXPosition(int xPosition) { this.xPosition = xPosition; }
    
    // Alias for compatibility
    public int getX() { return xPosition; }

    public int getYPosition() { return yPosition; }
    public void setYPosition(int yPosition) { this.yPosition = yPosition; }
    
    // Alias for compatibility
    public int getY() { return yPosition; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    
    /**
     * Converts String size to int for comparison with Vehicle size.
     * 1=small, 2=medium, 3=large
     */
    public int getSizeAsInt() {
        if (size == null) return 2; // default medium
        switch (size.toLowerCase()) {
            case "small": return 1;
            case "large": return 3;
            case "medium":
            default: return 2;
        }
    }

    public boolean isOccupied() { return isOccupied; }
    public void setOccupied(boolean occupied) { isOccupied = occupied; }

    public int getParkingLotID() { return parkingLotID; }
    public void setParkingLotID(int parkingLotID) { this.parkingLotID = parkingLotID; }
    
    // Alias for compatibility
    public int getParkingLotId() { return parkingLotID; }
}
