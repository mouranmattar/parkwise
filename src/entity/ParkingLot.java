package entity;

import java.util.ArrayList;
import java.util.List;

public class ParkingLot {
    private int lotNumber;
    private String name;
    private String address;
    private String city;
    private int availableSpaces;
    private List<Conveyor> conveyors;
    private List<PriceList> priceHistory;

    public ParkingLot(int lotNumber, String name, String address, String city, int availableSpaces) {
        this.lotNumber = lotNumber;
        this.name = name;
        this.address = address;
        this.city = city;
        this.availableSpaces = availableSpaces;
        this.conveyors = new ArrayList<>();
        this.priceHistory = new ArrayList<>();
    }

    public int getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(int lotNumber) {
        this.lotNumber = lotNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public int getAvailableSpaces() {
        return availableSpaces;
    }

    public void setAvailableSpaces(int availableSpaces) {
        this.availableSpaces = availableSpaces;
    }

    public void addPriceList(PriceList priceList) {
        this.priceHistory.add(priceList);
    }

    public List<PriceList> getPriceHistory() {
        return priceHistory;
    }

    public Conveyor getConveyor(int id) {
        for (Conveyor conveyor : conveyors) {
            if (conveyor.getId() == id) {
                return conveyor;
            }
        }
        return null;
    }

    public List<Conveyor> getAllConveyors() {
        return conveyors;
    }

    public void addConveyor(Conveyor conveyor) {
        this.conveyors.add(conveyor);
    }

    @Override
    public String toString() {
        return "ParkingLot #" + lotNumber + " - " + name + " (" + address + ", " + city + ") [" + availableSpaces + " spaces]";
    }
}
