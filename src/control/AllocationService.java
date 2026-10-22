package control;

import entity.Conveyor;
import entity.ConveyorStatus;
import entity.ParkingSpot;
import entity.Vehicle;
import utils.DatabaseConnection;
import utils.NotificationService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AllocationService {

    public static class AllocationResult {
        public final boolean success;
        public final int conveyorId;
        public final int spotId;
        public final int floor;
        public final String message;

        public AllocationResult(boolean success, int conveyorId, int spotId, int floor, String message) {
            this.success = success;
            this.conveyorId = conveyorId;
            this.spotId = spotId;
            this.floor = floor;
            this.message = message;
        }
    }

    public static class AllocationRecord {
        public final int vehicleNumber;
        public final int conveyorId;
        public final int spotId;
        public final int floor;
        public final String status;

        public AllocationRecord(int vehicleNumber, int conveyorId, int spotId, int floor, String status) {
            this.vehicleNumber = vehicleNumber;
            this.conveyorId = conveyorId;
            this.spotId = spotId;
            this.floor = floor;
            this.status = status;
        }
    }

    private static final List<AllocationRecord> activeAllocations = new ArrayList<>();

    public static boolean hasAvailableSpot(int lotId, int vehicleSize, double vehicleWeight) {
        // Check if there is any active conveyor that can handle the weight
        List<entity.Conveyor> conveyors = DatabaseConnection.getConveyorsByLot(lotId).stream()
                .filter(c -> c.getStatus() == entity.ConveyorStatus.ACTIVE 
                        && c.getMaxWeight() >= vehicleWeight)
                .collect(Collectors.toList());
        if (conveyors.isEmpty()) return false;

        // Check if there is any available spot that fits the size
        List<entity.ParkingSpot> spots = DatabaseConnection.getAvailableParkingSpaces(lotId).stream()
                .filter(s -> vehicleSize <= s.getSizeAsInt())
                .collect(Collectors.toList());
        
        return !spots.isEmpty();
    }

    public static AllocationResult assignVehicleToSpot(int vehicleNumber, int lotId) {
        Vehicle vehicle = DatabaseConnection.getVehicle(vehicleNumber);
        if (vehicle == null) {
            return new AllocationResult(false, -1, -1, -1, "Vehicle " + vehicleNumber + " not found in DB");
        }

        List<Conveyor> conveyors = DatabaseConnection.getConveyorsByLot(lotId).stream()
                .filter(c -> c.getStatus() == ConveyorStatus.ACTIVE && c.getMaxWeight() >= vehicle.getWeight())
                .collect(Collectors.toList());
        if (conveyors.isEmpty()) {
            return new AllocationResult(false, -1, -1, -1, "No ACTIVE conveyor can handle weight " + vehicle.getWeight());
        }

        List<ParkingSpot> spots = DatabaseConnection.getAvailableParkingSpaces(lotId).stream()
                .filter(s -> vehicle.getSize() <= s.getSizeAsInt())
                .collect(Collectors.toList());
        if (spots.isEmpty()) {
            return new AllocationResult(false, -1, -1, -1, "No available parking spot fits size " + vehicle.getSize());
        }

        // pick best (conveyor, spot) by minimal distance
        Optional<Selection> best = conveyors.stream()
                .filter(c -> !isConveyorBusy(c.getId())) // Ensure the conveyor is not currently busy
                .flatMap(c -> spots.stream().map(s -> new Selection(c, s, distance(c, s))))
                .min(Comparator.comparingDouble(s -> s.distance));

        if (!best.isPresent()) {
            return new AllocationResult(false, -1, -1, -1, "No feasible conveyor/spot pair found");
        }

        Selection sel = best.get();
        // mark conveyor busy during move
        sel.conveyor.setStatus(ConveyorStatus.PAUSED_BUSY);
        DatabaseConnection.updateConveyorStatus(sel.conveyor.getId(), ConveyorStatus.PAUSED_BUSY);
        DatabaseConnection.setParkingSpaceOccupied(sel.spot.getId(), true);

        AllocationResult result = new AllocationResult(true, sel.conveyor.getId(), sel.spot.getId(), sel.spot.getFloorNumber(),
                "Allocated vehicle " + vehicleNumber + " to spot " + sel.spot.getId() + " via conveyor " + sel.conveyor.getId() +
                        " (floor " + sel.spot.getFloorNumber() + ", dist=" + (int)sel.distance + ")");

        synchronized (activeAllocations) {
            activeAllocations.removeIf(r -> r.vehicleNumber == vehicleNumber);
            activeAllocations.add(new AllocationRecord(vehicleNumber, sel.conveyor.getId(), sel.spot.getId(), sel.spot.getFloorNumber(), "On conveyor"));
        }

        // Simulate transport time (20 seconds) then mark as parked automatically
        new javax.swing.Timer(20000, e -> {
            markParked(vehicleNumber);
            ((javax.swing.Timer)e.getSource()).stop();
        }).start();

        // Notify client with parking details if phone exists
        String phone = DatabaseConnection.getClientPhoneByVehicle(vehicleNumber);
        if (phone != null && !phone.trim().isEmpty()) {
            NotificationService.sendSMS(phone, "ParkWise: Vehicle " + vehicleNumber + " allocated to spot " + sel.spot.getId() + " via conveyor " + sel.conveyor.getId());
        }
        return result;
    }

    public static List<AllocationRecord> getActiveAllocations() {
        synchronized (activeAllocations) {
            return new ArrayList<>(activeAllocations);
        }
    }

    public static void markParked(int vehicleNumber) {
        synchronized (activeAllocations) {
            activeAllocations.replaceAll(r -> r.vehicleNumber == vehicleNumber
                    ? new AllocationRecord(r.vehicleNumber, r.conveyorId, r.spotId, r.floor, "Parked")
                    : r);
        }
        // reset conveyor to ACTIVE after completion
        AllocationRecord rec = null;
        synchronized (activeAllocations) {
            for (AllocationRecord r : activeAllocations) {
                if (r.vehicleNumber == vehicleNumber) { rec = r; break; }
            }
        }
        if (rec != null) {
            Conveyor c = DatabaseConnection.getConveyorById(rec.conveyorId);
            if (c != null) {
                c.setStatus(ConveyorStatus.ACTIVE);
                DatabaseConnection.updateConveyorStatus(c.getId(), ConveyorStatus.ACTIVE);
            }
        }
    }

    public static boolean isConveyorBusy(int conveyorId) {
        synchronized (activeAllocations) {
            return activeAllocations.stream().anyMatch(r -> r.conveyorId == conveyorId && "On conveyor".equalsIgnoreCase(r.status));
        }
    }

    private static class Selection {
        final Conveyor conveyor;
        final ParkingSpot spot;
        final double distance;
        Selection(Conveyor c, ParkingSpot s, double d) { this.conveyor = c; this.spot = s; this.distance = d; }
    }

    private static double distance(Conveyor c, ParkingSpot s) {
        double dx = c.getCurrentX() - s.getX();
        double dy = c.getCurrentY() - s.getY();
        // If floor differs, penalize to prefer same floor
        double floorPenalty = (c.getCurrentFloor() == s.getFloorNumber()) ? 0 : 1000;
        return Math.sqrt(dx*dx + dy*dy) + floorPenalty;
    }
}
