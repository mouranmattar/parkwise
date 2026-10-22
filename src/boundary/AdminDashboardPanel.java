package boundary;

import control.AllocationService;
import control.ConveyorController;
import control.DashboardController;
import control.ParkingLotController;
import control.PricingController;
import entity.Conveyor;
import entity.ParkingLot;
import entity.PriceList;
import utils.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class AdminDashboardPanel extends JPanel {
    public interface AdminNav { 
        void goHome(); 
        void refreshAll();
    }

    private final String ADMIN_OVERVIEW = "overview";
    private final String ADMIN_PRICING = "pricing";
    private final String ADMIN_CONV = "conv";
    private final String ADMIN_LOTS = "lots";
    private final String ADMIN_REPORTS = "reports";

    private final CardLayout adminLayout = new CardLayout();
    private final JPanel adminContent = new JPanel(adminLayout);
    private String adminName = "Admin";
    private final DashboardController dashboardController = new DashboardController();
    private JLabel carsStat;
    private JLabel freeStat;
    private JLabel convStat;
    private JLabel incomeStat;
    private JLabel adminWelcome;
    private JPanel lotMapPanel;
    private MapPanel mapPanel;
    private final AdminNav nav;

    private JComboBox<String> dashboardLotSelector;

    public AdminDashboardPanel(AdminNav nav) {
        super(new BorderLayout());
        this.nav = nav;
        buildAdminDashboard();
        loadDashboardLots();
    }

    private void loadDashboardLots() {
        if (dashboardLotSelector == null) return;
        dashboardLotSelector.removeAllItems();
        dashboardLotSelector.addItem("-1 - Overview");
        List<ParkingLot> lots = DatabaseConnection.getAllParkingLots();
        for (ParkingLot lot : lots) {
            dashboardLotSelector.addItem(lot.getLotNumber() + " - " + lot.getName());
        }
    }

    private int getSelectedDashboardLotId() {
        if (dashboardLotSelector == null) return -1;
        String selected = (String) dashboardLotSelector.getSelectedItem();
        if (selected == null || selected.startsWith("-1")) return -1;
        try {
            return Integer.parseInt(selected.split(" - ")[0]);
        } catch (Exception e) {
            return -1;
        }
    }

    public void setAdminName(String name) {
        this.adminName = name;
        if (adminWelcome != null) {
            adminWelcome.setText("Welcome, " + adminName);
        }
    }

    private void buildAdminDashboard() {
        Color bg = new Color(244, 247, 246);
        Color sidebar = new Color(44, 62, 80);
        setBackground(bg);

        // Top navbar
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(new EmptyBorder(10, 16, 10, 16));
        JLabel title = new JLabel("Admin Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        CustomButton logout = new CustomButton("Logout", new Color(231, 76, 60));
        logout.setPreferredSize(new Dimension(100, 32));
        logout.addActionListener(e -> nav.goHome());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        adminWelcome = new JLabel("Welcome, " + adminName);
        right.add(adminWelcome);
        right.add(logout);
        top.add(title, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // Sidebar
        JPanel side = new JPanel();
        side.setBackground(sidebar);
        side.setLayout(new GridLayout(6, 1, 0, 6));
        side.setBorder(new EmptyBorder(16, 16, 16, 16));

        JButton overviewBtn = new JButton("🏠 Dashboard Overview");
        JButton lotsBtn = new JButton("🅿️ Parking Lots");
        JButton convBtn = new JButton("⚙️ Conveyors");
        JButton pricingBtn = new JButton("💲 Pricing");
        JButton reportsBtn = new JButton("📊 Reports & Finance");

        StylingHelper.applySidebarStyle(overviewBtn, sidebar, new Color(52, 152, 219), Color.WHITE);
        StylingHelper.applySidebarStyle(lotsBtn, sidebar, new Color(52, 152, 219), Color.WHITE);
        StylingHelper.applySidebarStyle(convBtn, sidebar, new Color(52, 152, 219), Color.WHITE);
        StylingHelper.applySidebarStyle(pricingBtn, sidebar, new Color(52, 152, 219), Color.WHITE);
        StylingHelper.applySidebarStyle(reportsBtn, sidebar, new Color(52, 152, 219), Color.WHITE);

        adminContent.setBackground(bg);
        adminContent.add(buildOverviewPanel(), ADMIN_OVERVIEW);
        adminContent.add(buildPricingPanel(), ADMIN_PRICING);
        adminContent.add(buildConveyorPanel(), ADMIN_CONV);
        adminContent.add(buildParkingLotsPanel(), ADMIN_LOTS);
        adminContent.add(buildReportsPanel(), ADMIN_REPORTS);

        overviewBtn.addActionListener(e -> switchAdminContent(ADMIN_OVERVIEW));
        lotsBtn.addActionListener(e -> switchAdminContent(ADMIN_LOTS));
        convBtn.addActionListener(e -> switchAdminContent(ADMIN_CONV));
        pricingBtn.addActionListener(e -> switchAdminContent(ADMIN_PRICING));
        reportsBtn.addActionListener(e -> switchAdminContent(ADMIN_REPORTS));

        side.add(overviewBtn);
        side.add(lotsBtn);
        side.add(convBtn);
        side.add(pricingBtn);
        side.add(reportsBtn);

        add(side, BorderLayout.WEST);
        add(adminContent, BorderLayout.CENTER);
    }

    private JPanel buildPricingPanel() {
        Color bg = new Color(244, 247, 246);
        PricingController controller = new PricingController();

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(bg);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setOpaque(false);
        top.add(new JLabel("Pricing • Live Lists"));
        JComboBox<String> lotComboBox = new JComboBox<>();
        lotComboBox.addActionListener(e -> onLotChangedPricing(controller, lotComboBox, panel));
        top.add(lotComboBox);

        JButton importBtn = new JButton("Import Price List (JSON)");
        importBtn.addActionListener(e -> onImportPriceList(controller, lotComboBox, panel));
        top.add(importBtn);

        panel.add(top, BorderLayout.NORTH);

        String[] columnNames = {"List ID", "Year", "First Hour ($)", "Additional Hour ($)", "Full Day ($)", "Valid From"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        applyTableStyle(table);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new LineBorder(new Color(220, 225, 230), 1, true));
        panel.add(scrollPane, BorderLayout.CENTER);

        loadParkingLots(lotComboBox);
        displayPriceHistory(controller, model, parseLotId(lotComboBox));

        return panel;
    }

    private void onLotChangedPricing(PricingController controller, JComboBox<String> lotComboBox, JPanel parent) {
        int lotId = parseLotId(lotComboBox);
        DefaultTableModel model = (DefaultTableModel) ((JTable)((JScrollPane)parent.getComponent(1)).getViewport().getView()).getModel();
        displayPriceHistory(controller, model, lotId);
    }

    private void onImportPriceList(PricingController controller, JComboBox<String> lotComboBox, JPanel parent) {
        int lotId = parseLotId(lotComboBox);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override public boolean accept(File f) { return f.isDirectory() || f.getName().toLowerCase().endsWith(".json"); }
            @Override public String getDescription() { return "JSON Files (*.json)"; }
        });
        int result = fileChooser.showOpenDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            controller.importPriceList(selectedFile.getAbsolutePath(), lotId);
            JOptionPane.showMessageDialog(parent, "Price list imported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            DefaultTableModel model = (DefaultTableModel) ((JTable)((JScrollPane)parent.getComponent(1)).getViewport().getView()).getModel();
            displayPriceHistory(controller, model, lotId);
        }
    }

    private JLabel buildStatCard(String title, String value, Color accent, String icon) {
        JLabel valueLabel = new JLabel(value, JLabel.LEFT);
        valueLabel.putClientProperty("accent", accent);
        valueLabel.putClientProperty("title", title);
        valueLabel.putClientProperty("icon", icon);
        return valueLabel;
    }

    private JPanel wrapStatCard(JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(225, 230, 235), 1, true), new EmptyBorder(12, 12, 12, 12)));
        String title = (String) valueLabel.getClientProperty("title");
        String icon = (String) valueLabel.getClientProperty("icon");
        Color accent = (Color) valueLabel.getClientProperty("accent");
        JLabel t = new JLabel(icon + "  " + title);
        t.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        t.setForeground(new Color(80, 92, 105));
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(accent);
        card.add(t, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void refreshAll() {
        loadDashboardLots();
        refreshDashboardData();
        // Since pricing and conveyors are in CardLayout, we might need more specific refresh if they are visible
        // But for now, let's at least refresh the active tab if possible
    }

    private void refreshDashboardData() {
        int lotId = getSelectedDashboardLotId();
        DashboardController.DashboardStats stats = dashboardController.fetchDashboardStats(lotId);

        if (carsStat != null) carsStat.setText(String.valueOf(stats.cars));
        if (freeStat != null) freeStat.setText(String.valueOf(stats.free));
        if (convStat != null) convStat.setText(String.valueOf(stats.activeConveyors));
        if (incomeStat != null) incomeStat.setText("$" + String.format("%.2f", stats.income));
        
        if (mapPanel != null) {
            if (lotId == -1) {
                mapPanel.setSpaces(new ArrayList<>());
                mapPanel.setOverview(true);
            } else {
                mapPanel.setSpaces(convertSpaces(stats.spaces));
                mapPanel.setOverview(false);
            }
            mapPanel.repaint();
        }
    }

    private List<SpaceCell> convertSpaces(List<DashboardController.SpaceCell> source) {
        List<SpaceCell> result = new ArrayList<>();
        if (source == null) return result;
        for (DashboardController.SpaceCell s : source) {
            SpaceCell cell = new SpaceCell();
            cell.slotId = s.slotId;
            cell.x = s.x;
            cell.y = s.y;
            cell.occupied = s.occupied;
            result.add(cell);
        }
        return result;
    }

    private void rebuildLotMap() {
        refreshDashboardData();
    }

    private void applyTableStyle(JTable table) {
        table.setRowHeight(35);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(209, 228, 255));
        table.setSelectionForeground(new Color(20, 30, 40));
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(236, 240, 241));
        header.setForeground(new Color(44, 62, 80));
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));
        header.setOpaque(true);
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(new Color(236, 240, 241));
                c.setForeground(new Color(44, 62, 80));
                setHorizontalAlignment(LEFT);
                return c;
            }
        });
    }

    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (!isSelected) {
                String v = value != null ? value.toString() : "";
                if ("ACTIVE".equalsIgnoreCase(v)) c.setForeground(new Color(39, 174, 96));
                else if ("OFF".equalsIgnoreCase(v) || "PAUSED".equalsIgnoreCase(v)) c.setForeground(new Color(231, 76, 60));
                else c.setForeground(new Color(60, 68, 77));
            }
            return c;
        }
    }

    private static class StylingHelper {
        static void applySidebarStyle(JButton btn, Color sidebarBg, Color hoverBg, Color fg) {
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setForeground(fg);
            btn.setBackground(sidebarBg);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setMargin(new Insets(8, 12, 8, 12));
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { btn.setOpaque(true); btn.setBackground(hoverBg); }
                @Override public void mouseExited(MouseEvent e) { btn.setOpaque(false); btn.setBackground(sidebarBg); }
            });
        }
    }

    private static class SpaceCell {
        int slotId;
        double x;
        double y;
        boolean occupied;
    }

    private static class MapPanel extends JPanel {
        private List<SpaceCell> spaces = new ArrayList<>();
        private boolean isOverview = true;
        private final int scale = 50;
        private final int size = 24;

        void setOverview(boolean overview) {
            this.isOverview = overview;
        }

        private List<Double> getSortedY() {
            java.util.Set<Double> ys = new java.util.TreeSet<>();
            for (SpaceCell s : spaces) {
                ys.add(Math.round(s.y * 100.0) / 100.0);
            }
            return new ArrayList<>(ys);
        }

        void setSpaces(List<SpaceCell> spaces) {
            this.spaces = spaces != null ? spaces : new ArrayList<>();
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            double maxX = 0, maxY = 0;
            List<Double> sortedY = getSortedY();
            for (SpaceCell s : spaces) {
                maxX = Math.max(maxX, s.x);
                if (!sortedY.isEmpty()) {
                    int row = sortedY.indexOf(Math.round(s.y * 100.0) / 100.0);
                    maxY = Math.max(maxY, row);
                }
            }
            return new Dimension((int) Math.ceil((maxX + 2) * scale), (int) Math.ceil((maxY + 2) * scale));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            g2.setColor(new Color(240, 243, 246));
            g2.fillRect(0, 0, getWidth(), getHeight());
            
            if (isOverview) {
                drawBrandedPlaceholder(g2);
                return;
            }

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            if (!spaces.isEmpty()) {
                List<Double> sortedY = getSortedY();
                double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = 0, maxY = 0;
                for (SpaceCell s : spaces) {
                    minX = Math.min(minX, s.x);
                    int row = sortedY.indexOf(Math.round(s.y * 100.0) / 100.0);
                    minY = Math.min(minY, row);
                    maxX = Math.max(maxX, s.x);
                    maxY = Math.max(maxY, row);
                }
                int areaW = (int) Math.ceil((maxX - minX + 1) * scale);
                int areaH = (int) Math.ceil((maxY - minY + 1) * scale);
                int offsetX = Math.max(20, (getWidth() - areaW) / 2);
                int offsetY = Math.max(20, (getHeight() - areaH) / 2);

                for (SpaceCell s : spaces) {
                    int px = offsetX + (int) Math.round((s.x - minX) * scale);
                    int row = sortedY.indexOf(Math.round(s.y * 100.0) / 100.0);
                    int py = offsetY + (int) Math.round((row - minY) * scale);
                    Color fill = s.occupied ? new Color(231, 76, 60) : new Color(46, 204, 113);
                    g2.setColor(fill);
                    g2.fillRect(px, py, size, size);
                    g2.setColor(new Color(40, 44, 52));
                    g2.drawRect(px, py, size, size);
                    String label = String.valueOf(s.slotId);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = px + (size - fm.stringWidth(label)) / 2;
                    int ty = py + ((size - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(label, tx, ty);
                }
            }
        }

        private void drawBrandedPlaceholder(Graphics2D g2) {
            int w = getWidth();
            int h = getHeight();
            
            // Draw a subtle pattern background
            g2.setColor(new Color(230, 235, 240));
            for (int i = 0; i < w; i += 40) {
                for (int j = 0; j < h; j += 40) {
                    g2.fillOval(i, j, 2, 2);
                }
            }

            // Draw Logo Icon (Mock)
            int centerX = w / 2;
            int centerY = h / 2 - 40;
            
            g2.setColor(new Color(52, 152, 219, 40));
            g2.fillOval(centerX - 60, centerY - 60, 120, 120);
            
            g2.setColor(new Color(52, 152, 219));
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(centerX - 40, centerY - 40, 80, 80);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 40));
            g2.drawString("P", centerX - 12, centerY + 15);

            // Branded Text
            g2.setColor(new Color(44, 62, 80));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
            String brand = "ParkWise Control Center";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(brand, (w - fm.stringWidth(brand)) / 2, centerY + 100);

            g2.setColor(new Color(127, 140, 141));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            String sub = "Select a specific parking lot to view live slot mapping";
            fm = g2.getFontMetrics();
            g2.drawString(sub, (w - fm.stringWidth(sub)) / 2, centerY + 135);
            
            // Decorative line
            g2.setColor(new Color(52, 152, 219, 100));
            g2.setStroke(new BasicStroke(1));
            g2.drawLine(centerX - 100, centerY + 115, centerX + 100, centerY + 115);
        }
    }

    private void displayPriceHistory(PricingController controller, DefaultTableModel model, int lotId) {
        List<PriceList> prices = controller.getPriceHistory(lotId);
        model.setRowCount(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (PriceList price : prices) {
            Object[] row = {
                    price.getListID(),
                    price.getYear(),
                    String.format("%.2f", price.getFirstHourPrice()),
                    String.format("%.2f", price.getAdditionalHourPrice()),
                    String.format("%.2f", price.getFullDayPrice()),
                    dateFormat.format(price.getValidFrom())
            };
            model.addRow(row);
        }
    }

    private void loadParkingLots(JComboBox<String> lotComboBox) {
        lotComboBox.removeAllItems();
        List<ParkingLot> lots = DatabaseConnection.getAllParkingLots();
        for (ParkingLot lot : lots) {
            lotComboBox.addItem(lot.getLotNumber() + " - " + lot.getName());
        }
    }

    private int parseLotId(JComboBox<String> lotComboBox) {
        String selected = (String) lotComboBox.getSelectedItem();
        if (selected == null || !selected.contains(" - ")) return 1;
        try { return Integer.parseInt(selected.split(" - ")[0]); } catch (NumberFormatException e) { return 1; }
    }

    private JPanel buildConveyorPanel() {
        Color bg = new Color(244, 247, 246);
        ConveyorController controller = new ConveyorController();

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(bg);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // Define table and model FIRST (before any ActionListener uses them)
        String[] columnNames = {"ID", "Floor", "Position (X,Y)", "Max Weight (kg)", "Status", "Failed Attempts"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        applyTableStyle(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setOpaque(false);
        top.add(new JLabel("Conveyors • Live Status"));
        JComboBox<String> lotComboBox = new JComboBox<>();
        lotComboBox.addActionListener(e -> onLotChangedConveyor(controller, lotComboBox, panel));
        top.add(lotComboBox);

        JButton turnOnBtn = new JButton("Turn On");
        turnOnBtn.addActionListener(e -> {
            int lotId = parseLotId(lotComboBox);
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int conveyorId = (int) model.getValueAt(selectedRow, 0);
                showIntegrityCheckDialog(lotId, controller, lotComboBox, panel, conveyorId);
            } else {
                showIntegrityCheckDialog(lotId, controller, lotComboBox, panel, -1);
            }
        });
        top.add(turnOnBtn);

        JButton turnOffBtn = new JButton("Turn Off");
        turnOffBtn.addActionListener(e -> onTurnOffConveyor(controller, table, model, lotComboBox, panel));
        top.add(turnOffBtn);

        JButton turnAllOffBtn = new JButton("Turn All Off");
        turnAllOffBtn.addActionListener(e -> onTurnAllOff(controller, lotComboBox, panel));
        top.add(turnAllOffBtn);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshConveyorList(controller, lotComboBox, panel));
        top.add(refreshBtn);

        panel.add(top, BorderLayout.NORTH);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new LineBorder(new Color(220, 225, 230), 1, true));

        String[] allocCols = {"Vehicle", "Conveyor", "Spot", "Floor", "Status"};
        DefaultTableModel allocModel = new DefaultTableModel(allocCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable allocTable = new JTable(allocModel);
        applyTableStyle(allocTable);
        JScrollPane allocScroll = new JScrollPane(allocTable);
        allocScroll.setBorder(new LineBorder(new Color(220, 225, 230), 1, true));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, allocScroll);
        split.setResizeWeight(0.6);

        panel.add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton restartBtn = new JButton("Restart Conveyor");
        restartBtn.addActionListener(e -> onRestartConveyor(controller, table, model, lotComboBox, panel));
        bottom.add(restartBtn);

        JButton updateWeightBtn = new JButton("Update Max Weight");
        updateWeightBtn.addActionListener(e -> onUpdateMaxWeight(controller, table, model));
        bottom.add(updateWeightBtn);

        JButton confirmWeightBtn = new JButton("Confirm Weight Change");
        confirmWeightBtn.addActionListener(e -> onConfirmWeightChange(controller, table, model, lotComboBox, panel));
        bottom.add(confirmWeightBtn);

        JButton refreshAllocBtn = new JButton("Refresh Allocations");
        refreshAllocBtn.addActionListener(e -> refreshAllocations(allocModel));
        bottom.add(refreshAllocBtn);

        JButton markParkedBtn = new JButton("Mark Parked");
        markParkedBtn.addActionListener(e -> onMarkParked(allocTable, allocModel, controller, lotComboBox, panel));
        bottom.add(markParkedBtn);

        panel.add(bottom, BorderLayout.SOUTH);

        table.getColumnModel().getColumn(4).setCellRenderer(new StatusCellRenderer());

        loadParkingLots(lotComboBox);
        refreshConveyorList(controller, lotComboBox, panel);
        refreshAllocations(allocModel);

        // Auto-refresh every 3 seconds to show conveyor status changes
        javax.swing.Timer autoRefresh = new javax.swing.Timer(3000, e -> {
            refreshConveyorList(controller, lotComboBox, panel);
            refreshAllocations(allocModel);
        });
        autoRefresh.start();

        return panel;
    }

    private void showIntegrityCheckDialog(int lotId, ConveyorController controller, JComboBox<String> lotComboBox, JPanel parent, int conveyorId) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "System Integrity Check", true);
        dlg.setLayout(new BorderLayout(15, 15));
        dlg.setSize(400, 180);
        dlg.setLocationRelativeTo(parent);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));
        content.setBackground(Color.WHITE);

        JLabel statusLabel = new JLabel("[System] Starting Integrity Check...", JLabel.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JProgressBar progress = new JProgressBar(0, 100);
        progress.setStringPainted(true);
        progress.setPreferredSize(new Dimension(300, 25));
        progress.setForeground(new Color(52, 152, 219));

        content.add(statusLabel, BorderLayout.NORTH);
        content.add(progress, BorderLayout.CENTER);
        dlg.add(content, BorderLayout.CENTER);

        Timer timer = new Timer(100, null);
        final int[] val = {0};
        final int targetConveyorId = conveyorId;
        
        timer.addActionListener(e -> {
            val[0]++;
            progress.setValue(val[0]);

            if (val[0] <= 40) {
                statusLabel.setText("[Stage 1] Checking Mechanical Tension & Rollers...");
            } else if (val[0] <= 70) {
                statusLabel.setText("[Stage 2] Verifying Sensors Integrity...");
                progress.setForeground(new Color(155, 89, 182));
            } else if (val[0] <= 90) {
                statusLabel.setText("[Stage 3] Syncing with Central Controller...");
                progress.setForeground(new Color(46, 204, 113));
            } else if (val[0] < 100) {
                statusLabel.setText("[Finalizing] Preparing Protocols...");
            }

            if (val[0] >= 100) {
                timer.stop();
                dlg.dispose();
                
                // Decision moment
                boolean success = Math.random() < 0.90;
                if (success) {
                    if (targetConveyorId > 0) {
                        control.ConveyorController.ActivationResult result = controller.activateConveyorById(targetConveyorId);
                        if (result.success) {
                            JOptionPane.showMessageDialog(parent, 
                                "Conveyor #" + targetConveyorId + " is now ACTIVE", 
                                "System Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            // Create a beautiful detailed error panel
                            JPanel errorPanel = new JPanel(new BorderLayout(10, 10));
                            errorPanel.setBackground(new Color(254, 242, 242));
                            errorPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
                            
                            JLabel titleLabel = new JLabel("⚠️ Activation Failed", JLabel.LEFT);
                            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
                            titleLabel.setForeground(new Color(185, 28, 28));
                            
                            JLabel conveyorLabel = new JLabel("Conveyor #" + targetConveyorId, JLabel.LEFT);
                            conveyorLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
                            conveyorLabel.setForeground(new Color(107, 114, 128));
                            
                            JPanel detailsPanel = new JPanel();
                            detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
                            detailsPanel.setBackground(new Color(254, 242, 242));
                            detailsPanel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(252, 165, 165), 1, true),
                                BorderFactory.createEmptyBorder(10, 10, 10, 10)
                            ));
                            
                            JLabel reasonTitle = new JLabel("Detected Issue:");
                            reasonTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
                            reasonTitle.setForeground(new Color(153, 27, 27));
                            
                            JLabel reasonLabel = new JLabel(result.reason);
                            reasonLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                            reasonLabel.setForeground(new Color(127, 29, 29));
                            
                            detailsPanel.add(reasonTitle);
                            detailsPanel.add(Box.createVerticalStrut(5));
                            detailsPanel.add(reasonLabel);
                            
                            if (result.currentStatus != null) {
                                JLabel currentStatusLabel = new JLabel("Current Status: " + result.currentStatus.name());
                                currentStatusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
                                currentStatusLabel.setForeground(new Color(156, 163, 175));
                                detailsPanel.add(Box.createVerticalStrut(8));
                                detailsPanel.add(currentStatusLabel);
                            }
                            
                            JPanel topPanel = new JPanel(new GridLayout(2, 1, 5, 5));
                            topPanel.setOpaque(false);
                            topPanel.add(titleLabel);
                            topPanel.add(conveyorLabel);
                            
                            errorPanel.add(topPanel, BorderLayout.NORTH);
                            errorPanel.add(detailsPanel, BorderLayout.CENTER);
                            
                            JOptionPane.showMessageDialog(parent, errorPanel, 
                                "Activation Blocked", JOptionPane.WARNING_MESSAGE);
                        }
                    } else {
                        controller.activateAllConveyors(lotId);
                        JOptionPane.showMessageDialog(parent, "All conveyors are now ACTIVE", "System Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                    refreshConveyorList(controller, lotComboBox, parent);
                } else {
                    // Force failure logic (Mocking a failure state)
                    if (targetConveyorId > 0) {
                        utils.DatabaseConnection.updateConveyorStatus(targetConveyorId, entity.ConveyorStatus.PAUSED_ERROR);
                        JOptionPane.showMessageDialog(parent, "Critical Error: Integrity Check Failed for Conveyor #" + targetConveyorId, "System Failure", JOptionPane.ERROR_MESSAGE);
                    } else {
                        List<entity.Conveyor> conveyors = utils.DatabaseConnection.getConveyorsByLot(lotId);
                        for (entity.Conveyor c : conveyors) {
                            utils.DatabaseConnection.updateConveyorStatus(c.getId(), entity.ConveyorStatus.PAUSED_ERROR);
                        }
                        JOptionPane.showMessageDialog(parent, "Critical Error: Integrity Check Failed", "System Failure", JOptionPane.ERROR_MESSAGE);
                    }
                    refreshConveyorList(controller, lotComboBox, parent);
                }
            }
        });

        timer.start();
        dlg.setVisible(true);
    }

    private void onLotChangedConveyor(ConveyorController controller, JComboBox<String> lotComboBox, JPanel parent) {
        refreshConveyorList(controller, lotComboBox, parent);
    }

    private void refreshConveyorList(ConveyorController controller, JComboBox<String> lotComboBox, JPanel parent) {
        int lotId = parseLotId(lotComboBox);
        List<Conveyor> conveyors = DatabaseConnection.getConveyorsByLot(lotId);
        JScrollPane scrollPane = (JScrollPane) ((JSplitPane) parent.getComponent(1)).getLeftComponent();
        JTable table = (JTable) scrollPane.getViewport().getView();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (Conveyor conveyor : conveyors) {
            Object[] row = {
                    conveyor.getId(),
                    conveyor.getCurrentFloor(),
                    "(" + conveyor.getCurrentX() + "," + conveyor.getCurrentY() + ")",
                    conveyor.getMaxWeight(),
                    conveyor.getStatus().name(),
                    conveyor.getFailedAttempts()
            };
            model.addRow(row);
        }
        refreshAllocations((DefaultTableModel)((JTable)((JScrollPane)((JSplitPane) parent.getComponent(1)).getRightComponent()).getViewport().getView()).getModel());
    }

    private void refreshAllocations(DefaultTableModel allocModel) {
        allocModel.setRowCount(0);
        for (AllocationService.AllocationRecord r : AllocationService.getActiveAllocations()) {
            allocModel.addRow(new Object[]{r.vehicleNumber, r.conveyorId, r.spotId, r.floor, r.status});
        }
    }

    private void onRestartConveyor(ConveyorController controller, JTable table, DefaultTableModel model, JComboBox<String> lotComboBox, JPanel parent) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(parent, "Please select a conveyor from the table first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int conveyorID = (int) model.getValueAt(selectedRow, 0);
        ConveyorController.RestartResult result = controller.handleRestart(conveyorID);
        
        if (result.success) {
            JOptionPane.showMessageDialog(parent, result.message, "Restart Initiated", JOptionPane.INFORMATION_MESSAGE);
            refreshConveyorList(controller, lotComboBox, parent);
        } else {
            JOptionPane.showMessageDialog(parent, result.message, "Cannot Restart", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onUpdateMaxWeight(ConveyorController controller, JTable table, DefaultTableModel model) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a conveyor from the table first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int conveyorID = (int) model.getValueAt(selectedRow, 0);
        String currentStatus = (String) model.getValueAt(selectedRow, 4);
        
        // Pre-check: warn user if not OFF
        if (!"OFF".equals(currentStatus)) {
            JOptionPane.showMessageDialog(this, 
                "Weight can only be changed when conveyor is OFF.\nCurrent status: " + currentStatus + "\n\nPlease turn off the conveyor first.", 
                "Status Check Failed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String input = JOptionPane.showInputDialog(this, "Enter new max weight (kg):", "Update Max Weight", JOptionPane.QUESTION_MESSAGE);
        if (input == null || input.trim().isEmpty()) return;
        
        try {
            double newWeight = Double.parseDouble(input);
            ConveyorController.WeightChangeResult result = controller.changeMaxWeight(conveyorID, newWeight);
            
            if (result.success) {
                JOptionPane.showMessageDialog(this, result.message, "Weight Change Pending", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, result.message, "Update Failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid weight value. Please enter a number.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onConfirmWeightChange(ConveyorController controller, JTable table, DefaultTableModel model, JComboBox<String> lotComboBox, JPanel parent) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a conveyor from the table first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int conveyorID = (int) model.getValueAt(selectedRow, 0);
        ConveyorController.ConfirmResult result = controller.confirmWeightChange(conveyorID);
        
        if (result.success) {
            JOptionPane.showMessageDialog(this, result.message, "Weight Confirmed", JOptionPane.INFORMATION_MESSAGE);
            refreshConveyorList(controller, lotComboBox, parent);
        } else {
            JOptionPane.showMessageDialog(this, result.message, "Confirmation Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onMarkParked(JTable allocTable, DefaultTableModel allocModel, ConveyorController controller, JComboBox<String> lotComboBox, JPanel parent) {
        int row = allocTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a vehicle from the allocations table.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String status = (String) allocModel.getValueAt(row, 4);
        if ("Parked".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "This vehicle is already marked as parked.", "Already Parked", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int vehicle = (int) allocModel.getValueAt(row, 0);
        AllocationService.markParked(vehicle);
        refreshAllocations(allocModel);
        refreshConveyorList(controller, lotComboBox, parent);
        JOptionPane.showMessageDialog(this, "Vehicle #" + vehicle + " marked as parked. Conveyor is now free.", "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onTurnOffConveyor(ConveyorController controller, JTable table, DefaultTableModel model, JComboBox<String> lotComboBox, JPanel parent) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(parent, "Please select a conveyor from the table first.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int conveyorID = (int) model.getValueAt(selectedRow, 0);
        ConveyorController.DeactivateResult result = controller.deactivateSingleConveyor(conveyorID);
        
        if (result.success) {
            JOptionPane.showMessageDialog(parent, result.message, "Turned Off", JOptionPane.INFORMATION_MESSAGE);
            refreshConveyorList(controller, lotComboBox, parent);
        } else {
            JOptionPane.showMessageDialog(parent, result.message, "Cannot Turn Off", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void onTurnAllOff(ConveyorController controller, JComboBox<String> lotComboBox, JPanel parent) {
        int lotId = parseLotId(lotComboBox);
        int confirm = JOptionPane.showConfirmDialog(parent, 
            "Are you sure you want to turn OFF all conveyors in this lot?", 
            "Confirm Turn All Off", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            controller.deactivateAllConveyors(lotId);
            refreshConveyorList(controller, lotComboBox, parent);
            JOptionPane.showMessageDialog(parent, "All conveyors turned OFF successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JPanel buildOverviewPanel() {
        Color bg = new Color(244, 247, 246);
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg);

        JPanel cards = new JPanel(new GridLayout(1, 4, 12, 12));
        cards.setOpaque(false);
        cards.setBorder(new EmptyBorder(16, 16, 8, 16));

        carsStat = buildStatCard("Cars Parked", "0", new Color(52, 152, 219), "🚗");
        freeStat = buildStatCard("Free Spaces", "0", new Color(46, 204, 113), "🅿️");
        convStat = buildStatCard("Active Conveyors", "0", new Color(155, 89, 182), "⚙️");
        incomeStat = buildStatCard("Total Income", "$0", new Color(241, 196, 15), "💳");

        cards.add(wrapStatCard(carsStat));
        cards.add(wrapStatCard(freeStat));
        cards.add(wrapStatCard(convStat));
        cards.add(wrapStatCard(incomeStat));

        p.add(cards, BorderLayout.NORTH);

        lotMapPanel = new JPanel(new BorderLayout());
        lotMapPanel.setOpaque(false);
        lotMapPanel.setBorder(new EmptyBorder(8, 16, 16, 16));

        JPanel mapHeader = new JPanel();
        mapHeader.setOpaque(false);
        mapHeader.setLayout(new BorderLayout());
        mapHeader.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel titleArea = new JPanel();
        titleArea.setOpaque(false);
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));
        JLabel mapTitle = new JLabel("Live Lot Map");
        mapTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        JLabel mapHint = new JLabel("Visual representation of physical spots");
        mapHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        mapHint.setForeground(new Color(99, 110, 123));
        titleArea.add(mapTitle);
        titleArea.add(mapHint);

        JPanel selectorArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        selectorArea.setOpaque(false);
        selectorArea.add(new JLabel("View:"));
        dashboardLotSelector = new JComboBox<>();
        dashboardLotSelector.setPreferredSize(new Dimension(180, 28));
        dashboardLotSelector.addActionListener(e -> refreshDashboardData());
        selectorArea.add(dashboardLotSelector);

        mapHeader.add(titleArea, BorderLayout.WEST);
        mapHeader.add(selectorArea, BorderLayout.EAST);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        legend.setOpaque(false);
        legend.add(buildLegendItem(new Color(231, 76, 60), "Occupied"));
        legend.add(buildLegendItem(new Color(46, 204, 113), "Free"));
        
        JPanel headerFull = new JPanel(new BorderLayout());
        headerFull.setOpaque(false);
        headerFull.add(mapHeader, BorderLayout.NORTH);
        headerFull.add(legend, BorderLayout.SOUTH);
        
        lotMapPanel.add(headerFull, BorderLayout.NORTH);

        mapPanel = new MapPanel();
        mapPanel.setBackground(new Color(247, 249, 252));
        JScrollPane mapScroll = new JScrollPane(mapPanel);
        mapScroll.setBorder(new LineBorder(new Color(225, 230, 235), 1, true));
        mapScroll.getViewport().setBackground(new Color(247, 249, 252));
        lotMapPanel.add(mapScroll, BorderLayout.CENTER);

        p.add(lotMapPanel, BorderLayout.CENTER);
        refreshDashboardData();
        return p;
    }

    private JPanel buildLegendItem(Color color, String label) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        p.setOpaque(false);
        JLabel swatch = new JLabel("  ");
        swatch.setOpaque(true);
        swatch.setBackground(color);
        swatch.setPreferredSize(new Dimension(20, 12));
        swatch.setBorder(new LineBorder(color.darker(), 1, true));
        JLabel txt = new JLabel(label);
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txt.setForeground(new Color(60, 68, 77));
        p.add(swatch);
        p.add(txt);
        return p;
    }

    private JPanel buildParkingLotsPanel() {
        Color bg = new Color(244, 247, 246);
        ParkingLotController controller = new ParkingLotController();

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(bg);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setOpaque(false);
        JTextField nameField = new JTextField(12);
        JTextField addressField = new JTextField(15);
        JTextField cityField = new JTextField(12);
        JTextField spacesField = new JTextField(8);
        CustomButton refreshBtn = new CustomButton("Refresh", new Color(52, 152, 219));
        CustomButton addBtn = new CustomButton("Add", new Color(39, 174, 96));
        CustomButton updateBtn = new CustomButton("Update Selected", new Color(241, 196, 15));
        CustomButton deleteBtn = new CustomButton("Delete Selected", new Color(231, 76, 60));
        top.add(new JLabel("Name:"));
        top.add(nameField);
        top.add(new JLabel("Address:"));
        top.add(addressField);
        top.add(new JLabel("City:"));
        top.add(cityField);
        top.add(new JLabel("Spaces:"));
        top.add(spacesField);
        top.add(refreshBtn);
        top.add(addBtn);
        top.add(updateBtn);
        top.add(deleteBtn);
        panel.add(top, BorderLayout.NORTH);

        String[] columns = {"Lot ID", "Name", "Address", "City", "Spaces"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        applyTableStyle(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(new Color(220, 225, 230), 1, true));
        panel.add(scroll, BorderLayout.CENTER);

        Runnable loadLots = () -> {
            model.setRowCount(0);
            List<ParkingLot> lots = controller.getAllLots();
            for (ParkingLot lot : lots) {
                model.addRow(new Object[]{lot.getLotNumber(), lot.getName(), lot.getAddress(), lot.getCity(), lot.getAvailableSpaces()});
            }
        };

        refreshBtn.addActionListener(e -> loadLots.run());
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String address = addressField.getText().trim();
            String city = cityField.getText().trim();
            String spacesText = spacesField.getText().trim();
            if (name.isEmpty() || address.isEmpty() || city.isEmpty() || spacesText.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Name, Address, City, and Spaces are required", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int spaces = Integer.parseInt(spacesText);
            controller.addLot(name, address, city, spaces);
            nameField.setText(""); addressField.setText(""); cityField.setText(""); spacesField.setText("");
            loadLots.run();
        });
        updateBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(panel, "Select a row to update", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String name = nameField.getText().trim();
            String address = addressField.getText().trim();
            String city = cityField.getText().trim();
            String spacesText = spacesField.getText().trim();
            if (name.isEmpty() || address.isEmpty() || city.isEmpty() || spacesText.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Name, Address, City, and Spaces are required", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int spaces = Integer.parseInt(spacesText);
            int id = Integer.parseInt(model.getValueAt(row, 0).toString());
            controller.updateLot(id, name, address, city, spaces);
            loadLots.run();
        });
        deleteBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(panel, "Select a row to delete", "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(panel, "Are you sure to delete?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            int id = Integer.parseInt(model.getValueAt(row, 0).toString());
            controller.deleteLot(id);
            loadLots.run();
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    nameField.setText(model.getValueAt(row, 1).toString());
                    addressField.setText(model.getValueAt(row, 2).toString());
                    cityField.setText(model.getValueAt(row, 3).toString());
                    spacesField.setText(model.getValueAt(row, 4).toString());
                }
            }
        });

        loadLots.run();
        return panel;
    }

    private JPanel buildReportsPanel() {
        Color bg = new Color(244, 247, 246);
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(bg);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel heading = new JLabel("Reports & Finance", JLabel.LEFT);
        heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(heading, BorderLayout.NORTH);

        // Use ReportPaymentView with admin permissions (reports only)
        JPanel embedded = wrapFrameContent(() -> new boundary.ReportPaymentView(boundary.ReportPaymentView.ViewMode.ADMIN_ONLY), "Reports & Finance");
        panel.add(embedded, BorderLayout.CENTER);
        return panel;
    }

    private void switchAdminContent(String key) {
        adminLayout.show(adminContent, key);
        if (ADMIN_OVERVIEW.equals(key)) {
            refreshDashboardData();
        }
    }

    private JPanel wrapFrameContent(Supplier<JComponent> componentFactory, String title) {
        JComponent content = componentFactory.get();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(244, 247, 246));
        if (title != null) {
            JLabel heading = new JLabel(title);
            heading.setFont(new Font("Segoe UI", Font.BOLD, 16));
            heading.setBorder(new EmptyBorder(8, 8, 8, 8));
            wrapper.add(heading, BorderLayout.NORTH);
        }
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    static class CustomButton extends JButton {
        private final Color base;
        private final Color hover;
        CustomButton(String text, Color base) { this(text, base, Color.WHITE, base.darker()); }
        CustomButton(String text, Color base, Color fg, Color hover) {
            super(text);
            this.base = base;
            this.hover = hover;
            setForeground(fg);
            setBackground(base);
            setFocusPainted(false);
            setBorderPainted(false);
            setOpaque(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setBorder(new EmptyBorder(8, 14, 8, 14));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseEntered(java.awt.event.MouseEvent e) { setBackground(CustomButton.this.hover); }
                @Override public void mouseExited(java.awt.event.MouseEvent e) { setBackground(CustomButton.this.base); }
            });
        }
    }
}
