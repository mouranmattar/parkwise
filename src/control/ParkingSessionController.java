package control;

import entity.ParkingSession;
import entity.PriceList;
import utils.DatabaseConnection;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

public class ParkingSessionController {

    public static class PaymentResult {
        public final double amount;
        public final long billedHours;
        public final String appliedRate;
        public final String status;
        public final Timestamp startTime;
        public final Timestamp endTime;
        public final boolean alreadyPaid;
        public final boolean cancelled;
        public final int sessionId;
        public final int vehicleNumber;
        public final int parkingLotId;

        public PaymentResult(double amount, long billedHours, String appliedRate,
                             String status, Timestamp startTime, Timestamp endTime,
                             boolean alreadyPaid, boolean cancelled,
                             int sessionId, int vehicleNumber, int parkingLotId) {
            this.amount = amount;
            this.billedHours = billedHours;
            this.appliedRate = appliedRate;
            this.status = status;
            this.startTime = startTime;
            this.endTime = endTime;
            this.alreadyPaid = alreadyPaid;
            this.cancelled = cancelled;
            this.sessionId = sessionId;
            this.vehicleNumber = vehicleNumber;
            this.parkingLotId = parkingLotId;
        }
    }

    public PaymentResult endSessionAndPayByVehicle(int vehicleNumber, String phoneForReceipt) {
        ParkingSession session = DatabaseConnection.getActiveSessionByVehicle(vehicleNumber);
        if (session == null) {
            throw new IllegalArgumentException("Active session not found for vehicle: " + vehicleNumber);
        }

        // Validate phone matches client of this vehicle when provided
        if (phoneForReceipt != null && !phoneForReceipt.trim().isEmpty()) {
            String expectedPhone = DatabaseConnection.getClientPhoneByVehicle(vehicleNumber);
            if (expectedPhone != null && !expectedPhone.trim().isEmpty()) {
                String normalizedExpected = expectedPhone.replaceAll("\\s+", "");
                String normalizedProvided = phoneForReceipt.replaceAll("\\s+", "");
                if (!normalizedExpected.equals(normalizedProvided)) {
                    throw new IllegalArgumentException("Phone does not match client for this vehicle");
                }
            }
        }

        Timestamp endTime = session.getEndTime();
        if (endTime == null) {
            endTime = Timestamp.from(Instant.now());
        }

        String status = session.getStatus() == null ? "" : session.getStatus().trim();
        boolean isCancelled = status.equalsIgnoreCase("cancelled");
        boolean isCompleted = status.equalsIgnoreCase("completed") || status.equalsIgnoreCase("paid");

        // If already paid/completed, just return existing info without recalculation
        if (isCompleted && session.getFinalAmount() > 0 && session.getEndTime() != null) {
            long billedHours = computeBilledHours(session.getStartTime(), session.getEndTime());
            return new PaymentResult(
                    session.getFinalAmount(),
                    billedHours,
                    "previous payment",
                    status,
                    session.getStartTime(),
                    session.getEndTime(),
                    true,
                    false,
                    session.getSessionID(),
                    session.getVehicleNumber(),
                    session.getParkingLotID()
            );
        }

        PriceList priceList = DatabaseConnection.getLatestPriceListForLot(session.getParkingLotID(), session.getStartTime());
        if (priceList == null) {
            throw new IllegalStateException("No price list found for lot " + session.getParkingLotID());
        }

        // Club logic and free-first-preferred-in-join-month
        Integer clientId = DatabaseConnection.findClientIdByVehicle(session.getVehicleNumber());
        boolean clubMember = clientId != null && DatabaseConnection.isActiveClubMemberByClient(clientId);
        boolean freeApplied = false;
        double amountToCharge;
        String appliedRateLabel;

        if (clubMember && clientId != null) {
            java.time.LocalDate joinDate = DatabaseConnection.getMembershipDate(clientId);
            java.time.LocalDate billingDate = endTime.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            boolean sameMonth = joinDate != null && joinDate.getYear() == billingDate.getYear() && joinDate.getMonth() == billingDate.getMonth();
            if (sameMonth) {
                java.util.List<Integer> preferredLots = DatabaseConnection.getPreferredLots(clientId);
                boolean alreadyUsedFree = DatabaseConnection.hasUsedFreePreferredThisMonth(clientId, billingDate.getYear(), billingDate.getMonthValue(), preferredLots);
                boolean inPreferred = preferredLots != null && preferredLots.contains(session.getParkingLotID());
                if (inPreferred && !alreadyUsedFree) {
                    freeApplied = true;
                    amountToCharge = 0.0;
                    appliedRateLabel = "free preferred (join month)";
                } else {
                    PaymentResult base = computeAmount(session.getStartTime(), endTime, priceList,
                            session.getSessionID(), session.getVehicleNumber(), session.getParkingLotID());
                    double discounted = base.amount * 0.9; // 10% discount for club
                    amountToCharge = discounted;
                    appliedRateLabel = base.appliedRate + " + club 10% off";
                }
            } else {
                PaymentResult base = computeAmount(session.getStartTime(), endTime, priceList,
                        session.getSessionID(), session.getVehicleNumber(), session.getParkingLotID());
                double discounted = base.amount * 0.9;
                amountToCharge = discounted;
                appliedRateLabel = base.appliedRate + " + club 10% off";
            }
        } else {
            PaymentResult base = computeAmount(session.getStartTime(), endTime, priceList,
                    session.getSessionID(), session.getVehicleNumber(), session.getParkingLotID());
            amountToCharge = base.amount;
            appliedRateLabel = base.appliedRate;
        }

        // Prepare receipt fields
        long billedHours;
        if (clubMember && freeApplied) {
            billedHours = computeBilledHours(session.getStartTime(), endTime);
        } else {
            billedHours = computeBilledHours(session.getStartTime(), endTime);
        }

        // Simulate payment gateway before committing
        if (!simulatePaymentGateway(amountToCharge)) {
            throw new IllegalStateException("Payment gateway declined");
        }

        String targetStatus = isCancelled ? status : "Completed";
        DatabaseConnection.updateParkingSessionPayment(session.getSessionID(), endTime, amountToCharge, targetStatus);
        
        // Release the specific parking space used by this session
        if (session.getParkingSpaceID() > 0) {
            DatabaseConnection.setParkingSpaceOccupied(session.getParkingSpaceID(), false);
        }
        
        DatabaseConnection.insertReceipt(new Timestamp(System.currentTimeMillis()), billedHours, appliedRateLabel, amountToCharge, session.getSessionID());

        return new PaymentResult(
                amountToCharge,
                billedHours,
                appliedRateLabel,
                status,
                session.getStartTime(),
                endTime,
                false,
                isCancelled,
                session.getSessionID(),
                session.getVehicleNumber(),
                session.getParkingLotID()
        );
    }

    private PaymentResult computeAmount(Timestamp start, Timestamp end, PriceList priceList,
                                        int sessionId, int vehicleNumber, int parkingLotId) {
        long minutes = Duration.between(start.toInstant(), end.toInstant()).toMinutes();
        if (minutes < 0) minutes = 0;
        double hoursExact = minutes / 60.0;
        long billedHours = (long) Math.ceil(hoursExact);

        double amount;
        String applied;
        if (hoursExact <= 1.0) {
            amount = priceList.getFirstHourPrice();
            applied = "firstHourPrice";
            billedHours = 1;
        } else if (hoursExact < 24.0) {
            long additionalHours = (long) Math.ceil(hoursExact - 1.0);
            amount = priceList.getFirstHourPrice() + additionalHours * priceList.getAdditionalHourPrice();
            applied = "first + additional";
        } else {
            long days = (long) Math.ceil(hoursExact / 24.0);
            amount = days * priceList.getFullDayPrice();
            applied = "fullDay (days=" + days + ")";
        }
        return new PaymentResult(amount, billedHours, applied, "", start, end,
                false, false, sessionId, vehicleNumber, parkingLotId);
    }

    private long computeBilledHours(Timestamp start, Timestamp end) {
        long minutes = Duration.between(start.toInstant(), end.toInstant()).toMinutes();
        if (minutes < 0) minutes = 0;
        return (long) Math.ceil(minutes / 60.0);
    }

    private boolean simulatePaymentGateway(double amount) {
        System.out.println("Contacting External Payment Gateway... amount=" + amount);
        return true;
    }
}
