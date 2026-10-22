package boundary;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;

/**
 * LiveValetPanel - Interactive parking lot visualization for the Home Page.
 * BCE Boundary Layer: Custom JPanel that renders a top-down parking scene.
 * Features: Animated car with gate opening, reverse parking, and perpendicular parking.
 */
public class LiveValetPanel extends JPanel {

    // Navigation callback interface
    public interface Nav {
        void goClient();
        void goAdminLogin();
    }

    // Animation state for car
    private double carX;
    private double carY;
    private double carAngle; // Rotation angle in degrees (0 = vertical, 90 = horizontal)
    private double targetX;
    private double targetY;
    private double targetAngle;

    // Gate animation state
    private double clientGateAngle = 0; // 0 = closed, 90 = fully open
    private double adminGateAngle = 0;
    private double targetClientGateAngle = 0;
    private double targetAdminGateAngle = 0;

    // Parking spot rectangles (bounds)
    private Rectangle clientSpot;
    private Rectangle adminSpot;

    // Hover states
    private boolean clientHovered = false;
    private boolean adminHovered = false;

    // Animation phases
    private enum Phase { IDLE, APPROACHING, GATE_OPENING, ENTERING, ROTATING, PARKED, EXITING }
    private Phase clientPhase = Phase.IDLE;
    private Phase adminPhase = Phase.IDLE;

    // Animation timer (60fps)
    private Timer animationTimer;
    private static final int FPS = 60;
    private static final int FRAME_DELAY = 1000 / FPS;

    // Colors
    private static final Color BG_COLOR = new Color(248, 250, 252);
    private static final Color SPOT_BORDER = new Color(220, 225, 235);
    private static final Color CLIENT_HOVER_COLOR = new Color(46, 204, 113);
    private static final Color ADMIN_HOVER_COLOR = new Color(52, 152, 219);
    private static final Color CAR_BODY = new Color(44, 62, 80);
    private static final Color CAR_ROOF = new Color(52, 73, 94);
    private static final Color CAR_WINDOW = new Color(127, 140, 141);
    private static final Color CAR_HEADLIGHT = new Color(241, 196, 15);
    private static final Color CAR_TAILLIGHT = new Color(231, 76, 60);
    private static final Color GATE_COLOR = new Color(149, 165, 166);
    private static final Color GATE_POST_COLOR = new Color(99, 110, 114);

    // Car dimensions
    private static final int CAR_WIDTH = 45;
    private static final int CAR_HEIGHT = 70;

    // Nav callback
    private Nav navCallback;

    public LiveValetPanel(Nav nav) {
        this.navCallback = nav;
        setBackground(BG_COLOR);
        setLayout(null);

        // Initialize car position
        carX = 0;
        carY = 0;
        carAngle = 0;
        targetX = 0;
        targetY = 0;
        targetAngle = 0;

        // Setup animation timer
        animationTimer = new Timer(FRAME_DELAY, e -> animate());
        animationTimer.start();

        // Setup mouse listener
        setupMouseListener();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        calculateSpots();
        // Set initial car position at bottom center
        if (carX == 0 && carY == 0) {
            carX = getWidth() / 2.0 - CAR_WIDTH / 2.0;
            carY = getHeight() - CAR_HEIGHT - 20;
            targetX = carX;
            targetY = carY;
            targetAngle = 0;
        }
    }

    private void calculateSpots() {
        int width = getWidth();
        int height = getHeight();

        // Increased spot size to accommodate perpendicular parking
        int spotWidth = Math.min(320, width - 60);
        int spotHeight = 140;
        int spotX = (width - spotWidth) / 2;
        int clientY = (int) (height * 0.18);
        int adminY = (int) (height * 0.52);

        clientSpot = new Rectangle(spotX, clientY, spotWidth, spotHeight);
        adminSpot = new Rectangle(spotX, adminY, spotWidth, spotHeight);
    }

    private void setupMouseListener() {
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                checkHover(e.getX(), e.getY());
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                clientHovered = false;
                adminHovered = false;
                // Reset animation - car returns to idle
                startExitAnimation();
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (clientHovered && navCallback != null) {
                    navCallback.goClient();
                } else if (adminHovered && navCallback != null) {
                    navCallback.goAdminLogin();
                }
            }
        });
    }

    private void checkHover(int mouseX, int mouseY) {
        boolean wasClientHovered = clientHovered;
        boolean wasAdminHovered = adminHovered;

        clientHovered = clientSpot != null && clientSpot.contains(mouseX, mouseY);
        adminHovered = adminSpot != null && adminSpot.contains(mouseX, mouseY);

        if (clientHovered && !wasClientHovered) {
            startParkingAnimation(clientSpot, true);
        } else if (adminHovered && !wasAdminHovered) {
            startParkingAnimation(adminSpot, false);
        } else if (!clientHovered && !adminHovered && (wasClientHovered || wasAdminHovered)) {
            startExitAnimation();
        }

        if (clientHovered != wasClientHovered || adminHovered != wasAdminHovered) {
            repaint();
        }
    }

    private void startParkingAnimation(Rectangle spot, boolean isClient) {
        // Phase 1: Move car to approach position (in front of gate)
        targetX = spot.x + spot.width / 2.0 - CAR_WIDTH / 2.0;
        targetY = spot.y + spot.height + 30;
        targetAngle = 0;
        
        if (isClient) {
            clientPhase = Phase.APPROACHING;
            targetClientGateAngle = 90;
        } else {
            adminPhase = Phase.APPROACHING;
            targetAdminGateAngle = 90;
        }
    }

    private void startExitAnimation() {
        // Return car to bottom center
        targetX = getWidth() / 2.0 - CAR_WIDTH / 2.0;
        targetY = getHeight() - CAR_HEIGHT - 20;
        targetAngle = 0;
        targetClientGateAngle = 0;
        targetAdminGateAngle = 0;
        clientPhase = Phase.IDLE;
        adminPhase = Phase.IDLE;
    }

    private void animate() {
        boolean needsRepaint = false;

        // Animate car position
        double speed = 0.08;
        double dx = (targetX - carX) * speed;
        double dy = (targetY - carY) * speed;
        double dAngle = (targetAngle - carAngle) * speed;

        if (Math.abs(dx) > 0.3 || Math.abs(dy) > 0.3 || Math.abs(dAngle) > 0.3) {
            carX += dx;
            carY += dy;
            carAngle += dAngle;
            needsRepaint = true;
        } else {
            carX = targetX;
            carY = targetY;
            carAngle = targetAngle;
        }

        // Animate gates
        double gateSpeed = 0.1;
        double dgClient = (targetClientGateAngle - clientGateAngle) * gateSpeed;
        double dgAdmin = (targetAdminGateAngle - adminGateAngle) * gateSpeed;

        if (Math.abs(dgClient) > 0.2) {
            clientGateAngle += dgClient;
            needsRepaint = true;
        } else {
            clientGateAngle = targetClientGateAngle;
        }

        if (Math.abs(dgAdmin) > 0.2) {
            adminGateAngle += dgAdmin;
            needsRepaint = true;
        } else {
            adminGateAngle = targetAdminGateAngle;
        }

        // Check for phase transitions
        if (clientPhase == Phase.APPROACHING && Math.abs(dx) < 0.5 && Math.abs(dy) < 0.5) {
            // Car arrived at approach position, now enter and rotate
            clientPhase = Phase.ENTERING;
            // Move into spot and rotate 90 degrees for perpendicular parking
            targetX = clientSpot.x + clientSpot.width / 2.0 - CAR_HEIGHT / 2.0;
            targetY = clientSpot.y + clientSpot.height / 2.0 - CAR_WIDTH / 2.0;
            targetAngle = 90; // Rotate to horizontal (perpendicular)
        }

        if (adminPhase == Phase.APPROACHING && Math.abs(dx) < 0.5 && Math.abs(dy) < 0.5) {
            adminPhase = Phase.ENTERING;
            targetX = adminSpot.x + adminSpot.width / 2.0 - CAR_HEIGHT / 2.0;
            targetY = adminSpot.y + adminSpot.height / 2.0 - CAR_WIDTH / 2.0;
            targetAngle = 90;
        }

        if (needsRepaint) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Draw background gradient
        GradientPaint bgGradient = new GradientPaint(0, 0, BG_COLOR, getWidth(), getHeight(), new Color(235, 240, 245));
        g2.setPaint(bgGradient);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Draw road markings
        drawRoadMarkings(g2);

        // Draw parking spots with gates
        drawParkingSpot(g2, clientSpot, "Client Portal", "Start parking or pay", CLIENT_HOVER_COLOR, clientHovered, clientGateAngle);
        drawParkingSpot(g2, adminSpot, "Admin Access", "Manage system", ADMIN_HOVER_COLOR, adminHovered, adminGateAngle);

        // Draw the car with rotation
        drawCar(g2, carX, carY, carAngle);

        // Draw hint text at bottom
        g2.setColor(new Color(120, 130, 140));
        g2.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        String hint = "Hover over a spot to watch the car park";
        int hintWidth = g2.getFontMetrics().stringWidth(hint);
        g2.drawString(hint, (getWidth() - hintWidth) / 2, getHeight() - 8);

        g2.dispose();
    }

    private void drawRoadMarkings(Graphics2D g2) {
        g2.setColor(new Color(200, 205, 215));
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{15, 10}, 0));
        
        // Center line
        int centerX = getWidth() / 2;
        g2.drawLine(centerX, 50, centerX, getHeight() - 50);
        
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawParkingSpot(Graphics2D g2, Rectangle spot, String title, String subtitle,
                                  Color hoverColor, boolean hovered, double gateAngle) {
        if (spot == null) return;

        // Draw shadow
        g2.setColor(new Color(0, 0, 0, 12));
        g2.fillRoundRect(spot.x + 3, spot.y + 3, spot.width, spot.height, 12, 12);

        // Draw spot background
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(spot.x, spot.y, spot.width, spot.height, 12, 12);

        // Draw border with glow when hovered
        if (hovered) {
            // Glow effect
            g2.setColor(new Color(hoverColor.getRed(), hoverColor.getGreen(), hoverColor.getBlue(), 80));
            g2.setStroke(new BasicStroke(6f));
            g2.drawRoundRect(spot.x - 3, spot.y - 3, spot.width + 6, spot.height + 6, 15, 15);

            g2.setColor(hoverColor);
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(spot.x, spot.y, spot.width, spot.height, 12, 12);
        } else {
            g2.setColor(SPOT_BORDER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(spot.x, spot.y, spot.width, spot.height, 12, 12);
        }

        // Draw parking lines
        g2.setColor(new Color(180, 185, 195));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{5, 3}, 0));
        g2.drawLine(spot.x + 15, spot.y + spot.height - 15, spot.x + spot.width - 15, spot.y + spot.height - 15);
        g2.setStroke(new BasicStroke(1f));

        // Draw gate at the bottom of the spot
        drawGate(g2, spot.x + spot.width / 2, spot.y + spot.height + 5, gateAngle, hovered ? hoverColor : GATE_COLOR);

        // Draw title
        g2.setColor(hovered ? hoverColor : new Color(44, 62, 80));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2.drawString(title, spot.x + (spot.width - titleWidth) / 2, spot.y + 28);

        // Draw subtitle
        g2.setColor(new Color(100, 110, 120));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        int subWidth = g2.getFontMetrics().stringWidth(subtitle);
        g2.drawString(subtitle, spot.x + (spot.width - subWidth) / 2, spot.y + 45);

        // Draw P symbol
        g2.setColor(hovered ? hoverColor : new Color(180, 185, 195));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
        String p = "P";
        int pWidth = g2.getFontMetrics().stringWidth(p);
        g2.drawString(p, spot.x + (spot.width - pWidth) / 2, spot.y + spot.height - 12);
    }

    private void drawGate(Graphics2D g2, int centerX, int y, double angle, Color color) {
        // Draw gate posts
        g2.setColor(GATE_POST_COLOR);
        g2.fillRoundRect(centerX - 45, y - 3, 8, 16, 3, 3);
        g2.fillRoundRect(centerX + 37, y - 3, 8, 16, 3, 3);

        // Draw gate arm (rotates from horizontal to vertical when open)
        AffineTransform oldTransform = g2.getTransform();
        g2.translate(centerX - 41, y + 5);
        g2.rotate(Math.toRadians(-angle));

        // Gate arm
        g2.setColor(color);
        g2.fillRoundRect(0, -3, 82, 6, 3, 3);

        // Red/white stripes on gate
        g2.setColor(Color.WHITE);
        for (int i = 0; i < 4; i++) {
            g2.fillRect(10 + i * 20, -2, 8, 4);
        }

        g2.setTransform(oldTransform);
    }

    private void drawCar(Graphics2D g2, double x, double y, double angle) {
        AffineTransform oldTransform = g2.getTransform();

        // Rotate around car center
        g2.translate(x + CAR_WIDTH / 2.0, y + CAR_HEIGHT / 2.0);
        g2.rotate(Math.toRadians(angle));
        g2.translate(-CAR_WIDTH / 2.0, -CAR_HEIGHT / 2.0);

        // Car body
        RoundRectangle2D body = new RoundRectangle2D.Double(0, 0, CAR_WIDTH, CAR_HEIGHT, 10, 10);
        g2.setColor(CAR_BODY);
        g2.fill(body);

        // Car roof (cabin)
        int roofWidth = CAR_WIDTH - 10;
        int roofHeight = CAR_HEIGHT - 30;
        RoundRectangle2D roof = new RoundRectangle2D.Double(5, 12, roofWidth, roofHeight, 6, 6);
        g2.setColor(CAR_ROOF);
        g2.fill(roof);

        // Windows
        g2.setColor(CAR_WINDOW);
        // Front windshield
        int[] frontX = {8, CAR_WIDTH - 8, CAR_WIDTH - 10, 10};
        int[] frontY = {14, 14, 22, 22};
        g2.fillPolygon(frontX, frontY, 4);
        // Rear window
        int[] rearX = {8, CAR_WIDTH - 8, CAR_WIDTH - 10, 10};
        int[] rearY = {CAR_HEIGHT - 16, CAR_HEIGHT - 16, CAR_HEIGHT - 24, CAR_HEIGHT - 24};
        g2.fillPolygon(rearX, rearY, 4);
        // Side windows
        g2.fillRect(6, 25, 3, 20);
        g2.fillRect(CAR_WIDTH - 9, 25, 3, 20);

        // Headlights
        g2.setColor(CAR_HEADLIGHT);
        g2.fillRoundRect(5, 2, 8, 5, 2, 2);
        g2.fillRoundRect(CAR_WIDTH - 13, 2, 8, 5, 2, 2);

        // Taillights
        g2.setColor(CAR_TAILLIGHT);
        g2.fillRoundRect(5, CAR_HEIGHT - 7, 8, 5, 2, 2);
        g2.fillRoundRect(CAR_WIDTH - 13, CAR_HEIGHT - 7, 8, 5, 2, 2);

        // Wheels
        g2.setColor(new Color(25, 25, 25));
        g2.fillRoundRect(-2, 6, 5, 12, 2, 2);
        g2.fillRoundRect(CAR_WIDTH - 3, 6, 5, 12, 2, 2);
        g2.fillRoundRect(-2, CAR_HEIGHT - 18, 5, 12, 2, 2);
        g2.fillRoundRect(CAR_WIDTH - 3, CAR_HEIGHT - 18, 5, 12, 2, 2);

        g2.setTransform(oldTransform);
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }
}
