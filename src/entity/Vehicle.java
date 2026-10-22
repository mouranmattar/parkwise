package entity;

public class Vehicle {
    private final int vehicleNumber;
    private final String type;
    private final String color;
    private final int size; // 1=small,2=medium,3=large
    private final double weight;
    private final String photoFront;
    private final String photoRear;
    private final String photoSide;
    private final Integer clientId;

    public Vehicle(int vehicleNumber, String type, String color, int size, double weight,
                   String photoFront, String photoRear, String photoSide, Integer clientId) {
        this.vehicleNumber = vehicleNumber;
        this.type = type;
        this.color = color;
        this.size = size;
        this.weight = weight;
        this.photoFront = photoFront;
        this.photoRear = photoRear;
        this.photoSide = photoSide;
        this.clientId = clientId;
    }

    public int getVehicleNumber() { return vehicleNumber; }
    public String getType() { return type; }
    public String getColor() { return color; }
    public int getSize() { return size; }
    public double getWeight() { return weight; }
    public String getPhotoFront() { return photoFront; }
    public String getPhotoRear() { return photoRear; }
    public String getPhotoSide() { return photoSide; }
    public Integer getClientId() { return clientId; }
}
