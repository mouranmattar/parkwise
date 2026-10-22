package boundary;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LoginPanel extends JPanel {
    public interface Nav {
        void goClient();
        void goAdminLogin();
    }

    public LoginPanel(Nav nav) {
        super(new GridLayout(1, 2));
        setBackground(Color.WHITE);

        // Left panel - Dark blue with brand (unchanged)
        JPanel left = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x0f2027), getWidth(), getHeight(), new Color(0x203a43));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 30));
                for (int i = 0; i < 6; i++) {
                    int size = 80 + i * 20;
                    int x = (int) (Math.random() * Math.max(1, getWidth()));
                    int y = (int) (Math.random() * Math.max(1, getHeight()));
                    g2.drawOval(x, y, size, size);
                }
                g2.dispose();
            }
        };
        left.setLayout(new GridBagLayout());
        JLabel brand = new JLabel("ParkWise");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 42));
        brand.setForeground(Color.WHITE);
        JLabel tagline = new JLabel("Next Gen Parking Solutions");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        tagline.setForeground(new Color(220, 230, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(0, 0, 8, 0);
        left.add(brand, gbc);
        gbc.gridy = 1;
        left.add(tagline, gbc);

        // Right panel - Live Valet Parking visualization
        LiveValetPanel rightPanel = new LiveValetPanel(new LiveValetPanel.Nav() {
            @Override public void goClient() { nav.goClient(); }
            @Override public void goAdminLogin() { nav.goAdminLogin(); }
        });

        add(left);
        add(rightPanel);
    }
}
