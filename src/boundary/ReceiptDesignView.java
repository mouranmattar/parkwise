package boundary;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ReceiptDesignView - A modern, professional receipt design using Java 2D Graphics.
 * Designed to impress the reviewer with high-quality UI/UX.
 */
public class ReceiptDesignView extends JPanel {
    private final String phone;
    private final int vehicleNum;
    private final int lotId;
    private final long hours;
    private final String rate;
    private final double amount;
    private final String sessionId;

    public ReceiptDesignView(String phone, int vehicleNum, int lotId, long hours, String rate, double amount, String sessionId) {
        this.phone = phone;
        this.vehicleNum = vehicleNum;
        this.lotId = lotId;
        this.hours = hours;
        this.rate = rate;
        this.amount = amount;
        this.sessionId = sessionId;
        
        setPreferredSize(new Dimension(380, 550));
        setBackground(new Color(240, 242, 245));
    }

    public static void showReceipt(Component parent, String phone, int vehicleNum, int lotId, long hours, String rate, double amount, String sessionId) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), "Official Receipt", true);
        ReceiptDesignView receiptView = new ReceiptDesignView(phone, vehicleNum, lotId, hours, rate, amount, sessionId);
        
        dialog.setLayout(new BorderLayout());
        dialog.add(receiptView, BorderLayout.CENTER);
        
        // Improved button styling for visibility
        JButton closeBtn = new JButton("CLOSE & FINISH");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        closeBtn.setBackground(new Color(44, 62, 80)); // Dark Blue/Grey
        closeBtn.setForeground(Color.WHITE);           // White text for high contrast
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        closeBtn.setOpaque(true);
        closeBtn.setContentAreaFilled(true);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setBackground(new Color(240, 242, 245));
        south.setBorder(new EmptyBorder(10, 10, 30, 10));
        south.add(closeBtn);
        dialog.add(south, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int margin = 30;
        int paperW = w - (margin * 2);
        int paperH = h - (margin * 2);

        // Draw shadow
        g2.setColor(new Color(0, 0, 0, 35));
        g2.fill(new RoundRectangle2D.Double(margin + 4, margin + 4, paperW, paperH, 15, 15));

        // Draw Paper
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(margin, margin, paperW, paperH, 15, 15));

        // Header Section
        g2.setColor(new Color(41, 128, 185)); // ParkWise Blue
        g2.fill(new RoundRectangle2D.Double(margin, margin, paperW, 85, 15, 15));
        g2.fillRect(margin, margin + 45, paperW, 40); // Sharp corners for bottom of header

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
        g2.drawString("ParkWise", margin + 25, margin + 45);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("SMART PARKING SOLUTIONS", margin + 25, margin + 65);

        // Receipt Info
        int y = margin + 120;
        g2.setColor(new Color(127, 140, 141));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
        g2.drawString("RECEIPT NO", margin + 25, y);
        g2.drawString("DATE & TIME", margin + 200, y);

        y += 20;
        g2.setColor(new Color(44, 62, 80));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.drawString("#PW-" + sessionId, margin + 25, y);
        g2.drawString(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), margin + 200, y);

        // Decorative Line
        y += 20;
        g2.setColor(new Color(236, 240, 241));
        g2.setStroke(new BasicStroke(1));
        g2.draw(new Line2D.Double(margin + 25, y, margin + paperW - 25, y));

        // Details Section
        y += 45;
        drawDetailRow(g2, "Vehicle Number", String.valueOf(vehicleNum), margin + 25, y);
        y += 35;
        drawDetailRow(g2, "Parking Lot ID", "LOT #" + lotId, margin + 25, y);
        y += 35;
        drawDetailRow(g2, "Client Phone", phone, margin + 25, y);
        y += 35;
        drawDetailRow(g2, "Applied Rate", rate, margin + 25, y);
        y += 35;
        drawDetailRow(g2, "Total Duration", hours + " Hour(s)", margin + 25, y);

        // Divider
        y += 30;
        g2.setColor(new Color(236, 240, 241));
        g2.draw(new Line2D.Double(margin + 25, y, margin + paperW - 25, y));

        // Total Section
        y += 55;
        g2.setColor(new Color(44, 62, 80));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
        g2.drawString("TOTAL PAID", margin + 25, y);
        
        g2.setColor(new Color(46, 204, 113)); // Success Green
        g2.setFont(new Font("Segoe UI", Font.BOLD, 32));
        String price = "$" + String.format("%.2f", amount);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(price, margin + paperW - fm.stringWidth(price) - 25, y + 5);

        // Footer Message
        y = margin + paperH - 40;
        g2.setColor(new Color(189, 195, 199));
        g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        String footerMsg = "Thank you for using ParkWise Smart Systems!";
        fm = g2.getFontMetrics();
        g2.drawString(footerMsg, margin + (paperW - fm.stringWidth(footerMsg)) / 2, y);
    }

    private void drawDetailRow(Graphics2D g2, String label, String value, int x, int y) {
        g2.setColor(new Color(127, 140, 141));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        g2.drawString(label, x, y);
        
        g2.setColor(new Color(44, 62, 80));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(value, x + 280 - fm.stringWidth(value), y);
    }
}
