package boundary;

import boundary.AdminDashboardPanel;
import boundary.ClientPortalPanel;
import boundary.LoginPanel;
import utils.DatabaseConnection;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BiConsumer;
import java.util.List;

public final class MainContainerFrame extends JFrame {

    private static final String HOME = "home";
    private static final String CLIENT = "client";
    private static final String ADMIN_LOGIN = "adminLogin";
    private static final String ADMIN_DASH = "adminDash";
    private static final String CLUB = "club";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel root = new JPanel(cardLayout);

    public MainContainerFrame() {
        initializeUI();
    }

    private void initializeUI() {
        setTitle("ParkWise | Smart Parking Platform");
        setSize(1200, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        AdminDashboardPanel adminDashPanel = new AdminDashboardPanel(new AdminDashboardPanel.AdminNav() {
            @Override public void goHome() { showCard(HOME); }
            @Override public void refreshAll() {
                // This will be called when we need to refresh the admin dashboard
            }
        });
        adminDashPanel.setAdminName("Admin");

        AdminLoginPanel adminLoginPanel = new AdminLoginPanel((username, password) -> {
            String fullName = DatabaseConnection.getAdminFullName(username, password);
            if (fullName != null && !fullName.trim().isEmpty()) {
                adminDashPanel.setAdminName(fullName.trim());
                showCard(ADMIN_DASH);
            } else {
                JOptionPane.showMessageDialog(MainContainerFrame.this, "Invalid credentials", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        LoginPanel loginPanel = new LoginPanel(new LoginPanel.Nav() {
            @Override public void goClient() { showCard(CLIENT); }
            @Override public void goAdminLogin() { showCard(ADMIN_LOGIN); }
        });

        ClientPortalPanel clientPanel = new ClientPortalPanel(new ClientPortalPanel.Nav() {
            @Override public void goHome() { showCard(HOME); }
            @Override public void goClub() { showCard(CLUB); }
        });

        ClubRegistrationPanel clubPanel = new ClubRegistrationPanel(() -> showCard(HOME));

        // We need to keep references to refresh data when switching tabs
        root.add(loginPanel, HOME);
        root.add(clientPanel, CLIENT);
        root.add(adminLoginPanel, ADMIN_LOGIN);
        root.add(adminDashPanel, ADMIN_DASH);
        root.add(clubPanel, CLUB);
        add(root, BorderLayout.CENTER);

        // Add a listener to refresh data whenever a card is shown
        root.getLayout(); // Trigger layout if needed
        Timer refreshTimer = new Timer(500, e -> {
            Component visible = null;
            for (Component c : root.getComponents()) {
                if (c.isVisible()) {
                    visible = c;
                    break;
                }
            }
            if (visible == adminDashPanel) {
                adminDashPanel.refreshAll();
            } else if (visible == clientPanel) {
                // If ClientPortalPanel has a refresh method, call it
                // For now, it contains ReportPaymentView which needs refresh
            }
        });
        // Instead of a timer, let's wrap the cardLayout show to refresh
    }

    private void showCard(String name) {
        cardLayout.show(root, name);
        refreshVisibleCard(name);
    }

    private void refreshVisibleCard(String name) {
        for (Component c : root.getComponents()) {
            if (c.isVisible()) {
                if (c instanceof AdminDashboardPanel) {
                    ((AdminDashboardPanel) c).refreshAll();
                } else if (c instanceof ClientPortalPanel) {
                    // Refresh client portal internal views if needed
                }
            }
        }
    }

    static class AdminLoginPanel extends JPanel {
        AdminLoginPanel(BiConsumer<String, String> onLogin) {
            Color bg = new Color(244, 247, 246);
            setLayout(new GridBagLayout());
            setBackground(bg);

            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(Color.WHITE);
            card.setBorder(new CompoundBorder(new LineBorder(new Color(220, 225, 230), 1, true), new EmptyBorder(18, 18, 18, 18)));
            card.setPreferredSize(new Dimension(560, 340));

            JPanel accent = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    GradientPaint gp = new GradientPaint(0, 0, new Color(31, 44, 61), getWidth(), getHeight(), new Color(52, 152, 219));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                }
            };
            accent.setPreferredSize(new Dimension(170, 0));
            accent.setLayout(new GridBagLayout());
            JLabel lock = new JLabel("🔐 ParkWise");
            lock.setFont(new Font("Segoe UI Emoji", Font.BOLD, 18));
            lock.setForeground(Color.WHITE);
            accent.add(lock);

            JPanel form = new JPanel();
            form.setOpaque(false);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setBorder(new EmptyBorder(6, 12, 6, 12));

            JLabel title = new JLabel("Admin Login", JLabel.LEFT);
            title.setFont(new Font("Segoe UI", Font.BOLD, 22));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            JLabel subtitle = new JLabel("Secure access to dashboard");
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            subtitle.setForeground(new Color(99, 110, 123));
            subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel userRow = new JPanel(new BorderLayout(6, 0));
            userRow.setOpaque(false);
            JLabel userLbl = new JLabel("Username:");
            userLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            JTextField userField = new JTextField();
            userField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            userField.setPreferredSize(new Dimension(280, 44));
            userField.setBorder(new CompoundBorder(new LineBorder(new Color(205, 212, 222), 1, true), new EmptyBorder(8, 10, 8, 10)));
            userRow.add(userLbl, BorderLayout.WEST);
            userRow.add(userField, BorderLayout.CENTER);

            JPanel passRow = new JPanel(new BorderLayout(6, 0));
            passRow.setOpaque(false);
            JLabel passLbl = new JLabel("Password:");
            passLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            JPasswordField passField = new JPasswordField();
            passField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            passField.setPreferredSize(new Dimension(280, 44));
            passField.setBorder(new CompoundBorder(new LineBorder(new Color(205, 212, 222), 1, true), new EmptyBorder(8, 10, 8, 10)));
            passRow.add(passLbl, BorderLayout.WEST);
            passRow.add(passField, BorderLayout.CENTER);

            CustomButton login = new CustomButton("Login", new Color(52, 152, 219));
            login.setPreferredSize(new Dimension(200, 46));
            login.setFont(new Font("Segoe UI", Font.BOLD, 16));
            login.setAlignmentX(Component.LEFT_ALIGNMENT);
            login.addActionListener(e -> {
                String u = userField.getText().trim();
                String p = new String(passField.getPassword());
                onLogin.accept(u, p);
            });

            CustomButton hint = new CustomButton("Hint", new Color(127, 140, 141));
            hint.setPreferredSize(new Dimension(110, 46));
            hint.setToolTipText("Show sample admin credentials from database");
            hint.addActionListener(e -> {
                List<String[]> samples = DatabaseConnection.getAdminSamples(3);
                if (samples.isEmpty()) {
                    JOptionPane.showMessageDialog(AdminLoginPanel.this, "No admin records found in database", "Hint", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    String note = "<html><b>Note:</b> credentials are stored in Access (Administrator table).<br>Pick one to auto-fill.</html>";
                    String[] display = new String[samples.size()];
                    for (int i = 0; i < samples.size(); i++) {
                        String[] row = samples.get(i);
                        String name = (row[2] != null && !row[2].isEmpty()) ? row[2] : "Admin";
                        display[i] = name + "  [" + row[0] + "]";
                    }
                    JComboBox<String> combo = new JComboBox<>(display);
                    combo.setSelectedIndex(0);
                    JPanel panel = new JPanel(new BorderLayout(8, 8));
                    panel.add(new JLabel(note), BorderLayout.NORTH);
                    panel.add(combo, BorderLayout.CENTER);
                    int res = JOptionPane.showConfirmDialog(AdminLoginPanel.this, panel, "Hint", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
                    if (res == JOptionPane.OK_OPTION) {
                        int idx = combo.getSelectedIndex();
                        if (idx >= 0 && idx < samples.size()) {
                            String[] chosen = samples.get(idx);
                            userField.setText(chosen[0]);
                            passField.setText(chosen[1]);
                            userField.requestFocusInWindow();
                        }
                    }
                }
            });

            form.add(title);
            form.add(Box.createVerticalStrut(4));
            form.add(subtitle);
            form.add(Box.createVerticalStrut(18));
            form.add(userRow);
            form.add(Box.createVerticalStrut(12));
            form.add(passRow);
            form.add(Box.createVerticalStrut(18));
            JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
            actions.setOpaque(false);
            actions.setMaximumSize(new Dimension(350, 48));
            actions.add(login);
            actions.add(hint);
            actions.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(actions);

            card.add(accent, BorderLayout.WEST);
            card.add(form, BorderLayout.CENTER);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 0, 0);
            add(card, gbc);
        }
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
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { setBackground(CustomButton.this.hover); }
                @Override public void mouseExited(MouseEvent e) { setBackground(CustomButton.this.base); }
            });
        }
    }
}
