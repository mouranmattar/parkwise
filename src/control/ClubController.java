package control;

import entity.ClubMember;
import entity.ParkingLot;
import utils.DatabaseConnection;

import java.time.LocalDate;
import java.util.List;

/**
 * ClubController - Controller for Club Membership operations
 * Handles business logic for client registration and preferred lot selection.
 * Part of BCE architecture (Controller layer).
 */
public class ClubController {

    /**
     * Result object for club registration operations.
     */
    public static class RegistrationResult {
        public final boolean success;
        public final String message;
        public final Integer clientId;
        public final int selectedLotsCount;

        public RegistrationResult(boolean success, String message, Integer clientId, int selectedLotsCount) {
            this.success = success;
            this.message = message;
            this.clientId = clientId;
            this.selectedLotsCount = selectedLotsCount;
        }

        public static RegistrationResult success(Integer clientId, int selectedLotsCount) {
            return new RegistrationResult(true, "Registration successful", clientId, selectedLotsCount);
        }

        public static RegistrationResult error(String message) {
            return new RegistrationResult(false, message, null, 0);
        }
    }

    /**
     * Register a client to the club with preferred lot selections.
     * 
     * @param firstName First name of the client
     * @param lastName Last name of the client
     * @paramPhone Phone number (required)
     * @param preferredLotIds List of selected parking lot IDs
     * @return RegistrationResult with success status and details
     */
    public RegistrationResult registerClubMember(String firstName, String lastName, String phone, List<Integer> preferredLotIds) {
        // Validation
        if (phone == null || phone.trim().isEmpty()) {
            return RegistrationResult.error("Phone number is required for membership");
        }

        // Default names if empty
        String f = (firstName == null || firstName.trim().isEmpty()) ? "Club" : firstName.trim();
        String l = (lastName == null || lastName.trim().isEmpty()) ? "Member" : lastName.trim();
        String p = phone.trim();

        try {
            // Find or create client
            Integer clientId = DatabaseConnection.findClientIdByPhone(p);
            if (clientId == null) {
                clientId = DatabaseConnection.insertClient(f, l, p);
                if (clientId == null || clientId <= 0) {
                    return RegistrationResult.error("Failed to create client record");
                }
            }

            // Check if already an active member
            if (DatabaseConnection.isActiveClubMemberByClient(clientId)) {
                return RegistrationResult.error("Client is already an active club member");
            }

            // Upsert club membership
            LocalDate today = LocalDate.now();
            DatabaseConnection.upsertClubMember(clientId, f, l, p, today, true);

            // Save preferred selections
            DatabaseConnection.deletePreferredSelectionsByClient(clientId);
            int selectedCount = 0;
            if (preferredLotIds != null && !preferredLotIds.isEmpty()) {
                for (Integer lotId : preferredLotIds) {
                    if (lotId != null && lotId > 0) {
                        DatabaseConnection.insertPreferredSelection(clientId, lotId, today);
                        selectedCount++;
                    }
                }
            }

            return RegistrationResult.success(clientId, selectedCount);

        } catch (Exception ex) {
            return RegistrationResult.error("Registration failed: " + ex.getMessage());
        }
    }

    /**
     * Get all available parking lots for selection.
     * 
     * @return List of all parking lots
     */
    public List<ParkingLot> getAllParkingLots() {
        return DatabaseConnection.getAllParkingLots();
    }

    /**
     * Check if a client is already a club member.
     * 
     * @param phone Phone number to check
     * @return true if active member, false otherwise
     */
    public boolean isActiveMember(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        Integer clientId = DatabaseConnection.findClientIdByPhone(phone.trim());
        if (clientId == null) {
            return false;
        }
        return DatabaseConnection.isActiveClubMemberByClient(clientId);
    }

    /**
     * Get membership details for a client.
     * 
     * @param clientId Client ID
     * @return ClubMember object or null if not found
     */
    public ClubMember getMembershipDetails(int clientId) {
        return DatabaseConnection.getClubMemberByClientId(clientId);
    }
}
