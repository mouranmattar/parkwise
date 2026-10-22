package boundary;

import entity.ParkingLot;
import control.ClubController;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * ClubRegistrationPanel - Boundary for Club Membership Registration
 * Allows clients to register to the club and select preferred parking lots.
 * Accessible from anywhere in the system (Home screen or Header).
 */
public class ClubRegistrationPanel extends JPanel {

    public interface Nav {
        void goBack();
    }

    private final Nav nav;
    private final ClubController controller;
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField phoneField;
    private List<JCheckBox> lotCheckboxes;
    private JLabel statusLabel;

    public ClubRegistrationPanel(Nav nav) {
        super(new BorderLayout());
        this.nav = nav;
        this.controller = new ClubController();
        initializeUI();
    }

    private void initializeUI() {
        setBackground(new Color(244, 247, 246));

        // Header with back button
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel(new BorderLayout(20, 20));
        content.setBackground(new Color(244, 247, 246));
        content.setBorder(new EmptyBorder(20, 40, 20, 40));

        // Welcome/Hero section
        JPanel hero = createHeroSection();
        content.add(hero, BorderLayout.NORTH);

        // Form section
        JPanel form = createFormSection();
        content.add(form, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = createStatusBar();
        content.add(statusBar, BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(44, 62, 80));
        header.setBorder(new EmptyBorder(12, 16, 12, 16));

        JButton backBtn = new JButton("← Back");
        backBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        backBtn.setForeground(Color.WHITE);
        backBtn.setBackground(new Color(52, 73, 94));
        backBtn.setFocusPainted(false);
        backBtn.setBorderPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> nav.goBack());
        backBtn.setPreferredSize(new Dimension(100, 36));

        JLabel title = new JLabel("Join ParkWise Club", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        header.add(backBtn, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);

        return header;
    }

    private JPanel createHeroSection() {
        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setOpaque(false);
        hero.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel welcome = new JLabel("Become a Club Member Today!");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 28));
        welcome.setForeground(new Color(33, 47, 61));
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Get exclusive benefits including free first stay every month at your preferred lots!");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(90, 100, 110));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        hero.add(welcome);
        hero.add(Box.createVerticalStrut(8));
        hero.add(subtitle);

        return hero;
    }

    private JPanel createFormSection() {
        JPanel form = new JPanel(new BorderLayout(20, 20));
        form.setOpaque(false);

        // Personal info card
        JPanel personalCard = createPersonalInfoCard();

        // Preferred lots card
        JPanel lotsCard = createPreferredLotsCard();

        // Submit button
        JButton joinBtn = createJoinButton();

        // Left side: Personal info
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setOpaque(false);
        leftPanel.add(personalCard, BorderLayout.NORTH);
        leftPanel.add(joinBtn, BorderLayout.SOUTH);

        // Right side: Preferred lots
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        rightPanel.add(lotsCard, BorderLayout.CENTER);

        // Split layout
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        split.setResizeWeight(0.4);
        split.setBorder(null);
        split.setOpaque(false);

        form.add(split, BorderLayout.CENTER);

        return form;
    }

    private JPanel createPersonalInfoCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 225, 230), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        card.setPreferredSize(new Dimension(350, 250));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("Personal Information");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(44, 62, 80));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        card.add(title, gbc);

        // First Name
        gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.3;
        JLabel firstLbl = new JLabel("First Name:");
        firstLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        card.add(firstLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        firstNameField = new JTextField(15);
        firstNameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        firstNameField.setBorder(new CompoundBorder(
            new LineBorder(new Color(205, 212, 222), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        card.add(firstNameField, gbc);

        // Last Name
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel lastLbl = new JLabel("Last Name:");
        lastLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        card.add(lastLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        lastNameField = new JTextField(15);
        lastNameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lastNameField.setBorder(new CompoundBorder(
            new LineBorder(new Color(205, 212, 222), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        card.add(lastNameField, gbc);

        // Phone
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        JLabel phoneLbl = new JLabel("Phone:*");
        phoneLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        phoneLbl.setForeground(new Color(52, 152, 219));
        card.add(phoneLbl, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        phoneField = new JTextField(15);
        phoneField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        phoneField.setBorder(new CompoundBorder(
            new LineBorder(new Color(52, 152, 219), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        card.add(phoneField, gbc);

        // Required note
        gbc.gridy = 4;
        JLabel note = new JLabel("* Phone is required for membership");
        note.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        note.setForeground(new Color(156, 163, 175));
        card.add(note, gbc);

        return card;
    }

    private JPanel createPreferredLotsCard() {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 225, 230), 1, true),
            new EmptyBorder(20, 20, 20, 20)
        ));

        // Title
        JLabel title = new JLabel("Preferred Parking Lots");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(new Color(44, 62, 80));
        title.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(title, BorderLayout.NORTH);

        // Subtitle
        JLabel subtitle = new JLabel("Select your favorite lots for priority access:");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(90, 100, 110));
        subtitle.setBorder(new EmptyBorder(0, 0, 15, 0));
        card.add(subtitle, BorderLayout.NORTH);

        // Checkboxes panel
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        lotCheckboxes = new ArrayList<>();
        List<ParkingLot> lots = controller.getAllParkingLots();

        if (lots.isEmpty()) {
            JLabel empty = new JLabel("No parking lots available");
            empty.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            empty.setForeground(new Color(156, 163, 175));
            listPanel.add(empty);
        } else {
            for (ParkingLot lot : lots) {
                JCheckBox cb = new JCheckBox("Lot " + lot.getLotNumber() + " - " + lot.getName() + " (" + lot.getCity() + ")");
                cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                cb.putClientProperty("lotId", lot.getLotNumber());
                cb.setBackground(Color.WHITE);
                cb.setBorder(new EmptyBorder(5, 5, 5, 5));
                lotCheckboxes.add(cb);
                listPanel.add(cb);
                listPanel.add(Box.createVerticalStrut(5));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setPreferredSize(new Dimension(300, 150));

        card.add(scroll, BorderLayout.CENTER);

        // Benefit note
        JLabel benefit = new JLabel("Members get FREE first stay every month at selected lots!");
        benefit.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        benefit.setForeground(new Color(46, 204, 113));
        benefit.setBorder(new EmptyBorder(10, 0, 0, 0));
        card.add(benefit, BorderLayout.SOUTH);

        return card;
    }

    private JButton createJoinButton() {
        JButton joinBtn = new JButton("Join Club Now");
        joinBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        joinBtn.setForeground(Color.WHITE);
        joinBtn.setBackground(new Color(46, 204, 113));
        joinBtn.setFocusPainted(false);
        joinBtn.setBorderPainted(false);
        joinBtn.setOpaque(true);
        joinBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        joinBtn.setPreferredSize(new Dimension(200, 50));
        joinBtn.setBorder(new EmptyBorder(10, 20, 10, 20));

        joinBtn.addActionListener(e -> handleJoinClub());

        // Hover effect
        joinBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                joinBtn.setBackground(new Color(39, 174, 96));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                joinBtn.setBackground(new Color(46, 204, 113));
            }
        });

        return joinBtn;
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(254, 242, 242));
        statusBar.setBorder(new EmptyBorder(10, 15, 10, 15));
        statusBar.setVisible(false);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusBar.add(statusLabel, BorderLayout.CENTER);

        return statusBar;
    }

    private void handleJoinClub() {
        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String phone = phoneField.getText().trim();

        // Validation
        if (phone.isEmpty()) {
            showStatus("Phone number is required for membership", true);
            phoneField.requestFocus();
            return;
        }

        // Collect selected lots
        List<Integer> selectedLots = new ArrayList<>();
        for (JCheckBox cb : lotCheckboxes) {
            if (cb.isSelected()) {
                selectedLots.add((int) cb.getClientProperty("lotId"));
            }
        }

        // Call controller
        ClubController.RegistrationResult result = controller.registerClubMember(first, last, phone, selectedLots);

        if (result.success) {
            // Success message
            StringBuilder msg = new StringBuilder();
            msg.append("Welcome to ParkWise Club!\n\n");
            msg.append("Member: ").append(first.isEmpty() ? "Club" : first).append(" ").append(last.isEmpty() ? "Member" : last).append("\n");
            msg.append("Phone: ").append(phone).append("\n");
            msg.append("Joined: ").append(LocalDate.now().toString()).append("\n\n");
            if (result.selectedLotsCount > 0) {
                msg.append("Selected ").append(result.selectedLotsCount).append(" preferred lot(s).\n");
                msg.append("Enjoy your FREE first stay every month!");
            } else {
                msg.append("You can select preferred lots later from your profile.");
            }

            JOptionPane.showMessageDialog(
                this,
                msg.toString(),
                "Membership Confirmed!",
                JOptionPane.INFORMATION_MESSAGE
            );

            // Clear form
            clearForm();
            showStatus("Successfully joined! Welcome to the club!", false);
        } else {
            showStatus(result.message, true);
            JOptionPane.showMessageDialog(
                this,
                result.message,
                "Registration Failed",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void clearForm() {
        firstNameField.setText("");
        lastNameField.setText("");
        phoneField.setText("");
        for (JCheckBox cb : lotCheckboxes) {
            cb.setSelected(false);
        }
    }

    private void showStatus(String message, boolean isError) {
        JPanel statusBar = (JPanel) statusLabel.getParent();
        statusBar.setVisible(true);
        statusLabel.setText(message);
        if (isError) {
            statusBar.setBackground(new Color(254, 242, 242));
            statusLabel.setForeground(new Color(185, 28, 28));
        } else {
            statusBar.setBackground(new Color(240, 253, 244));
            statusLabel.setForeground(new Color(22, 163, 74));
        }
    }
}
