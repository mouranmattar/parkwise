package control;

import entity.*;
import utils.DatabaseConnection;
import java.util.List;
import java.util.concurrent.*;

public class ConveyorController {
    // 10-minute SLA for integrity checks
    private static final int CHECK_TIMEOUT_SECONDS = 600;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private ExecutorService executorService;

    public ConveyorController() {
        this.executorService = Executors.newCachedThreadPool();
    }

    public void activateAllConveyors(int lotID) {
        List<Conveyor> conveyors = DatabaseConnection.getConveyorsByLot(lotID);
        
        for (Conveyor conveyor : conveyors) {
            if (conveyor.getStatus() == ConveyorStatus.OFF && !conveyor.isWeightChangePending()) {
                activateSingleConveyor(conveyor);
            }
        }
    }

    public static class ActivationResult {
        public final boolean success;
        public final String reason;
        public final ConveyorStatus currentStatus;
        
        public ActivationResult(boolean success, String reason, ConveyorStatus currentStatus) {
            this.success = success;
            this.reason = reason;
            this.currentStatus = currentStatus;
        }
    }

    public ActivationResult activateConveyorById(int conveyorID) {
        Conveyor conveyor = DatabaseConnection.getConveyorById(conveyorID);
        if (conveyor == null) {
            return new ActivationResult(false, "Conveyor not found in database", null);
        }
        
        ConveyorStatus currentStatus = conveyor.getStatus();
        boolean weightPending = conveyor.isWeightChangePending();
        
        // Check conditions in order of priority
        if (currentStatus != ConveyorStatus.OFF) {
            return new ActivationResult(false, 
                "Status is not OFF (currently: " + currentStatus.name() + ")", 
                currentStatus);
        }
        
        if (weightPending) {
            return new ActivationResult(false, 
                "Weight change is pending - please confirm weight change first", 
                currentStatus);
        }
        
        if (AllocationService.isConveyorBusy(conveyorID)) {
            return new ActivationResult(false, 
                "Conveyor is currently busy with a vehicle", 
                currentStatus);
        }
        
        // All checks passed - activate
        activateSingleConveyor(conveyor);
        return new ActivationResult(true, "Successfully activated", ConveyorStatus.ACTIVE);
    }

    private void activateSingleConveyor(Conveyor conveyor) {
        conveyor.setStatus(ConveyorStatus.ACTIVE);
        DatabaseConnection.updateConveyorStatus(conveyor.getId(), ConveyorStatus.ACTIVE);
        
        // No background thread needed here as UI handles simulation now
    }

    public static class DeactivateResult {
        public final boolean success;
        public final String message;
        public DeactivateResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public DeactivateResult deactivateSingleConveyor(int conveyorID) {
        Conveyor conveyor = DatabaseConnection.getConveyorById(conveyorID);
        
        if (conveyor == null) {
            return new DeactivateResult(false, "Conveyor not found in database");
        }
        
        ConveyorStatus status = conveyor.getStatus();
        
        if (status == ConveyorStatus.PAUSED_BUSY || AllocationService.isConveyorBusy(conveyorID)) {
            return new DeactivateResult(false, "Cannot turn off: Conveyor is currently busy with a vehicle");
        }
        
        if (status == ConveyorStatus.OFF) {
            return new DeactivateResult(false, "Conveyor is already OFF");
        }
        
        conveyor.setStatus(ConveyorStatus.OFF);
        DatabaseConnection.updateConveyorStatus(conveyorID, ConveyorStatus.OFF);
        
        return new DeactivateResult(true, "Conveyor #" + conveyorID + " turned OFF successfully");
    }

    public void deactivateAllConveyors(int lotID) {
        List<Conveyor> conveyors = DatabaseConnection.getConveyorsByLot(lotID);
        
        for (Conveyor conveyor : conveyors) {
            if (conveyor.getStatus() != ConveyorStatus.PAUSED_BUSY && !control.AllocationService.isConveyorBusy(conveyor.getId())) {
                conveyor.setStatus(ConveyorStatus.OFF);
                DatabaseConnection.updateConveyorStatus(conveyor.getId(), ConveyorStatus.OFF);
            }
        }
    }

    public static class RestartResult {
        public final boolean success;
        public final String message;
        public RestartResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public RestartResult handleRestart(int conveyorID) {
        Conveyor conveyor = DatabaseConnection.getConveyorById(conveyorID);
        
        if (conveyor == null) {
            return new RestartResult(false, "Conveyor not found in database");
        }
        
        ConveyorStatus status = conveyor.getStatus();
        
        // Can restart from PAUSED_ERROR, UNDER_REPAIR, or PAUSED_BUSY (stuck)
        if (status == ConveyorStatus.PAUSED_ERROR || 
            status == ConveyorStatus.UNDER_REPAIR ||
            status == ConveyorStatus.PAUSED_BUSY) {
            
            conveyor.resetCheckAttempts();
            conveyor.setStatus(ConveyorStatus.CHECKING);
            DatabaseConnection.updateConveyorStatus(conveyorID, ConveyorStatus.CHECKING);
            
            executorService.submit(() -> {
                monitorIntegrityChecks(conveyorID);
            });
            
            return new RestartResult(true, "Conveyor #" + conveyorID + " restart initiated. Running integrity checks...");
        } else if (status == ConveyorStatus.ACTIVE) {
            return new RestartResult(false, "Conveyor is already ACTIVE. No restart needed.");
        } else if (status == ConveyorStatus.OFF) {
            return new RestartResult(false, "Conveyor is OFF. Use 'Turn On' instead of Restart.");
        } else if (status == ConveyorStatus.CHECKING) {
            return new RestartResult(false, "Conveyor is already running integrity checks.");
        } else {
            return new RestartResult(false, "Cannot restart from status: " + status.name());
        }
    }

    public static class WeightChangeResult {
        public final boolean success;
        public final String message;
        public final boolean pending;
        public WeightChangeResult(boolean success, String message, boolean pending) {
            this.success = success;
            this.message = message;
            this.pending = pending;
        }
    }

    public WeightChangeResult changeMaxWeight(int conveyorID, double weight) {
        Conveyor conveyor = DatabaseConnection.getConveyorById(conveyorID);
        
        if (conveyor == null) {
            return new WeightChangeResult(false, "Conveyor not found in database", false);
        }
        
        ConveyorStatus status = conveyor.getStatus();
        
        if (status != ConveyorStatus.OFF) {
            return new WeightChangeResult(false, 
                "Weight can only be changed when conveyor is OFF. Current status: " + status.name(), false);
        }
        
        if (weight <= 0) {
            return new WeightChangeResult(false, "Weight must be a positive value", false);
        }
        
        // Set pending flag in both object and database
        conveyor.setWeightChangePending(true);
        conveyor.setMaxWeight(weight);
        DatabaseConnection.updateConveyorWeightPending(conveyorID, true);
        
        return new WeightChangeResult(true, 
            "Weight change set to " + weight + "kg. Click 'Confirm Weight Change' to apply.", true);
    }

    public static class ConfirmResult {
        public final boolean success;
        public final String message;
        public ConfirmResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public ConfirmResult confirmWeightChange(int conveyorID) {
        Conveyor conveyor = DatabaseConnection.getConveyorById(conveyorID);
        
        if (conveyor == null) {
            return new ConfirmResult(false, "Conveyor not found in database");
        }
        
        if (!conveyor.isWeightChangePending()) {
            return new ConfirmResult(false, "No pending weight change to confirm.");
        }
        
        // Update max weight and clear pending flag in database
        DatabaseConnection.updateConveyorMaxWeight(conveyorID, conveyor.getMaxWeight());
        conveyor.setWeightChangePending(false);
        DatabaseConnection.updateConveyorWeightPending(conveyorID, false);
        
        return new ConfirmResult(true, "Weight change confirmed! New max weight: " + conveyor.getMaxWeight() + "kg. You can now turn on the conveyor.");
    }

    public void monitorIntegrityChecks(int conveyorID) {
        Conveyor conveyor = DatabaseConnection.getConveyorById(conveyorID);
        
        if (conveyor == null) {
            return;
        }

        ExecutorService checkExecutor = Executors.newFixedThreadPool(2);
        
        try {
            Future<Boolean> mechanicalFuture = checkExecutor.submit(() -> conveyor.runMechanicalCheck());
            Future<Boolean> electronicFuture = checkExecutor.submit(() -> conveyor.runElectronicCheck());
            
            boolean mechanicalSuccess = mechanicalFuture.get(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            boolean electronicSuccess = electronicFuture.get(CHECK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (mechanicalSuccess && electronicSuccess) {
                conveyor.setStatus(ConveyorStatus.ACTIVE);
                DatabaseConnection.updateConveyorStatus(conveyorID, ConveyorStatus.ACTIVE);
                conveyor.resetCheckAttempts();
            } else {
                handleCheckFailure(conveyor);
            }
            
        } catch (TimeoutException e) {
            conveyor.incrementFailedAttempts();
            
            if (conveyor.getFailedAttempts() >= MAX_RETRY_ATTEMPTS) {
                conveyor.setStatus(ConveyorStatus.PAUSED_ERROR);
                DatabaseConnection.updateConveyorStatus(conveyorID, ConveyorStatus.PAUSED_ERROR);
                sendAdminAlert(conveyorID, "Conveyor failed integrity checks after 3 attempts (timeout)");
            } else {
                conveyor.setStatus(ConveyorStatus.CHECKING);
                DatabaseConnection.updateConveyorStatus(conveyorID, ConveyorStatus.CHECKING);
                monitorIntegrityChecks(conveyorID);
            }
            
        } catch (Exception e) {
            handleCheckFailure(conveyor);
        } finally {
            checkExecutor.shutdownNow();
        }
    }

    private void handleCheckFailure(Conveyor conveyor) {
        conveyor.setStatus(ConveyorStatus.PAUSED_ERROR);
        DatabaseConnection.updateConveyorStatus(conveyor.getId(), ConveyorStatus.PAUSED_ERROR);
        sendAdminAlert(conveyor.getId(), "Conveyor integrity check failed");
    }

    public void performRepair(int conveyorID) {
        Conveyor conveyor = DatabaseConnection.getConveyorById(conveyorID);
        
        if (conveyor != null) {
            conveyor.setStatus(ConveyorStatus.UNDER_REPAIR);
            DatabaseConnection.updateConveyorStatus(conveyorID, ConveyorStatus.UNDER_REPAIR);
        }
    }

    public void sendAdminAlert(int conveyorID, String reason) {
        System.out.println("ALERT: Conveyor #" + conveyorID + " - " + reason);
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
