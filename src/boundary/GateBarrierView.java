package boundary;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;

/**
 * GateBarrierView - A modern, animated barrier gate using Java 2D Graphics.
 * Part of the Boundary layer in BCE architecture.
 */
public class GateBarrierView extends JPanel {
    private double angle = 0; // 0 is closed, -90 is open
    private Timer timer;
    private boolean opening = true;
    private final int BARRIER_WIDTH = 250;
    private final int BARRIER_HEIGHT = 15;
    private final Color ACCENT_COLOR = new Color(231, 76, 60); // Red
    private final Color BASE_COLOR = new Color(52, 73, 94);   // Dark Blue/Grey

    public GateBarrierView() {
        setPreferredSize(new Dimension(400, 300));
        setBackground(new Color(245, 245, 245));
    }

    /**
     * Shows a modal dialog with the gate animation.
     */
    public static void showGateAnimation(Component parent, boolean open) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Gate Barrier System", true);
        GateBarrierView gateView = new GateBarrierView();
        gateView.opening = open;
        
        // Setup initial angle
        gateView.angle = open ? 0 : -90;

        dialog.add(gateView);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        // Start animation
        gateView.timer = new Timer(15, e -> {
            if (gateView.opening) {
                gateView.angle -= 2; // Smoothly open
                if (gateView.angle <= -90) {
                    gateView.angle = -90;
                    gateView.timer.stop();
                    // Close after delay
                    new Timer(1500, e2 -> dialog.dispose()).start();
                }
            } else {
                gateView.angle += 2; // Smoothly close
                if (gateView.angle >= 0) {
                    gateView.angle = 0;
                    gateView.timer.stop();
                    new Timer(1500, e2 -> dialog.dispose()).start();
                }
            }
            gateView.repaint();
        });
        
        gateView.timer.start();
        dialog.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = 100;
        int centerY = 220;

        // Draw Ground
        g2.setColor(new Color(200, 200, 200));
        g2.fillRect(0, centerY + 20, getWidth(), getHeight());

        // Draw Base Pillar
        g2.setColor(BASE_COLOR);
        g2.fill(new RoundRectangle2D.Double(centerX - 25, centerY - 80, 50, 100, 15, 15));
        
        // Pillar Detail (Light)
        g2.setColor(opening ? new Color(46, 204, 113) : new Color(231, 76, 60)); // Green if opening, Red if closing
        g2.fillOval(centerX - 10, centerY - 60, 20, 20);

        // Save transform for barrier rotation
        AffineTransform old = g2.getTransform();
        
        // Draw the Barrier
        g2.translate(centerX, centerY - 40);
        g2.rotate(Math.toRadians(angle));

        // Barrier Body
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(0, -BARRIER_HEIGHT/2.0, BARRIER_WIDTH, BARRIER_HEIGHT, 10, 10));
        
        // Red Stripes on Barrier
        g2.setColor(ACCENT_COLOR);
        for (int i = 20; i < BARRIER_WIDTH; i += 60) {
            g2.fillRect(i, -BARRIER_HEIGHT/2, 30, BARRIER_HEIGHT);
        }

        // Pivot Bolt
        g2.setColor(Color.LIGHT_GRAY);
        g2.fillOval(-10, -10, 20, 20);
        g2.setColor(Color.DARK_GRAY);
        g2.drawOval(-10, -10, 20, 20);

        g2.setTransform(old);

        // Draw Status Text
        g2.setColor(BASE_COLOR);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        String status = opening ? "GATE OPENING..." : "GATE CLOSING...";
        if (angle <= -90) status = "ACCESS GRANTED";
        g2.drawString(status, 150, 50);
    }
}
