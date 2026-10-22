package control;

import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class DashboardController {

    public static class SpaceCell {
        public int slotId;
        public double x;
        public double y;
        public boolean occupied;
    }

    public static class DashboardStats {
        public int cars;
        public int totalSpaces;
        public int free;
        public int activeConveyors;
        public double income;
        public List<SpaceCell> spaces = new ArrayList<>();
    }

    public DashboardStats fetchDashboardStats(Integer lotId) {
        DashboardStats stats = new DashboardStats();
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                // 1. Cars Parked: Count isOccupied = true in ParkingSpace
                String carsQ = "SELECT COUNT(*) FROM ParkingSpace WHERE isOccupied = 1";
                // 2. Free Spaces: Count isOccupied = false in ParkingSpace
                String freeQ = "SELECT COUNT(*) FROM ParkingSpace WHERE isOccupied = 0";
                // 3. Active Conveyors: Count Idle, Moving, or ACTIVE/Active
                String convQ = "SELECT COUNT(*) FROM Conveyor WHERE status IN ('Idle', 'Moving', 'ACTIVE', 'Active', 'PAUSED_BUSY')";
                // 4. Total Income: SUM finalAmount from ParkingSession for Completed sessions
                String incomeQ = "SELECT IIF(SUM(finalAmount) IS NULL, 0, SUM(finalAmount)) FROM ParkingSession WHERE status = 'Completed'";
                // 5. Spaces for Map
                String listQ = "SELECT ParkingSpaceID, xPosition, yPosition, isOccupied FROM ParkingSpace";

                if (lotId != null && lotId != -1) {
                    carsQ += " AND ParkingLotID = " + lotId;
                    freeQ += " AND ParkingLotID = " + lotId;
                    convQ += " AND ParkingLotID = " + lotId;
                    incomeQ += " AND ParkingLotID = " + lotId;
                    listQ += " WHERE ParkingLotID = " + lotId;
                }

                stats.cars = queryCount(conn, carsQ);
                stats.totalSpaces = queryCount(conn, lotId != null && lotId != -1 ? "SELECT COUNT(*) FROM ParkingSpace WHERE ParkingLotID = " + lotId : "SELECT COUNT(*) FROM ParkingSpace");
                stats.free = queryCount(conn, freeQ);
                stats.activeConveyors = queryCount(conn, convQ);
                stats.income = querySum(conn, incomeQ);

                try (PreparedStatement ps = conn.prepareStatement(listQ)) {
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        SpaceCell sc = new SpaceCell();
                        sc.slotId = rs.getInt("ParkingSpaceID");
                        sc.x = rs.getDouble("xPosition");
                        sc.y = rs.getDouble("yPosition");
                        sc.occupied = rs.getBoolean("isOccupied");
                        stats.spaces.add(sc);
                    }
                }
            }
        } catch (Exception ignored) {}
        return stats;
    }

    private int queryCount(Connection conn, String sql) {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    private double querySum(Connection conn, String sql) {
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (Exception ignored) {}
        return 0;
    }
}
