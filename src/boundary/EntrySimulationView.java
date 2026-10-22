package boundary;

import control.EntryController;
import control.AllocationService.AllocationResult;
import utils.MockPhotoExtractor;
import utils.MockPhotoExtractor.ExtractedVehicleData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.Dialog;
import java.awt.Window;
import java.io.File;
import java.sql.Timestamp;

public class EntrySimulationView extends JPanel {

    private final EntryController entryController = new EntryController();
    private final JTextArea logArea = new JTextArea();
    private JLabel systemStatusIndicator;
    private JComboBox<String> lotSelector;

    public EntrySimulationView() {
        super(new BorderLayout());
        buildUI();
        loadLots();
        updateSystemStatus();
        
        // Timer to auto-update status indicator
        new Timer(3000, e -> updateSystemStatus()).start();
    }

    private void loadLots() {
        lotSelector.removeAllItems();
        java.util.List<entity.ParkingLot> lots = entryController.getAllParkingLots();
        for (entity.ParkingLot lot : lots) {
            lotSelector.addItem("Lot " + lot.getLotNumber() + " - " + lot.getName());
        }
    }

    private int getSelectedLotId() {
        String selected = (String) lotSelector.getSelectedItem();
        if (selected == null) return -1;
        try {
            return Integer.parseInt(selected.split(" ")[1]);
        } catch (Exception e) {
            return -1;
        }
    }

    private void updateSystemStatus() {
        int lotId = getSelectedLotId();
        if (lotId == -1) {
            if (systemStatusIndicator != null) {
                systemStatusIndicator.setText(" ● No Lot Selected");
                systemStatusIndicator.setForeground(Color.GRAY);
            }
            return;
        }
        boolean operational = isSystemReady(lotId);
        if (systemStatusIndicator != null) {
            if (operational) {
                systemStatusIndicator.setText(" ● System Operational");
                systemStatusIndicator.setForeground(new Color(46, 204, 113));
            } else {
                systemStatusIndicator.setText(" ● System Offline");
                systemStatusIndicator.setForeground(new Color(231, 76, 60));
            }
        }
    }

    private boolean isSystemReady(int lotId) {
        return entryController.isSystemReady(lotId);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        root.setBackground(new Color(248, 249, 252));

        JPanel hero = new JPanel(new BorderLayout());
        hero.setBackground(new Color(41, 128, 185));
        hero.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel title = new JLabel("Entry Scenario • Gate Sensor Simulation");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        
        systemStatusIndicator = new JLabel(" ● Checking Status...");
        systemStatusIndicator.setFont(new Font("Segoe UI", Font.BOLD, 13));
        systemStatusIndicator.setForeground(Color.WHITE);
        
        lotSelector = new JComboBox<>();
        lotSelector.setPreferredSize(new Dimension(200, 25));
        lotSelector.addActionListener(e -> updateSystemStatus());
        
        JPanel controlsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controlsRow.setOpaque(false);
        controlsRow.add(new JLabel("Entrance:"));
        controlsRow.add(lotSelector);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(systemStatusIndicator, BorderLayout.EAST);

        JLabel subtitle = new JLabel("Known vehicle -> Gate opens. Unknown -> Self-registration kiosk.");
        subtitle.setForeground(new Color(220, 233, 245));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        
        JPanel headerContent = new JPanel();
        headerContent.setLayout(new BoxLayout(headerContent, BoxLayout.Y_AXIS));
        headerContent.setOpaque(false);
        headerContent.add(titleRow);
        headerContent.add(Box.createVerticalStrut(8));
        headerContent.add(controlsRow);
        headerContent.add(Box.createVerticalStrut(8));
        headerContent.add(subtitle);

        hero.add(headerContent, BorderLayout.CENTER);
        root.add(hero, BorderLayout.NORTH);

        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(new LineBorder(new Color(220, 220, 220), 1, true));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel prompt = new JLabel("Press to simulate a car arrival at the gate:");
        prompt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        prompt.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton simulateBtn = new JButton("Simulate Car Arrival");
        simulateBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        simulateBtn.setBackground(new Color(230, 240, 251));
        simulateBtn.setForeground(new Color(33, 47, 61));
        simulateBtn.setFocusPainted(false);
        simulateBtn.setBorder(new LineBorder(new Color(46, 134, 193), 1, true));
        simulateBtn.setOpaque(true);
        simulateBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        simulateBtn.addActionListener(e -> onSimulateArrival());

        JLabel note = new JLabel("System will auto-start a session for known vehicles.");
        note.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        note.setForeground(new Color(90, 98, 110));
        note.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(prompt);
        left.add(Box.createVerticalStrut(8));
        left.add(simulateBtn);
        left.add(Box.createVerticalStrut(6));
        left.add(note);

        card.add(left, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBorder(BorderFactory.createTitledBorder("Events"));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(200, 260));
        card.add(logScroll, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);

        removeAll();
        add(root, BorderLayout.CENTER);
    }

    private void appendLog(String msg) {
        logArea.append(msg + "\n");
    }

    private void onSimulateArrival() {
        int lotId = getSelectedLotId();
        if (lotId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a parking lot first.", "Selection Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!isSystemReady(lotId)) {
            JOptionPane.showMessageDialog(this, 
                "⚠️ System Offline: Cannot accept vehicles at this entrance. Parking mechanism is currently turned off or under maintenance.", 
                "System Status", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String vehStr = JOptionPane.showInputDialog(this, "Enter Vehicle Number", "Car Arrival", JOptionPane.PLAIN_MESSAGE);
        if (vehStr == null || vehStr.trim().isEmpty()) return;
        int veh;
        try {
            veh = Integer.parseInt(vehStr.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid vehicle number", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (entryController.vehicleExists(veh)) {
            entity.Vehicle vehicle = entryController.getVehicle(veh);
            if (!entryController.hasAvailableSpot(lotId, vehicle.getSize(), vehicle.getWeight())) {
                JOptionPane.showMessageDialog(this, "⚠️ Parking Lot Full: No suitable spot or conveyor available for your vehicle.", "Full Capacity", JOptionPane.WARNING_MESSAGE);
                return;
            }
            handleKnownVehicle(veh);
        } else {
            // Pre-check for generic spot availability if unknown
            if (entryController.getAvailableParkingSpaces(lotId).isEmpty()) {
                JOptionPane.showMessageDialog(this, "⚠️ Parking Lot Full: No spots available.", "Full Capacity", JOptionPane.WARNING_MESSAGE);
                return;
            }
            openKioskDialog(veh);
        }
    }

    private void handleKnownVehicle(int vehicleNumber) {
        int lotId = getSelectedLotId();
        
        AllocationResult alloc = entryController.assignVehicleToSpot(vehicleNumber, lotId);
        if (!alloc.success) {
            appendLog("Allocation Failed: " + alloc.message);
            JOptionPane.showMessageDialog(this, "⚠️ " + alloc.message, "Allocation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (entryController.getActiveSession(vehicleNumber) == null) {
            entryController.startParkingSession(vehicleNumber, lotId, new Timestamp(System.currentTimeMillis()), alloc.spotId);
            appendLog("Started session for vehicle " + vehicleNumber + " at lot " + lotId + " (Spot " + alloc.spotId + ")");
        } else {
            appendLog("Vehicle " + vehicleNumber + " already has an active session.");
        }
        
        appendLog(alloc.message);
        
        // Show animated gate barrier instead of simple dialog
        GateBarrierView.showGateAnimation(this, true);
    }

    private void openKioskDialog(int vehicleNumber) {
        // First, ask for phone to check if client already exists (per story requirement)
        String phone = JOptionPane.showInputDialog(this, 
            "Enter your phone number to check for existing account:", 
            "Client Lookup", JOptionPane.QUESTION_MESSAGE);
        
        if (phone == null || phone.trim().isEmpty()) {
            return; // User cancelled
        }
        
        // Check if client exists by phone
        Integer existingClientId = entryController.findClientIdByPhone(phone.trim());
        
        if (existingClientId != null) {
            // Client found! Just register the new vehicle and link to existing client
            handleExistingClientNewVehicle(vehicleNumber, existingClientId, phone.trim());
            return;
        }
        
        // Client not found - open full registration kiosk
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Self Registration Kiosk", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(640, 520);
        dlg.setLocationRelativeTo(owner);
        dlg.setLayout(new BorderLayout(12, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(52, 73, 94));
        header.setBorder(new EmptyBorder(12, 14, 12, 14));
        JLabel title = new JLabel("Welcome • Self Registration");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        JLabel subtitle = new JLabel("Please confirm your details and upload 3 vehicle photos (Front, Rear, Side).");
        subtitle.setForeground(new Color(210, 220, 230));
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        dlg.add(header, BorderLayout.NORTH);

        JTextField firstName = createField(18);
        JTextField lastName = createField(18);
        JTextField phoneField = createField(18);
        phoneField.setText(phone.trim()); // Pre-fill the phone they entered
        JTextField vehField = createField(18);
        vehField.setText(String.valueOf(vehicleNumber));
        JTextField frontPath = createField(18);
        JTextField rearPath = createField(18);
        JTextField sidePath = createField(18);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(new EmptyBorder(14, 16, 14, 16));
        content.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;

        JPanel driverCard = createSectionPanel("Driver Info");
        addRow(driverCard, "First Name", firstName);
        addRow(driverCard, "Last Name", lastName);
        addRow(driverCard, "Phone", phoneField);
        addRow(driverCard, "Vehicle Number", vehField);

        JPanel photoCard = createSectionPanel("Vehicle Photos (Front / Rear / Side)");
        addBrowseRow(photoCard, "Front Photo", frontPath);
        addBrowseRow(photoCard, "Rear Photo", rearPath);
        addBrowseRow(photoCard, "Side Photo", sidePath);

        gbc.weightx = 1;
        content.add(driverCard, gbc);
        gbc.gridy = 1;
        content.add(photoCard, gbc);

        dlg.add(content, BorderLayout.CENTER);

        JButton submit = new JButton("Register & Open Gate");
        submit.setFont(new Font("Segoe UI", Font.BOLD, 14));
        submit.setBackground(new Color(230, 240, 251));
        submit.setForeground(new Color(33, 47, 61));
        submit.setFocusPainted(false);
        submit.setBorder(new LineBorder(new Color(46, 134, 193), 1, true));
        submit.setOpaque(true);
        submit.addActionListener(e -> {
            try {
                handleRegistration(dlg, firstName.getText().trim(), lastName.getText().trim(), phoneField.getText().trim(),
                        vehField.getText().trim(), frontPath.getText().trim(), rearPath.getText().trim(), sidePath.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(new EmptyBorder(10, 16, 12, 16));
        south.setBackground(Color.WHITE);
        JLabel reminder = new JLabel("Tip: clearer photo names (e.g., blue_front.jpg) help the mock extractor detect color.");
        reminder.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        reminder.setForeground(new Color(90, 98, 110));
        south.add(reminder, BorderLayout.WEST);
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnWrap.setOpaque(false);
        btnWrap.add(submit);
        south.add(btnWrap, BorderLayout.EAST);
        dlg.add(south, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private void handleExistingClientNewVehicle(int vehicleNumber, int clientId, String phone) {
        // Client exists - just need to register new vehicle and link it
        int lotId = getSelectedLotId();
        
        // Beautiful welcome dialog
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog welcomeDlg = new JDialog(owner, "Welcome Back!", Dialog.ModalityType.APPLICATION_MODAL);
        welcomeDlg.setSize(450, 280);
        welcomeDlg.setLocationRelativeTo(owner);
        welcomeDlg.setLayout(new BorderLayout());
        
        // Header with gradient-like effect
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(46, 204, 113));
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel welcomeIcon = new JLabel("[OK]", JLabel.CENTER);
        welcomeIcon.setFont(new Font("Segoe UI", Font.BOLD, 48));
        welcomeIcon.setForeground(Color.WHITE);
        welcomeIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel welcomeTitle = new JLabel("Account Found!", JLabel.CENTER);
        welcomeTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        welcomeTitle.setForeground(Color.WHITE);
        welcomeTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel welcomeMsg = new JLabel("<html><div style='text-align:center;'>Welcome back to ParkWise!<br>Please upload photos of your vehicle to complete registration.</div></html>", JLabel.CENTER);
        welcomeMsg.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        welcomeMsg.setForeground(new Color(220, 255, 220));
        welcomeMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        headerPanel.add(welcomeIcon);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(welcomeTitle);
        headerPanel.add(Box.createVerticalStrut(8));
        headerPanel.add(welcomeMsg);
        
        welcomeDlg.add(headerPanel, BorderLayout.CENTER);
        
        JButton continueBtn = new JButton("Continue to Upload Photos");
        continueBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        continueBtn.setBackground(new Color(30, 150, 80));
        continueBtn.setForeground(Color.WHITE);
        continueBtn.setUI(new BasicButtonUI());
        continueBtn.setOpaque(true);
        continueBtn.setBorderPainted(false);
        continueBtn.setFocusPainted(false);
        continueBtn.setBorder(new EmptyBorder(12, 20, 12, 20));
        continueBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        continueBtn.addActionListener(e -> {
            welcomeDlg.dispose();
            showPhotoUploadDialog(vehicleNumber, clientId, phone);
        });
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(new EmptyBorder(15, 0, 15, 0));
        btnPanel.add(continueBtn);
        welcomeDlg.add(btnPanel, BorderLayout.SOUTH);
        
        welcomeDlg.setVisible(true);
    }

    private void showPhotoUploadDialog(int vehicleNumber, int clientId, String phone) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Register Your Vehicle", Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setSize(600, 450);
        dlg.setLocationRelativeTo(owner);
        dlg.setLayout(new BorderLayout(0, 0));
        
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(52, 73, 94));
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel title = new JLabel("Upload Vehicle Photos");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        
        JLabel subtitle = new JLabel("Please provide 3 clear photos of your vehicle");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(180, 200, 220));
        
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        dlg.add(header, BorderLayout.NORTH);
        
        // Photos panel with cards
        JPanel photosPanel = new JPanel(new GridLayout(3, 1, 0, 15));
        photosPanel.setBackground(new Color(248, 249, 252));
        photosPanel.setBorder(new EmptyBorder(20, 30, 20, 30));
        
        JTextField frontPath = createField(25);
        JTextField rearPath = createField(25);
        JTextField sidePath = createField(25);
        
        photosPanel.add(createPhotoCard("Front View", "Photo of the vehicle from the front", frontPath, new Color(41, 128, 185)));
        photosPanel.add(createPhotoCard("Rear View", "Photo of the vehicle from the back", rearPath, new Color(142, 68, 173)));
        photosPanel.add(createPhotoCard("Side View", "Photo of the vehicle from the side", sidePath, new Color(39, 174, 96)));
        
        dlg.add(photosPanel, BorderLayout.CENTER);
        
        // Footer with button
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(new EmptyBorder(15, 30, 20, 30));
        
        JLabel tip = new JLabel("Tip: Clear photos help us detect your vehicle's color automatically");
        tip.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        tip.setForeground(new Color(120, 130, 140));
        footer.add(tip, BorderLayout.WEST);
        
        JButton registerBtn = new JButton("Register Vehicle & Enter Parking");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        registerBtn.setBackground(new Color(30, 150, 80));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setUI(new BasicButtonUI());
        registerBtn.setOpaque(true);
        registerBtn.setBorderPainted(false);
        registerBtn.setFocusPainted(false);
        registerBtn.setBorder(new EmptyBorder(12, 25, 12, 25));
        registerBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        registerBtn.addActionListener(e -> {
            // Extract vehicle data from photos
            ExtractedVehicleData data = MockPhotoExtractor.extract(
                frontPath.getText().trim(), rearPath.getText().trim(), sidePath.getText().trim());
            
            // Register vehicle and link to existing client
            entryController.registerVehicleForClient(vehicleNumber, data.type, data.color, data.size, data.weight, clientId,
                frontPath.getText().trim(), rearPath.getText().trim(), sidePath.getText().trim());
            
            dlg.dispose();
            
            // Show beautiful success dialog
            showRegistrationSuccessDialog(vehicleNumber);
            
            // Now proceed with parking session
            handleKnownVehicle(vehicleNumber);
        });
        
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnWrap.setBackground(Color.WHITE);
        btnWrap.add(registerBtn);
        footer.add(btnWrap, BorderLayout.EAST);
        
        dlg.add(footer, BorderLayout.SOUTH);
        
        dlg.setVisible(true);
    }

    private void showRegistrationSuccessDialog(int vehicleNumber) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog successDlg = new JDialog(owner, "Registration Complete", Dialog.ModalityType.APPLICATION_MODAL);
        successDlg.setSize(420, 300);
        successDlg.setLocationRelativeTo(owner);
        successDlg.setLayout(new BorderLayout());
        successDlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        // Success panel with green background
        JPanel successPanel = new JPanel();
        successPanel.setBackground(new Color(46, 204, 113));
        successPanel.setLayout(new BoxLayout(successPanel, BoxLayout.Y_AXIS));
        successPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        
        JLabel checkIcon = new JLabel("[OK]", JLabel.CENTER);
        checkIcon.setFont(new Font("Segoe UI", Font.BOLD, 42));
        checkIcon.setForeground(Color.WHITE);
        checkIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel successTitle = new JLabel("Vehicle Registered Successfully!", JLabel.CENTER);
        successTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        successTitle.setForeground(Color.WHITE);
        successTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel vehicleLabel = new JLabel("Vehicle #" + vehicleNumber, JLabel.CENTER);
        vehicleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        vehicleLabel.setForeground(new Color(220, 255, 220));
        vehicleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel messageLabel = new JLabel("has been linked to your account.", JLabel.CENTER);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setForeground(new Color(200, 255, 200));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel gateLabel = new JLabel("The gate will now open for you to enter.", JLabel.CENTER);
        gateLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        gateLabel.setForeground(new Color(180, 255, 180));
        gateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        successPanel.add(checkIcon);
        successPanel.add(Box.createVerticalStrut(12));
        successPanel.add(successTitle);
        successPanel.add(Box.createVerticalStrut(10));
        successPanel.add(vehicleLabel);
        successPanel.add(messageLabel);
        successPanel.add(Box.createVerticalStrut(8));
        successPanel.add(gateLabel);
        
        successDlg.add(successPanel, BorderLayout.CENTER);
        
        JButton okBtn = new JButton("OK");
        okBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        okBtn.setBackground(new Color(39, 174, 96));
        okBtn.setForeground(Color.WHITE);
        okBtn.setUI(new BasicButtonUI());
        okBtn.setOpaque(true);
        okBtn.setBorderPainted(false);
        okBtn.setFocusPainted(false);
        okBtn.setBorder(new EmptyBorder(10, 30, 10, 30));
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addActionListener(e -> successDlg.dispose());
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(Color.WHITE);
        btnPanel.setBorder(new EmptyBorder(15, 0, 15, 0));
        btnPanel.add(okBtn);
        successDlg.add(btnPanel, BorderLayout.SOUTH);
        
        successDlg.setVisible(true);
    }

    private JPanel createPhotoCard(String title, String description, JTextField field, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220, 225, 230), 1, true),
            new EmptyBorder(12, 15, 12, 15)
        ));
        
        // Left side - icon and text
        JPanel leftPanel = new JPanel(new BorderLayout(5, 2));
        leftPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(44, 62, 80));
        
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descLabel.setForeground(new Color(120, 130, 140));
        
        leftPanel.add(titleLabel, BorderLayout.NORTH);
        leftPanel.add(descLabel, BorderLayout.SOUTH);
        
        // Accent bar
        JPanel accent = new JPanel();
        accent.setPreferredSize(new Dimension(4, 50));
        accent.setBackground(accentColor);
        
        // Right side - field and browse button
        JPanel rightPanel = new JPanel(new BorderLayout(5, 0));
        rightPanel.setOpaque(false);
        
        field.setBorder(new LineBorder(new Color(200, 210, 220), 1, true));
        rightPanel.add(field, BorderLayout.CENTER);
        
        JButton browse = new JButton("Browse");
        browse.setFont(new Font("Segoe UI", Font.BOLD, 12));
        browse.setBackground(new Color(44, 62, 80));
        browse.setForeground(Color.WHITE);
        browse.setUI(new BasicButtonUI());
        browse.setOpaque(true);
        browse.setBorderPainted(false);
        browse.setFocusPainted(false);
        browse.setBorder(new EmptyBorder(8, 15, 8, 15));
        browse.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        browse.addActionListener(ev -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        rightPanel.add(browse, BorderLayout.EAST);
        
        card.add(accent, BorderLayout.WEST);
        card.add(leftPanel, BorderLayout.CENTER);
        card.add(rightPanel, BorderLayout.EAST);
        
        return card;
    }

    private JTextField createField(int columns) {
        JTextField field = new JTextField(columns);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return field;
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createTitledBorder(new LineBorder(new Color(220, 220, 220), 1, true), title));
        return panel;
    }

    private void addRow(JPanel panel, String label, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = panel.getComponentCount() / 2; gbc.weightx = 0.2;
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.8;
        panel.add(field, gbc);
    }

    private void addBrowseRow(JPanel panel, String label, JTextField field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = panel.getComponentCount() / 2; gbc.weightx = 0.25;
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        panel.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.55;
        panel.add(field, gbc);

        JButton browse = new JButton("Browse...");
        browse.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        browse.addActionListener(e -> chooseFile(field));
        gbc.gridx = 2; gbc.weightx = 0.2;
        panel.add(browse, gbc);
    }

    private void chooseFile(JTextField target) {
        JFileChooser chooser = new JFileChooser();
        int res = chooser.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            target.setText(f.getAbsolutePath());
        }
    }

    private void handleRegistration(JDialog dlg, String fName, String lName, String phone, String vehText,
                                    String front, String rear, String side) {
        if (fName.isEmpty() || lName.isEmpty() || phone.isEmpty() || vehText.isEmpty()) {
            throw new IllegalArgumentException("Please fill all required fields");
        }
        int vehicleNumber;
        try { vehicleNumber = Integer.parseInt(vehText); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Vehicle number must be numeric"); }

        // Extract vehicle data (mock)
        ExtractedVehicleData data = MockPhotoExtractor.extract(front, rear, side);

        // Check availability first (fail-fast)
        int lotId = getSelectedLotId();
        if (!entryController.hasAvailableSpot(lotId, data.size, data.weight)) {
            throw new IllegalStateException("Parking Lot Full: No suitable spot fits size " + data.size + " or conveyor capacity.");
        }

        // Find or create client
        Integer clientId = entryController.findClientIdByPhone(phone);
        if (clientId == null) {
            clientId = entryController.registerClient(fName, lName, phone);
        }

        // Register vehicle
        entryController.registerVehicle(vehicleNumber, data.type, data.color, data.size, data.weight,
                clientId, front, rear, side);

        // Perform actual allocation
        AllocationResult alloc = entryController.assignVehicleToSpot(vehicleNumber, lotId);
        if (!alloc.success) {
            throw new IllegalStateException("Allocation Failed: " + alloc.message);
        }

        // Start session if not active
        if (entryController.getActiveSession(vehicleNumber) == null) {
            entryController.startParkingSession(vehicleNumber, lotId, new Timestamp(System.currentTimeMillis()), alloc.spotId);
            appendLog("Registered vehicle " + vehicleNumber + " (color=" + data.color + ") and started session at lot " + lotId + " (Spot " + alloc.spotId + ")");
        } else {
            appendLog("Vehicle " + vehicleNumber + " already active after registration.");
        }

        appendLog(alloc.message);

        dlg.dispose();
        // Show animated gate barrier instead of simple dialog
        GateBarrierView.showGateAnimation(this, true);
    }
}
