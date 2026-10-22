package utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import javax.swing.JOptionPane;
import entity.PriceList;
import entity.ParkingLot;
import entity.ParkingSession;
import entity.Vehicle;
import entity.ParkingSpot;
import entity.*;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnection {
    // Resolve DB path with fallback (current dir then parent dir)
    private static final String DB_FILE_NAME = "parkwise.accdb";
    private static Connection connection = null;
    private static String resolvedUrl = null;
    
    private static ConveyorStatus parseStatus(String statusStr) {
        if (statusStr == null) return ConveyorStatus.OFF;
        try {
            return ConveyorStatus.valueOf(statusStr.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            // map legacy/other labels to closest states
            String s = statusStr.trim().toUpperCase();
            if ("IDLE".equalsIgnoreCase(s)) return ConveyorStatus.ACTIVE;
            if ("OFF".equalsIgnoreCase(s)) return ConveyorStatus.OFF;
            if ("MOVING".equalsIgnoreCase(s)) return ConveyorStatus.PAUSED_BUSY;
            if ("PAUSED".equalsIgnoreCase(s)) return ConveyorStatus.PAUSED_BUSY;
            return ConveyorStatus.OFF;
        }
    }

    public static int[] getMonthlyClubJoins(int year) {
        int[] counts = new int[12];
        String q = "SELECT MONTH(membershipDate) AS m, COUNT(*) AS c FROM ClubMember WHERE membershipDate IS NOT NULL AND YEAR(membershipDate)=? GROUP BY MONTH(membershipDate)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, year);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int m = rs.getInt("m");
                int c = rs.getInt("c");
                if (m >= 1 && m <= 12) counts[m - 1] = c;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching monthly club joins: " + e.getMessage());
        }
        return counts;
    }

    public static int getClubMembersBeforeYear(int year) {
        String q = "SELECT COUNT(*) AS c FROM ClubMember WHERE membershipDate IS NOT NULL AND YEAR(membershipDate) < ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, year);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("c");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching club members before year: " + e.getMessage());
        }
        return 0;
    }

    public static String getAdminFullName(String username, String password) {
        String q = "SELECT full_name FROM Administrator WHERE username=? AND password=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("full_name");
            }
        } catch (SQLException e) {
            System.err.println("Error validating admin: " + e.getMessage());
        }
        return null;
    }

    public static List<String[]> getAdminSamples(int limit) {
        List<String[]> rows = new ArrayList<>();
        String q = "SELECT TOP " + Math.max(1, limit) + " username, password, full_name FROM Administrator";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{rs.getString("username"), rs.getString("password"), rs.getString("full_name")});
            }
        } catch (SQLException e) {
            System.err.println("Error fetching admin samples: " + e.getMessage());
        }
        return rows;
    }

    // ---- Allocation helpers ----

    public static Vehicle getVehicle(int vehicleNumber) {
        String query = "SELECT * FROM Vehicle WHERE VehicleNumber = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, vehicleNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Vehicle(
                        rs.getInt("VehicleNumber"),
                        rs.getString("type"),
                        rs.getString("color"),
                        rs.getInt("size"),
                        rs.getDouble("weight"),
                        rs.getString("photoFront"),
                        rs.getString("photoRear"),
                        rs.getString("photoSide"),
                        rs.getInt("ClientID")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching vehicle: " + e.getMessage());
        }
        return null;
    }

    public static List<ParkingSpot> getAvailableParkingSpaces(int lotId) {
        List<ParkingSpot> spots = new ArrayList<>();
        String query = "SELECT * FROM ParkingSpace WHERE ParkingLotID = ? AND (isOccupied = FALSE OR isOccupied IS NULL)";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, lotId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String sizeText = rs.getString("size");
                ParkingSpot spot = new ParkingSpot(
                        rs.getInt("ParkingSpaceID"),
                        rs.getInt("floorNumber"),
                        rs.getInt("xPosition"),
                        rs.getInt("yPosition"),
                        sizeText, // Pass String directly
                        rs.getBoolean("isOccupied"),
                        rs.getInt("ParkingLotID")
                );
                spots.add(spot);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching parking spaces: " + e.getMessage());
        }
        return spots;
    }

    public static void setParkingSpaceOccupied(int parkingSpaceId, boolean occupied) {
        String query = "UPDATE ParkingSpace SET isOccupied = ? WHERE ParkingSpaceID = ?";
        try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setBoolean(1, occupied);
            stmt.setInt(2, parkingSpaceId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating parking space occupancy: " + e.getMessage());
        }
    }

    private static int parseSize(String sizeText) {
        if (sizeText == null) return 2; // default medium
        String s = sizeText.trim().toLowerCase();
        if (s.matches("\\d+")) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        if (s.contains("small") || s.equals("s")) return 1;
        if (s.contains("large") || s.equals("l")) return 3;
        return 2; // medium
    }

    // ---- Club / Preferred Selection ----

    public static boolean isActiveClubMemberByClient(int clientId) {
        String q = "SELECT isActive FROM ClubMember WHERE ClientID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("isActive");
            }
        } catch (SQLException e) {
            System.err.println("Error checking club member: " + e.getMessage());
        }
        return false;
    }

    public static LocalDate getMembershipDate(int clientId) {
        String q = "SELECT membershipDate FROM ClubMember WHERE ClientID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                java.sql.Date d = rs.getDate("membershipDate");
                if (d != null) return d.toLocalDate();
            }
        } catch (SQLException e) {
            System.err.println("Error fetching membership date: " + e.getMessage());
        }
        return null;
    }

    public static ClubMember getClubMemberByClientId(int clientId) {
        String q = "SELECT * FROM ClubMember WHERE ClientID = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                java.sql.Date d = rs.getDate("membershipDate");
                LocalDate membershipDate = (d != null) ? d.toLocalDate() : null;
                return new ClubMember(
                    rs.getInt("ClientID"),
                    rs.getString("firstName"),
                    rs.getString("lastName"),
                    rs.getString("mobilePhone"),
                    membershipDate,
                    rs.getBoolean("isActive")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching club member: " + e.getMessage());
        }
        return null;
    }

    public static void upsertClubMember(int clientId, String first, String last, String phone, LocalDate joinDate, boolean active) {
        String select = "SELECT 1 FROM ClubMember WHERE ClientID=?";
        String insert = "INSERT INTO ClubMember (ClientID, firstName, lastName, mobilePhone, membershipDate, isActive) VALUES (?, ?, ?, ?, ?, ?)";
        String update = "UPDATE ClubMember SET firstName=?, lastName=?, mobilePhone=?, membershipDate=?, isActive=? WHERE ClientID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            if (exists) {
                try (PreparedStatement st = conn.prepareStatement(update)) {
                    st.setString(1, first);
                    st.setString(2, last);
                    st.setString(3, phone);
                    st.setDate(4, java.sql.Date.valueOf(joinDate));
                    st.setBoolean(5, active);
                    st.setInt(6, clientId);
                    st.executeUpdate();
                }
            } else {
                try (PreparedStatement st = conn.prepareStatement(insert)) {
                    st.setInt(1, clientId);
                    st.setString(2, first);
                    st.setString(3, last);
                    st.setString(4, phone);
                    st.setDate(5, java.sql.Date.valueOf(joinDate));
                    st.setBoolean(6, active);
                    st.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error upserting club member: " + e.getMessage());
        }
    }

    public static void deletePreferredSelectionsByClient(int clientId) {
        String q = "DELETE FROM PreferredSelection WHERE ClientID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, clientId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting preferred selections: " + e.getMessage());
        }
    }

    public static void insertPreferredSelection(int clientId, int lotId, LocalDate selectionDate) {
        String idQuery = "SELECT MAX(PreferredSelectionID) AS maxId FROM PreferredSelection";
        String insert = "INSERT INTO PreferredSelection (PreferredSelectionID, selectionDate, ParkingLotID, ClientID) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(idQuery)) {
            int next = 1;
            if (rs.next()) next = rs.getInt("maxId") + 1;
            try (PreparedStatement ps = conn.prepareStatement(insert)) {
                ps.setInt(1, next);
                ps.setDate(2, java.sql.Date.valueOf(selectionDate));
                ps.setInt(3, lotId);
                ps.setInt(4, clientId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error inserting preferred selection: " + e.getMessage());
        }
    }

    public static List<Integer> getPreferredLots(int clientId) {
        List<Integer> lots = new ArrayList<>();
        String q = "SELECT ParkingLotID FROM PreferredSelection WHERE ClientID=?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            ps.setInt(1, clientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lots.add(rs.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching preferred lots: " + e.getMessage());
        }
        return lots;
    }

    public static boolean hasUsedFreePreferredThisMonth(int clientId, int year, int month, List<Integer> preferredLots) {
        if (preferredLots == null || preferredLots.isEmpty()) return false;
        String placeholders = String.join(",", preferredLots.stream().map(x -> "?").toArray(String[]::new));
        String q = "SELECT TOP 1 ps.SessionID FROM ParkingSession ps " +
                "INNER JOIN Vehicle v ON ps.VehicleNumber = v.VehicleNumber " +
                "WHERE v.ClientID = ? AND YEAR(ps.startTime)=? AND MONTH(ps.startTime)=? " +
                "AND ps.ParkingLotID IN (" + placeholders + ") " +
                "AND (ps.status='Completed' OR ps.status='Paid')";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(q)) {
            int idx = 1;
            ps.setInt(idx++, clientId);
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            for (Integer lot : preferredLots) {
                ps.setInt(idx++, lot);
            }
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking free preferred usage: " + e.getMessage());
        }
        return false;
    }

    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                String url = resolveDbUrl();
                connection = DriverManager.getConnection(url);
            }
            return connection;
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                connection = null;
                resolvedUrl = null;
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    private static String resolveDbUrl() {
        if (resolvedUrl != null) return resolvedUrl;

        Path current = Paths.get(System.getProperty("user.dir"));
        Path primary = current.resolve(Paths.get("database", DB_FILE_NAME));
        Path fallback = current.getParent() == null ? null : current.getParent().resolve(Paths.get("database", DB_FILE_NAME));

        boolean primaryExists = Files.exists(primary);
        boolean fallbackExists = fallback != null && Files.exists(fallback);

        if (primaryExists) {
            resolvedUrl = "jdbc:ucanaccess://" + primary.toAbsolutePath();
            return resolvedUrl;
        }
        if (fallbackExists) {
            resolvedUrl = "jdbc:ucanaccess://" + fallback.toAbsolutePath();
            return resolvedUrl;
        }

        String msg = "Database file not found.\nTried:\n" +
                (primary != null ? primary.toAbsolutePath() : "(no primary)") + "\n" +
                (fallback != null ? fallback.toAbsolutePath() : "(no parent)") + "\n" +
                "Please place " + DB_FILE_NAME + " under a 'database' folder.";
        JOptionPane.showMessageDialog(null, msg, "Database Not Found", JOptionPane.ERROR_MESSAGE);
        throw new IllegalStateException(msg);
    }

    public static List<Conveyor> getConveyorsByLot(int lotID) {
        List<Conveyor> conveyors = new ArrayList<>();
        String query = "SELECT * FROM Conveyor WHERE ParkingLotID = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, lotID);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Conveyor conveyor = new Conveyor(
                    rs.getInt("ConveyorID"),
                    rs.getInt("ParkingLotID"),
                    rs.getInt("floorNumber"),
                    rs.getInt("xPosition"),
                    rs.getInt("yPosition"),
                    rs.getDouble("maxWeightCapacity")
                );
                conveyor.setStatus(parseStatus(rs.getString("status")));
                conveyor.setFailedCheckAttempts(rs.getInt("failedCheckAttempts"));
                conveyor.setWeightChangePending(rs.getBoolean("isWeightChangePending"));
                conveyors.add(conveyor);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching conveyors: " + e.getMessage());
        }
        
        return conveyors;
    }

    public static void updateConveyorStatus(int conveyorID, ConveyorStatus status) {
        String query = "UPDATE Conveyor SET status = ? WHERE ConveyorID = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, status.name());
            stmt.setInt(2, conveyorID);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error updating conveyor status: " + e.getMessage());
        }
    }

    public static void updateConveyorMaxWeight(int conveyorID, double maxWeight) {
        String query = "UPDATE Conveyor SET maxWeightCapacity = ? WHERE ConveyorID = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setDouble(1, maxWeight);
            stmt.setInt(2, conveyorID);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error updating conveyor max weight: " + e.getMessage());
        }
    }

    public static void updateConveyorWeightPending(int conveyorID, boolean pending) {
        String query = "UPDATE Conveyor SET isWeightChangePending = ? WHERE ConveyorID = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setBoolean(1, pending);
            stmt.setInt(2, conveyorID);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error updating conveyor weight pending flag: " + e.getMessage());
        }
    }

    public static Conveyor getConveyorById(int conveyorID) {
        String query = "SELECT * FROM Conveyor WHERE ConveyorID = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, conveyorID);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Conveyor conveyor = new Conveyor(
                    rs.getInt("ConveyorID"),
                    rs.getInt("ParkingLotID"),
                    rs.getInt("floorNumber"),
                    rs.getInt("xPosition"),
                    rs.getInt("yPosition"),
                    rs.getDouble("maxWeightCapacity")
                );
                conveyor.setStatus(parseStatus(rs.getString("status")));
                conveyor.setFailedCheckAttempts(rs.getInt("failedCheckAttempts"));
                conveyor.setWeightChangePending(rs.getBoolean("isWeightChangePending"));
                return conveyor;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching conveyor: " + e.getMessage());
        }
        
        return null;
    }

    public static List<PriceList> getPriceHistory(int lotID) {
        List<PriceList> priceHistory = new ArrayList<>();
        String query = "SELECT * FROM PriceList WHERE ParkingLotID = ? ORDER BY validFrom DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, lotID);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                PriceList priceList = new PriceList(
                    rs.getInt("PriceListID"),
                    rs.getInt("ParkingLotID"),
                    rs.getInt("plYear"),
                    rs.getDouble("firstHourPrice"),
                    rs.getDouble("additionalHourPrice"),
                    rs.getDouble("fullDayPrice"),
                    rs.getDate("validFrom")
                );
                priceHistory.add(priceList);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching price history: " + e.getMessage());
        }
        
        return priceHistory;
    }

    public static void insertPriceList(PriceList priceList) {
        String idQuery = "SELECT MAX(PriceListID) AS maxId FROM PriceList";
        String insertQuery = "INSERT INTO PriceList (PriceListID, ParkingLotID, plYear, firstHourPrice, additionalHourPrice, fullDayPrice, validFrom) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             Statement idStmt = conn.createStatement();
             ResultSet idRs = idStmt.executeQuery(idQuery);
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            int nextId = 1;
            if (idRs.next()) {
                nextId = idRs.getInt("maxId") + 1;
            }
            
            stmt.setInt(1, nextId);
            stmt.setInt(2, priceList.getParkingLotID());
            stmt.setInt(3, priceList.getYear());
            stmt.setDouble(4, priceList.getFirstHourPrice());
            stmt.setDouble(5, priceList.getAdditionalHourPrice());
            stmt.setDouble(6, priceList.getFullDayPrice());
            stmt.setDate(7, new java.sql.Date(priceList.getValidFrom().getTime()));
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Error inserting price list: " + e.getMessage());
        }
    }

    public static ParkingLot getParkingLot(int lotID) {
        String query = "SELECT * FROM ParkingLot WHERE ParkingLotID = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, lotID);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                ParkingLot lot = new ParkingLot(
                    rs.getInt("ParkingLotID"),
                    rs.getString("plName"),
                    rs.getString("address"),
                    rs.getString("city"),
                    rs.getInt("availableSpaces")
                );
                
                List<Conveyor> conveyors = getConveyorsByLot(lotID);
                for (Conveyor c : conveyors) {
                    lot.addConveyor(c);
                }
                
                List<PriceList> prices = getPriceHistory(lotID);
                for (PriceList p : prices) {
                    lot.addPriceList(p);
                }
                
                return lot;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching parking lot: " + e.getMessage());
        }
        
        return null;
    }

    public static List<ParkingLot> getAllParkingLots() {
        List<ParkingLot> lots = new ArrayList<>();
        String query = "SELECT * FROM ParkingLot";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                ParkingLot lot = new ParkingLot(
                    rs.getInt("ParkingLotID"),
                    rs.getString("plName"),
                    rs.getString("address"),
                    rs.getString("city"),
                    rs.getInt("availableSpaces")
                );
                lots.add(lot);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching parking lots: " + e.getMessage());
        }
        
        return lots;
    }

    public static void insertParkingLot(String name, String address, String city, int availableSpaces) {
        String idQuery = "SELECT MAX(ParkingLotID) AS maxId FROM ParkingLot";
        String insertQuery = "INSERT INTO ParkingLot (ParkingLotID, plName, address, city, availableSpaces) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             Statement idStmt = conn.createStatement();
             ResultSet idRs = idStmt.executeQuery(idQuery);
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            int nextId = 1;
            if (idRs.next()) {
                nextId = idRs.getInt("maxId") + 1;
            }

            stmt.setInt(1, nextId);
            stmt.setString(2, name);
            stmt.setString(3, address);
            stmt.setString(4, city);
            stmt.setInt(5, availableSpaces);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error inserting parking lot: " + e.getMessage());
        }
    }

    public static void updateParkingLot(int id, String name, String address, String city, int availableSpaces) {
        String query = "UPDATE ParkingLot SET plName = ?, address = ?, city = ?, availableSpaces = ? WHERE ParkingLotID = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, name);
            stmt.setString(2, address);
            stmt.setString(3, city);
            stmt.setInt(4, availableSpaces);
            stmt.setInt(5, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error updating parking lot: " + e.getMessage());
        }
    }

    public static void deleteParkingLot(int id) {
        String query = "DELETE FROM ParkingLot WHERE ParkingLotID = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Error deleting parking lot: " + e.getMessage());
        }
    }

    // --- ParkingSession & Receipt helpers ---

    public static ParkingSession getParkingSessionById(int sessionId) {
        String query = "SELECT * FROM ParkingSession WHERE SessionID = ?";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for getParkingSessionById");
            return null;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, sessionId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new ParkingSession(
                    rs.getInt("SessionID"),
                    rs.getTimestamp("startTime"),
                    rs.getTimestamp("endTime"),
                    rs.getDouble("finalAmount"),
                    rs.getString("status"),
                    rs.getInt("VehicleNumber"),
                    rs.getInt("ParkingLotID")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching parking session: " + e.getMessage());
        }
        return null;
    }

    // ---- Entry/Kiosk helpers ----

    public static boolean vehicleExists(int vehicleNumber) {
        String query = "SELECT 1 FROM Vehicle WHERE VehicleNumber = ?";
        Connection conn = getConnection();
        if (conn == null) return false;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, vehicleNumber);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking vehicle existence: " + e.getMessage());
            return false;
        }
    }

    public static Integer findClientIdByVehicle(int vehicleNumber) {
        String query = "SELECT ClientID FROM Vehicle WHERE VehicleNumber = ?";
        Connection conn = getConnection();
        if (conn == null) return null;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, vehicleNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("ClientID");
        } catch (SQLException e) {
            System.err.println("Error finding client by vehicle: " + e.getMessage());
        }
        return null;
    }

    public static Integer findClientIdByPhone(String phone) {
        String query = "SELECT ClientID FROM Client WHERE mobilePhone = ?";
        Connection conn = getConnection();
        if (conn == null) return null;
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, phone);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt("ClientID");
        } catch (SQLException e) {
            System.err.println("Error finding client by phone: " + e.getMessage());
        }
        return null;
    }

    public static int insertClient(String firstName, String lastName, String phone) {
        String idQuery = "SELECT MAX(ClientID) AS maxId FROM Client";
        String insert = "INSERT INTO Client (ClientID, firstName, lastName, mobilePhone) VALUES (?, ?, ?, ?)";
        Connection conn = getConnection();
        if (conn == null) throw new IllegalStateException("No DB connection for insertClient");
        try (Statement idStmt = conn.createStatement();
             ResultSet rs = idStmt.executeQuery(idQuery);
             PreparedStatement stmt = conn.prepareStatement(insert)) {
            int nextId = 1;
            if (rs.next()) {
                nextId = rs.getInt("maxId") + 1;
            }
            stmt.setInt(1, nextId);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setString(4, phone);
            stmt.executeUpdate();
            return nextId;
        } catch (SQLException e) {
            throw new IllegalStateException("Error inserting client: " + e.getMessage(), e);
        }
    }

    public static void upsertVehicle(int vehicleNumber, String type, String color, int size, double weight,
                                     int clientId, String photoFront, String photoRear, String photoSide) {
        String select = "SELECT VehicleNumber FROM Vehicle WHERE VehicleNumber = ?";
        String insert = "INSERT INTO Vehicle (VehicleNumber, type, color, size, weight, photoFront, photoRear, photoSide, ClientID) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String update = "UPDATE Vehicle SET type=?, color=?, size=?, weight=?, photoFront=?, photoRear=?, photoSide=?, ClientID=? WHERE VehicleNumber=?";
        Connection conn = getConnection();
        if (conn == null) throw new IllegalStateException("No DB connection for upsertVehicle");
        try (PreparedStatement sel = conn.prepareStatement(select)) {
            sel.setInt(1, vehicleNumber);
            ResultSet rs = sel.executeQuery();
            boolean exists = rs.next();
            if (exists) {
                try (PreparedStatement stmt = conn.prepareStatement(update)) {
                    stmt.setString(1, type);
                    stmt.setString(2, color);
                    stmt.setInt(3, size);
                    stmt.setDouble(4, weight);
                    stmt.setString(5, photoFront);
                    stmt.setString(6, photoRear);
                    stmt.setString(7, photoSide);
                    stmt.setInt(8, clientId);
                    stmt.setInt(9, vehicleNumber);
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                    stmt.setInt(1, vehicleNumber);
                    stmt.setString(2, type);
                    stmt.setString(3, color);
                    stmt.setInt(4, size);
                    stmt.setDouble(5, weight);
                    stmt.setString(6, photoFront);
                    stmt.setString(7, photoRear);
                    stmt.setString(8, photoSide);
                    stmt.setInt(9, clientId);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error upserting vehicle: " + e.getMessage(), e);
        }
    }

    public static int getDefaultParkingLotId() {
        String query = "SELECT TOP 1 ParkingLotID FROM ParkingLot ORDER BY ParkingLotID";
        Connection conn = getConnection();
        if (conn == null) throw new IllegalStateException("No DB connection for getDefaultParkingLotId");
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new IllegalStateException("Error fetching default parking lot: " + e.getMessage(), e);
        }
        return 1; // fallback
    }

    public static void startParkingSession(int vehicleNumber, int parkingLotId, Timestamp startTime, int parkingSpaceId) {
        String insert = "INSERT INTO ParkingSession (SessionID, VehicleNumber, startTime, status, ParkingLotID, ParkingSpaceID) VALUES (?, ?, ?, ?, ?, ?)";
        String idQuery = "SELECT MAX(SessionID) AS maxId FROM ParkingSession";
        Connection conn = getConnection();
        if (conn == null) throw new IllegalStateException("No DB connection for startParkingSession");
        try (Statement idStmt = conn.createStatement(); ResultSet rs = idStmt.executeQuery(idQuery)) {
            int nextId = 1;
            if (rs.next()) nextId = rs.getInt("maxId") + 1;
            try (PreparedStatement stmt = conn.prepareStatement(insert)) {
                stmt.setInt(1, nextId);
                stmt.setInt(2, vehicleNumber);
                stmt.setTimestamp(3, startTime);
                stmt.setString(4, "InProgress");
                stmt.setInt(5, parkingLotId);
                stmt.setInt(6, parkingSpaceId);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error starting parking session: " + e.getMessage(), e);
        }
    }

    public static PriceList getLatestPriceListForLot(int lotId, java.sql.Timestamp startTime) {
        String query = "SELECT TOP 1 * FROM PriceList WHERE ParkingLotID = ? AND validFrom <= ? ORDER BY validFrom DESC";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for getLatestPriceListForLot");
            return null;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, lotId);
            stmt.setTimestamp(2, startTime);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new PriceList(
                    rs.getInt("PriceListID"),
                    rs.getInt("ParkingLotID"),
                    rs.getInt("plYear"),
                    rs.getDouble("firstHourPrice"),
                    rs.getDouble("additionalHourPrice"),
                    rs.getDouble("fullDayPrice"),
                    rs.getTimestamp("validFrom")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching latest price list: " + e.getMessage());
        }
        return null;
    }

    public static void updateParkingSessionPayment(int sessionId, java.sql.Timestamp endTime, double finalAmount, String status) {
        String query = "UPDATE ParkingSession SET endTime = ?, finalAmount = ?, status = ? WHERE SessionID = ?";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for updateParkingSessionPayment");
            return;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setTimestamp(1, endTime);
            stmt.setDouble(2, finalAmount);
            stmt.setString(3, status);
            stmt.setInt(4, sessionId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating parking session payment: " + e.getMessage());
        }
    }

    public static void insertReceipt(java.sql.Timestamp issueDate, long overallHours, String appliedRate, double finalAmount, int sessionId) {
        String checkQuery = "SELECT COUNT(*) AS cnt FROM Receipt WHERE SessionID = ?";
        String insertQuery = "INSERT INTO Receipt (issueDate, overallHours, appliedRate, finalAmount, SessionID) VALUES (?, ?, ?, ?, ?)";
        String updateQuery = "UPDATE Receipt SET issueDate = ?, overallHours = ?, appliedRate = ?, finalAmount = ? WHERE SessionID = ?";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for insertReceipt");
            return;
        }
        try (PreparedStatement check = conn.prepareStatement(checkQuery)) {
            check.setInt(1, sessionId);
            ResultSet rs = check.executeQuery();
            boolean exists = false;
            if (rs.next()) {
                exists = rs.getInt("cnt") > 0;
            }
            if (exists) {
                try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                    stmt.setTimestamp(1, issueDate);
                    stmt.setLong(2, overallHours);
                    stmt.setString(3, appliedRate);
                    stmt.setDouble(4, finalAmount);
                    stmt.setInt(5, sessionId);
                    stmt.executeUpdate();
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                    stmt.setTimestamp(1, issueDate);
                    stmt.setLong(2, overallHours);
                    stmt.setString(3, appliedRate);
                    stmt.setDouble(4, finalAmount);
                    stmt.setInt(5, sessionId);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            System.err.println("Error inserting receipt: " + e.getMessage());
        }
    }

    public static List<ParkingSession> getActiveSessions() {
        List<ParkingSession> sessions = new ArrayList<>();
        String query = "SELECT SessionID, VehicleNumber, startTime, endTime, finalAmount, status, ParkingLotID " +
                       "FROM ParkingSession WHERE status IN ('InProgress', 'ACTIVE', 'Active')";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for getActiveSessions");
            return sessions;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sessions.add(new ParkingSession(
                        rs.getInt("SessionID"),
                        rs.getTimestamp("startTime"),
                        rs.getTimestamp("endTime"),
                        rs.getDouble("finalAmount"),
                        rs.getString("status"),
                        rs.getInt("VehicleNumber"),
                        rs.getInt("ParkingLotID")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching active sessions: " + e.getMessage());
        }
        return sessions;
    }

    public static ParkingSession getActiveSessionByVehicle(int vehicleNumber) {
        String query = "SELECT SessionID, VehicleNumber, startTime, endTime, finalAmount, status, ParkingLotID, ParkingSpaceID " +
                "FROM ParkingSession " +
                "WHERE VehicleNumber = ? AND status IN ('InProgress', 'ACTIVE', 'Active')";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for getActiveSessionByVehicle");
            return null;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, vehicleNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                ParkingSession session = new ParkingSession(
                        rs.getInt("SessionID"),
                        rs.getTimestamp("startTime"),
                        rs.getTimestamp("endTime"),
                        rs.getDouble("finalAmount"),
                        rs.getString("status"),
                        rs.getInt("VehicleNumber"),
                        rs.getInt("ParkingLotID")
                );
                session.setParkingSpaceID(rs.getInt("ParkingSpaceID"));
                return session;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching active session: " + e.getMessage());
        }
        return null;
    }

    public static boolean isActiveClubMemberByVehicle(int vehicleNumber) {
        String query = "SELECT c.isActive FROM Vehicle v " +
                "INNER JOIN ClubMember c ON v.ClientID = c.ClientID " +
                "WHERE v.VehicleNumber = ? AND c.isActive = TRUE";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for isActiveClubMemberByVehicle");
            return false;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, vehicleNumber);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking club member: " + e.getMessage());
            return false;
        }
    }

    public static String getClientPhoneByVehicle(int vehicleNumber) {
        String query = "SELECT c.mobilePhone FROM Client c " +
                "INNER JOIN Vehicle v ON c.ClientID = v.ClientID " +
                "WHERE v.VehicleNumber = ?";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for getClientPhoneByVehicle");
            return null;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, vehicleNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("mobilePhone");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching client phone by vehicle: " + e.getMessage());
        }
        return null;
    }

    public static List<ParkingSession> getSessionsByYear(int year) {
        List<ParkingSession> sessions = new ArrayList<>();
        String query = "SELECT SessionID, startTime, endTime, finalAmount, status, VehicleNumber, ParkingLotID " +
                "FROM ParkingSession " +
                "WHERE DatePart('yyyy', IIf(endTime IS NULL, startTime, endTime)) = ?";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for getSessionsByYear");
            return sessions;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, year);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                sessions.add(new ParkingSession(
                        rs.getInt("SessionID"),
                        rs.getTimestamp("startTime"),
                        rs.getTimestamp("endTime"),
                        rs.getDouble("finalAmount"),
                        rs.getString("status"),
                        rs.getInt("VehicleNumber"),
                        rs.getInt("ParkingLotID")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching sessions by year: " + e.getMessage());
        }
        return sessions;
    }

    public static List<Integer> getAvailableYears() {
        List<Integer> years = new ArrayList<>();
        String query = "SELECT DISTINCT DatePart('yyyy', IIf(endTime IS NULL, startTime, endTime)) AS yr " +
                       "FROM ParkingSession " +
                       "WHERE IIf(endTime IS NULL, startTime, endTime) IS NOT NULL " +
                       "ORDER BY yr DESC";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for getAvailableYears");
            return years;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                years.add(rs.getInt("yr"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching available years: " + e.getMessage());
        }
        return years;
    }

    public static ParkingLot getParkingLotById(int lotId) {
        String query = "SELECT ParkingLotID, plName, address, city, availableSpaces FROM ParkingLot WHERE ParkingLotID = ?";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for getParkingLotById");
            return null;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, lotId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new ParkingLot(
                        rs.getInt("ParkingLotID"),
                        rs.getString("plName"),
                        rs.getString("address"),
                        rs.getString("city"),
                        rs.getInt("availableSpaces")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching parking lot: " + e.getMessage());
        }
        return null;
    }

    public static String getVehicleColor(int vehicleNumber) {
        String query = "SELECT color FROM Vehicle WHERE VehicleNumber = ?";
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("No DB connection for getVehicleColor");
            return null;
        }
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, vehicleNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("color");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching vehicle color: " + e.getMessage());
        }
        return null;
    }
}
