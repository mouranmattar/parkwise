package control;

import entity.ParkingLot;
import utils.DatabaseConnection;

import java.util.List;

public class ParkingLotController {

    public List<ParkingLot> getAllLots() {
        return DatabaseConnection.getAllParkingLots();
    }

    public void addLot(String name, String address, String city, int availableSpaces) {
        DatabaseConnection.insertParkingLot(name, address, city, availableSpaces);
    }

    public void updateLot(int id, String name, String address, String city, int availableSpaces) {
        DatabaseConnection.updateParkingLot(id, name, address, city, availableSpaces);
    }

    public void deleteLot(int id) {
        DatabaseConnection.deleteParkingLot(id);
    }
}
