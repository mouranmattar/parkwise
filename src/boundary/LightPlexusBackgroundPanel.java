package boundary;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * LightPlexusBackgroundPanel - Cyberpunk style neon background with animated cars.
 * BCE Boundary Layer: Custom JPanel for Client Portal background visualization.
 * Features: Grid lines, scanlines, animated 2D cars traveling across screen on hover.
 */
public class LightPlexusBackgroundPanel extends JPanel {

    // Animation constants
    private static final int FPS = 60;
    private static final int FRAME_DELAY = 1000 / FPS;

    // Cyberpunk colors
    private static final Color CYBER_CYAN = new Color(0, 255, 255);
    private static final Color CYBER_GREEN = new Color(0, 255, 150);
    private static final Color GRID_COLOR = new Color(230, 235, 240);
    private static final Color SCANLINE_COLOR = new Color(200, 210, 220, 30);

    // Animated car class
    private static class AnimatedCar {
        double x, y;
        double speed;
        int direction; // 1 = right, -1 = left
        Color bodyColor;
        Color glowColor;
        boolean active = true;
        double scale;

        AnimatedCar(double x, double y, double speed, int direction, Color bodyColor, Color glowColor) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.direction = direction;
            this.bodyColor = bodyColor;
            this.glowColor = glowColor;
            this.scale = 0.4 + Math.random() * 0.2; // Random size
        }

        void update(int width) {
            x += speed * direction;
            // Deactivate when off screen
            if (direction == 1 && x > width + 100) {
                active = false;
            } else if (direction == -1 && x < -100) {
                active = false;
            }
        }

        void draw(Graphics2D g2) {
            AffineTransform oldTransform = g2.getTransform();

            // Position and scale
            g2.translate(x, y);
            g2.scale(scale, scale);

            // Flip if going left
            if (direction == -1) {
                g2.scale(-1, 1);
            }

            // Car dimensions
            int carWidth = 60;
            int carHeight = 30;

            // Glow effect under car
            g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 60));
            g2.fillRoundRect(-carWidth/2 - 5, -carHeight/2 + 5, carWidth + 10, carHeight + 10, 15, 15);

            // Car body
            g2.setColor(bodyColor);
            g2.fillRoundRect(-carWidth/2, -carHeight/2, carWidth, carHeight, 10, 10);

            // Car roof
            g2.setColor(bodyColor.darker());
            g2.fillRoundRect(-carWidth/4, -carHeight/2 - 8, carWidth/2, carHeight/2 + 5, 8, 8);

            // Windows
            g2.setColor(new Color(200, 230, 255, 200));
            g2.fillRoundRect(-carWidth/4 + 3, -carHeight/2 - 5, carWidth/2 - 6, carHeight/3, 5, 5);

            // Headlights
            g2.setColor(new Color(255, 255, 200));
            g2.fillOval(carWidth/2 - 8, -carHeight/4, 6, 6);
            g2.fillOval(carWidth/2 - 8, carHeight/4 - 6, 6, 6);

            // Headlight glow
            g2.setColor(new Color(255, 255, 200, 100));
            g2.fillOval(carWidth/2 - 2, -carHeight/3, 20, carHeight/2 + carHeight/3);

            // Taillights
            g2.setColor(new Color(255, 50, 50));
            g2.fillOval(-carWidth/2 + 2, -carHeight/4, 5, 5);
            g2.fillOval(-carWidth/2 + 2, carHeight/4 - 5, 5, 5);

            // Wheels
            g2.setColor(new Color(40, 40, 40));
            g2.fillRoundRect(-carWidth/2 + 5, carHeight/2 - 5, 14, 8, 3, 3);
            g2.fillRoundRect(carWidth/2 - 19, carHeight/2 - 5, 14, 8, 3, 3);

            // Neon underglow
            g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 150));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(-carWidth/2, -carHeight/2, carWidth, carHeight, 10, 10);

            g2.setTransform(oldTransform);
        }
    }

    // State
    private final List<AnimatedCar> cars = new ArrayList<>();
    private final Random random = new Random();

    // Hover states
    private boolean checkInHovered = false;
    private boolean checkOutHovered = false;

    // Animation timer
    private Timer animationTimer;

    // Glow intensity
    private double cyanIntensity = 0;
    private double greenIntensity = 0;
    private double targetCyanIntensity = 0;
    private double targetGreenIntensity = 0;

    public LightPlexusBackgroundPanel() {
        setBackground(Color.WHITE);
        setLayout(new BorderLayout());
        setOpaque(true);

        animationTimer = new Timer(FRAME_DELAY, e -> animate());
        animationTimer.start();
    }

    public void hoverCheckIn() {
        checkInHovered = true;
        targetCyanIntensity = 1.0;
        spawnCars(true);
    }

    public void hoverCheckOut() {
        checkOutHovered = true;
        targetGreenIntensity = 1.0;
        spawnCars(false);
    }

    public void resetIdle() {
        checkInHovered = false;
        checkOutHovered = false;
        targetCyanIntensity = 0;
        targetGreenIntensity = 0;
    }

    private void spawnCars(boolean isCyan) {
        int width = getWidth();
        int height = getHeight();

        Color bodyColor = isCyan ? new Color(0, 150, 200) : new Color(0, 150, 100);
        Color glowColor = isCyan ? CYBER_CYAN : CYBER_GREEN;

        // Spawn car from left or right
        int direction = isCyan ? 1 : -1; // Cyan cars go right, Magenta cars go left
        double startX = isCyan ? -80 : width + 80;
        double y = height * 0.2 + random.nextDouble() * height * 0.6;
        double speed = 4 + random.nextDouble() * 3;

        cars.add(new AnimatedCar(startX, y, speed, direction, bodyColor, glowColor));
    }

    private void animate() {
        int width = getWidth();

        double fadeSpeed = 0.1;
        cyanIntensity += (targetCyanIntensity - cyanIntensity) * fadeSpeed;
        greenIntensity += (targetGreenIntensity - greenIntensity) * fadeSpeed;

        // Update cars
        cars.removeIf(car -> {
            car.update(width);
            return !car.active;
        });

        // Spawn new cars periodically when hovered
        if ((checkInHovered || checkOutHovered) && random.nextDouble() < 0.03) {
            spawnCars(checkInHovered);
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // White background
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        // Draw grid
        drawGrid(g2, width, height);

        // Draw scanlines
        drawScanlines(g2, width, height);

        // Draw animated cars
        drawCars(g2);

        // Draw corner accents
        drawCornerAccents(g2, width, height);

        g2.dispose();
    }

    private void drawGrid(Graphics2D g2, int width, int height) {
        g2.setColor(GRID_COLOR);
        g2.setStroke(new BasicStroke(0.5f));

        // Vertical lines
        int gridSpacing = 60;
        for (int x = 0; x < width; x += gridSpacing) {
            boolean isLeft = x < width / 2;
            double intensity = isLeft ? cyanIntensity : greenIntensity;

            if (intensity > 0.01) {
                Color glowColor = isLeft ? CYBER_CYAN : CYBER_GREEN;
                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), (int) (60 * intensity)));
                g2.setStroke(new BasicStroke(3f));
                g2.draw(new Line2D.Double(x, 0, x, height));
                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), (int) (150 * intensity)));
                g2.setStroke(new BasicStroke(1f));
            }
            g2.draw(new Line2D.Double(x, 0, x, height));
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(0.5f));
        }

        // Horizontal lines
        for (int y = 0; y < height; y += gridSpacing) {
            boolean isTop = y < height / 2;
            double intensity = isTop ? cyanIntensity : greenIntensity;

            if (intensity > 0.01) {
                Color glowColor = isTop ? CYBER_CYAN : CYBER_GREEN;
                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), (int) (60 * intensity)));
                g2.setStroke(new BasicStroke(3f));
                g2.draw(new Line2D.Double(0, y, width, y));
                g2.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), (int) (150 * intensity)));
                g2.setStroke(new BasicStroke(1f));
            }
            g2.draw(new Line2D.Double(0, y, width, y));
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(0.5f));
        }
    }

    private void drawScanlines(Graphics2D g2, int width, int height) {
        g2.setColor(SCANLINE_COLOR);
        for (int y = 0; y < height; y += 4) {
            g2.drawLine(0, y, width, y);
        }
    }

    private void drawCars(Graphics2D g2) {
        for (AnimatedCar car : cars) {
            car.draw(g2);
        }
    }

    private void drawCornerAccents(Graphics2D g2, int width, int height) {
        int bracketSize = 40;
        int offset = 20;

        drawBracket(g2, offset, offset, bracketSize, 0, cyanIntensity > 0.01 ? CYBER_CYAN : GRID_COLOR.darker());
        drawBracket(g2, width - offset, offset, bracketSize, 1, greenIntensity > 0.01 ? CYBER_GREEN : GRID_COLOR.darker());
        drawBracket(g2, offset, height - offset, bracketSize, 3, cyanIntensity > 0.01 ? CYBER_CYAN : GRID_COLOR.darker());
        drawBracket(g2, width - offset, height - offset, bracketSize, 2, greenIntensity > 0.01 ? CYBER_GREEN : GRID_COLOR.darker());
    }

    private void drawBracket(Graphics2D g2, int x, int y, int size, int corner, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f));

        int halfSize = size / 2;
        switch (corner) {
            case 0:
                g2.drawPolyline(new int[]{x - halfSize, x - halfSize, x}, new int[]{y, y - halfSize, y - halfSize}, 3);
                break;
            case 1:
                g2.drawPolyline(new int[]{x + halfSize, x + halfSize, x}, new int[]{y, y - halfSize, y - halfSize}, 3);
                break;
            case 2:
                g2.drawPolyline(new int[]{x + halfSize, x + halfSize, x}, new int[]{y, y + halfSize, y + halfSize}, 3);
                break;
            case 3:
                g2.drawPolyline(new int[]{x - halfSize, x - halfSize, x}, new int[]{y, y + halfSize, y + halfSize}, 3);
                break;
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }
}
