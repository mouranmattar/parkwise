package control;

import entity.Vehicle;
import entity.ParkingSession;
import entity.ParkingSpot;
import entity.ParkingLot;
import entity.Conveyor;
import utils.DatabaseConnection;
import control.AllocationService.AllocationResult;

import java.sql.Timestamp;
import java.util.List;

/**
 * EntryController - Controller for vehicle entry and self-registration operations.
 * Handles business logic for entry simulation, client registration, and session start.
 * Part of BCE architecture (Controller layer).
 */
public class EntryController {

    /**
     * Get all parking lots.
     */
    public List<ParkingLot> getAllParkingLots() {
        return DatabaseConnection.getAllParkingLots();
    }

    /**
     * Check if system is ready (has at least one active conveyor).
     */
    public boolean isSystemReady(int lotId) {
        List<Conveyor> conveyors = DatabaseConnection.getConveyorsByLot(lotId);
        for (Conveyor c : conveyors) {
            if (c.getStatus() == entity.ConveyorStatus.ACTIVE) return true;
        }
        return false;
    }

    /**
     * Check if a vehicle exists in the system.
     */
    public boolean vehicleExists(int vehicleNumber) {
        return DatabaseConnection.vehicleExists(vehicleNumber);
    }

    /**
     * Get vehicle details by number.
     */
    public Vehicle getVehicle(int vehicleNumber) {
        return DatabaseConnection.getVehicle(vehicleNumber);
    }

    /**
     * Check if there is an active session for a vehicle.
     */
    public boolean hasActiveSession(int vehicleNumber) {
        return DatabaseConnection.getActiveSessionByVehicle(vehicleNumber) != null;
    }

    /**
     * Get active session for a vehicle.
     */
    public ParkingSession getActiveSession(int vehicleNumber) {
        return DatabaseConnection.getActiveSessionByVehicle(vehicleNumber);
    }

    /**
     * Check if parking lot has available spots for a vehicle.
     */
    public boolean hasAvailableSpot(int lotId, int vehicleSize, double vehicleWeight) {
        return AllocationService.hasAvailableSpot(lotId, vehicleSize, vehicleWeight);
    }

    /**
     * Get available parking spaces for a lot.
     */
    public List<ParkingSpot> getAvailableParkingSpaces(int lotId) {
        return DatabaseConnection.getAvailableParkingSpaces(lotId);
    }

    /**
     * Assign vehicle to a parking spot using AllocationService.
     */
    public AllocationResult assignVehicleToSpot(int vehicleNumber, int lotId) {
        return AllocationService.assignVehicleToSpot(vehicleNumber, lotId);
    }

    /**
     * Find client by phone number.
     */
    public Integer findClientIdByPhone(String phone) {
        return DatabaseConnection.findClientIdByPhone(phone);
    }

    /**
     * Register a new client.
     */
    public int registerClient(String firstName, String lastName, String phone) {
        Integer clientId = DatabaseConnection.insertClient(firstName, lastName, phone);
        if (clientId == null || clientId <= 0) {
            throw new RuntimeException("Failed to create client record");
        }
        return clientId;
    }

    /**
     * Register or update vehicle information.
     */
    public void registerVehicle(int vehicleNumber, String type, String color, int size, 
                                 double weight, int clientId, String frontPhoto, 
                                 String rearPhoto, String sidePhoto) {
        DatabaseConnection.upsertVehicle(vehicleNumber, type, color, size, weight, 
                                          clientId, frontPhoto, rearPhoto, sidePhoto);
    }

    /**
     * Register a new vehicle for an existing client (client already in DB).
     * This is used when a returning client arrives with a new vehicle.
     */
    public void registerVehicleForClient(int vehicleNumber, String type, String color, int size,
                                          double weight, int clientId, String frontPhoto,
                                          String rearPhoto, String sidePhoto) {
        DatabaseConnection.upsertVehicle(vehicleNumber, type, color, size, weight,
                                          clientId, frontPhoto, rearPhoto, sidePhoto);
    }

    /**
     * Start a parking session for a vehicle.
     */
    public void startParkingSession(int vehicleNumber, int lotId, Timestamp startTime, int spotId) {
        DatabaseConnection.startParkingSession(vehicleNumber, lotId, startTime, spotId);
    }

    /**
     * Complete entry process for a known vehicle (existing in system).
     * Returns allocation result message.
     */
    public String processKnownVehicleEntry(int vehicleNumber, int lotId) {
        // Check if already has active session
        if (hasActiveSession(vehicleNumber)) {
            return "Vehicle " + vehicleNumber + " already has an active session.";
        }

        // Assign to spot
        AllocationResult alloc = assignVehicleToSpot(vehicleNumber, lotId);
        if (!alloc.success) {
            throw new IllegalStateException("Allocation Failed: " + alloc.message);
        }

        // Start session
        startParkingSession(vehicleNumber, lotId, 
                          new Timestamp(System.currentTimeMillis()), alloc.spotId);

        return "Started session for vehicle " + vehicleNumber + " at lot " + lotId + 
               " (Spot " + alloc.spotId + ")";
    }

    /**
     * Complete registration and entry for a new vehicle.
     * Returns allocation result message.
     */
    public String processNewVehicleEntry(int vehicleNumber, String firstName, String lastName, 
                                        String phone, int lotId, String type, String color, 
                                        int size, double weight, String frontPhoto, 
                                        String rearPhoto, String sidePhoto) {
        // Check availability first (fail-fast)
        if (!hasAvailableSpot(lotId, size, weight)) {
            throw new IllegalStateException("Parking Lot Full: No suitable spot fits size " + 
                                          size + " or conveyor capacity.");
        }

        // Find or create client
        Integer clientId = findClientIdByPhone(phone);
        if (clientId == null) {
            clientId = registerClient(firstName, lastName, phone);
        }

        // Register vehicle
        registerVehicle(vehicleNumber, type, color, size, weight, clientId, 
                       frontPhoto, rearPhoto, sidePhoto);

        // Process entry
        return processKnownVehicleEntry(vehicleNumber, lotId);
    }
}
