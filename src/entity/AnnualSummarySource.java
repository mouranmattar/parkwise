package entity;

public class AnnualSummarySource {
    private final int parkingLotID;
    private final String plName;
    private final String city;
    private final double totalAmount;
    private final long otherCount;

    public AnnualSummarySource(int parkingLotID, String plName, String city, double totalAmount, long otherCount) {
        this.parkingLotID = parkingLotID;
        this.plName = plName;
        this.city = city;
        this.totalAmount = totalAmount;
        this.otherCount = otherCount;
    }

    public int getParkingLotID() {
        return parkingLotID;
    }

    public String getPlName() {
        return plName;
    }

    public String getCity() {
        return city;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public long getOtherCount() {
        return otherCount;
    }
}
