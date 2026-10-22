package boundary;

import control.ParkingSessionController;
import control.ReportController;
import control.ParkingSessionController.PaymentResult;
import entity.ClubGrowthRow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.Dialog;
import java.awt.Desktop;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import utils.DatabaseConnection;

public class ReportPaymentView extends JPanel {

    public enum ViewMode {
        ADMIN_ONLY,    // Show reports only
        CLIENT_ONLY    // Show payment only
    }

    private final ParkingSessionController sessionController = new ParkingSessionController();
    private final ReportController reportController = new ReportController();
    private final ViewMode viewMode;

    private JTextField vehicleField;
    private JTextField phoneField;
    private JTextArea paymentOutput;
    private JLabel statusLabel;
    private JTable activeTable;
    private DefaultTableModel activeTableModel;
    private JComboBox<Integer> yearCombo;
    private JComboBox<Integer> growthYearCombo;
    private DefaultTableModel growthTableModel;
    private JTextField pdfPathField;
    private JTextField xmlPathField;

    public ReportPaymentView() {
        this(ViewMode.CLIENT_ONLY); // Default for backward compatibility
    }

    public ReportPaymentView(ViewMode mode) {
        super(new BorderLayout(10, 10));
        this.viewMode = mode;

        JTabbedPane tabs = new JTabbedPane();
        
        // Add tabs based on user permissions
        if (viewMode == ViewMode.CLIENT_ONLY) {
            tabs.add("End Session & Pay", buildPaymentPanel());
        } else if (viewMode == ViewMode.ADMIN_ONLY) {
            tabs.add("Annual Report", buildReportPanel());
            tabs.add("Club Growth", buildGrowthPanel());
        }
        
        add(tabs, BorderLayout.CENTER);

        // Populate available years for the Annual Report combo (admin only)
        if (viewMode == ViewMode.ADMIN_ONLY) {
            refreshAvailableYears();
        }
    }

    public void refreshData() {
        refreshActiveSessions();
        if (viewMode == ViewMode.ADMIN_ONLY) {
            refreshAvailableYears();
            refreshGrowthTable();
        }
    }

    private JPanel buildPaymentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));

        form.add(new JLabel("Vehicle Number:"));
        vehicleField = new JTextField(8);
        form.add(vehicleField);

        form.add(new JLabel("Phone:"));
        phoneField = new JTextField(10);
        form.add(phoneField);

        JCheckBox receiptCheck = new JCheckBox("I want a Receipt");
        JButton payBtn = new JButton("End & Pay");
        payBtn.addActionListener(e -> onEndSession(receiptCheck));
        form.add(payBtn);
        form.add(receiptCheck);

        statusLabel = new JLabel(" ");
        statusLabel.setForeground(Color.RED);
        JPanel northWrap = new JPanel(new BorderLayout());
        northWrap.add(form, BorderLayout.NORTH);
        northWrap.add(statusLabel, BorderLayout.CENTER);
        panel.add(northWrap, BorderLayout.NORTH);

        // Table of active sessions
        String[] cols = {"SessionID", "VehicleNum", "EntryTime"};
        activeTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        activeTable = new JTable(activeTableModel);
        applyTableStyle(activeTable);
        activeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activeTable.getSelectionModel().addListSelectionListener(e -> {
            int row = activeTable.getSelectedRow();
            if (row >= 0) {
                Object val = activeTableModel.getValueAt(row, 0);
                Object veh = activeTableModel.getValueAt(row, 1);
                if (veh != null) {
                    vehicleField.setText(veh.toString());
                }
            }
        });

        JScrollPane tableScroll = new JScrollPane(activeTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Active Sessions"));

        paymentOutput = new JTextArea();
        paymentOutput.setEditable(false);
        paymentOutput.setBorder(BorderFactory.createTitledBorder("Payment Result"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, new JScrollPane(paymentOutput));
        split.setResizeWeight(0.6);
        panel.add(split, BorderLayout.CENTER);

        refreshActiveSessions();
        return panel;
    }

    private JPanel buildGrowthPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Year:"));
        growthYearCombo = new JComboBox<>();
        growthYearCombo.setEditable(false);
        growthYearCombo.setPreferredSize(new Dimension(100, 24));
        top.add(growthYearCombo);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshGrowthTable());
        top.add(refresh);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = {"Month", "New Members", "Cumulative", "% Growth"};
        growthTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(growthTableModel);
        applyTableStyle(table);
        JScrollPane scroll = new JScrollPane(table);
        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private void applyTableStyle(JTable table) {
        table.setRowHeight(35);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(225, 239, 252));
        table.setSelectionForeground(new Color(33, 47, 61));
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
        header.setOpaque(true);
    }

    private JPanel buildReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JPanel yearRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        yearRow.add(new JLabel("Year:"));
        yearCombo = new JComboBox<>();
        yearCombo.setEditable(false);
        yearCombo.setPreferredSize(new Dimension(100, 24));
        yearRow.add(yearCombo);
        yearRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        yearRow.setBorder(new EmptyBorder(0, 0, 4, 0));
        form.add(yearRow);

        JPanel pdfRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        pdfRow.add(new JLabel("Choose save location (PDF):"));
        pdfPathField = new JTextField(25);
        pdfPathField.setText("AnnualSummary.pdf");
        JButton pdfBrowse = new JButton("Browse...");
        pdfBrowse.addActionListener(e -> chooseFile(pdfPathField, "pdf"));
        pdfRow.add(pdfPathField);
        pdfRow.add(pdfBrowse);
        pdfRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        pdfRow.setBorder(new EmptyBorder(0, 0, 4, 0));
        form.add(pdfRow);

        JPanel xmlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        xmlRow.add(new JLabel("Choose save location (XML):"));
        xmlPathField = new JTextField(25);
        xmlPathField.setText("AnnualSummary.xml");
        JButton xmlBrowse = new JButton("Browse...");
        xmlBrowse.addActionListener(e -> chooseFile(xmlPathField, "xml"));
        xmlRow.add(xmlPathField);
        xmlRow.add(xmlBrowse);
        xmlRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        xmlRow.setBorder(new EmptyBorder(0, 0, 4, 0));
        form.add(xmlRow);

        panel.add(form, BorderLayout.NORTH);

        JButton generateBtn = new JButton("Generate & Open PDF");
        generateBtn.addActionListener(e -> onGenerateReport());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnRow.add(generateBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    private void chooseFile(JTextField target, String ext) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File(target.getText()));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public void onEndSession(JCheckBox receiptCheck) {
        String vehicleText = vehicleField.getText().trim();
        if (vehicleText.isEmpty()) {
            showMessage("Enter Vehicle Number");
            setStatus("Enter Vehicle Number", Color.RED);
            return;
        }
        String phone = phoneField.getText().trim();
        if (receiptCheck.isSelected() && phone.isEmpty()) {
            showMessage("Phone is required to issue receipt.");
            setStatus("Phone required for receipt", Color.RED);
            return;
        }
        try {
            int vehicleNumber = Integer.parseInt(vehicleText);
            setStatus("Contacting External Payment Gateway...", Color.BLUE);
            showGatewayDialog();
            PaymentResult res = sessionController.endSessionAndPayByVehicle(vehicleNumber, phone);
            paymentOutput.setText("Session #" + res.sessionId + "\n" +
                    "Vehicle: " + res.vehicleNumber + "\n" +
                    "Lot: " + res.parkingLotId + "\n" +
                    "Amount: " + String.format("%.2f", res.amount) + "\n" +
                    "Billed Hours: " + res.billedHours + "\n" +
                    "Applied: " + res.appliedRate);
            if (res.alreadyPaid) {
                setStatus("⚠ ALREADY PAID on " + res.endTime, Color.RED);
                showMessage("Session already paid. No new payment processed.");
            } else if (res.cancelled) {
                setStatus("⚠ cancelled on " + res.endTime, Color.RED);
                showMessage("Payment completed for session #" + res.sessionId);
            } else {
                setStatus("Payment completed for session #" + res.sessionId, new Color(0, 128, 0));
                // Show animated gate barrier instead of simple message
                GateBarrierView.showGateAnimation(this, true);
            }
            if (!res.alreadyPaid && receiptCheck.isSelected()) {
                // Show the new professional receipt design
                ReceiptDesignView.showReceipt(
                    this, 
                    phoneField.getText().trim(), 
                    res.vehicleNumber, 
                    res.parkingLotId, 
                    res.billedHours, 
                    res.appliedRate, 
                    res.amount, 
                    String.valueOf(res.sessionId)
                );
            }
            refreshActiveSessions();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Vehicle Number", "Error", JOptionPane.ERROR_MESSAGE);
            setStatus("Invalid Vehicle Number", Color.RED);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "DB/Error: " + ex.getMessage() + "\nPlease verify DB connection and that SessionID exists.", "Error", JOptionPane.ERROR_MESSAGE);
            setStatus("DB/Error: " + ex.getMessage(), Color.RED);
        }
    }

    public void onGenerateReport() {
        try {
            Integer selected = (Integer) yearCombo.getSelectedItem();
            if (selected == null) {
                showMessage("No available years in data");
                return;
            }
            int year = selected;
            String pdf = pdfPathField.getText().trim();
            String xml = xmlPathField.getText().trim();
            if (pdf.isEmpty()) {
                showMessage("Choose a PDF file path");
                return;
            }
            if (xml.isEmpty()) {
                showMessage("Choose an XML file path");
                return;
            }
            reportController.generateAnnualReport(year, pdf, xml);
            tryOpenPdf(pdf);
            showMessage("Report generated:\nPDF: " + pdf + "\nXML: " + xml);
        } catch (NumberFormatException ex) {
            showMessage("Invalid year");
        } catch (Exception ex) {
            showMessage("Error: " + ex.getMessage());
        }
    }

    private void tryOpenPdf(String path) {
        try {
            File f = new File(path);
            if (Desktop.isDesktopSupported() && f.exists()) {
                Desktop.getDesktop().open(f);
            }
        } catch (IOException ignore) {
            // if open fails, just skip
        }
    }

    private void showGatewayDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        final JDialog dlg = new JDialog(owner, "Payment Gateway", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(300, 120);
        dlg.setLocationRelativeTo(owner);
        dlg.setLayout(new BorderLayout());
        JLabel msg = new JLabel("Contacting External Payment Gateway...", SwingConstants.CENTER);
        dlg.add(msg, BorderLayout.CENTER);
        Timer t = new Timer(1200, e -> {
            msg.setText("Approved");
            ((Timer) e.getSource()).stop();
            // close after short delay
            new Timer(500, ev -> dlg.dispose()).start();
        });
        t.setRepeats(false);
        t.start();
        dlg.setVisible(true);
    }

    private void refreshActiveSessions() {
        activeTableModel.setRowCount(0);
        for (entity.ParkingSession s : utils.DatabaseConnection.getActiveSessions()) {
            activeTableModel.addRow(new Object[]{
                    s.getSessionID(),
                    s.getVehicleNumber(),
                    s.getStartTime()
            });
        }
    }

    private void refreshAvailableYears() {
        yearCombo.removeAllItems();
        if (growthYearCombo != null) growthYearCombo.removeAllItems();
        List<Integer> years = utils.DatabaseConnection.getAvailableYears();
        for (Integer yr : years) {
            yearCombo.addItem(yr);
            if (growthYearCombo != null) growthYearCombo.addItem(yr);
        }
        if (yearCombo.getItemCount() == 0) {
            int current = java.time.Year.now().getValue();
            yearCombo.addItem(current);
            if (growthYearCombo != null) growthYearCombo.addItem(current);
        }
        refreshGrowthTable();
    }

    private void refreshGrowthTable() {
        if (growthTableModel == null) return;
        Integer yr = growthYearCombo != null ? (Integer) growthYearCombo.getSelectedItem() : null;
        if (yr == null && growthYearCombo != null && growthYearCombo.getItemCount() > 0) {
            yr = growthYearCombo.getItemAt(0);
        }
        if (yr == null) return;
        List<ClubGrowthRow> rows = reportController.getClubGrowthRows(yr);
        growthTableModel.setRowCount(0);
        for (ClubGrowthRow r : rows) {
            growthTableModel.addRow(new Object[]{
                    r.getMonth(),
                    r.getNewMembers(),
                    r.getCumulative(),
                    String.format("%.2f%%", r.getGrowthPercent())
            });
        }
    }

    private void setStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private void showMessage(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }
}
