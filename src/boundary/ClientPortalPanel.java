package boundary;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.function.Supplier;

public class ClientPortalPanel extends JPanel {
    public interface Nav {
        void goHome();
        void goClub();
    }

    private final CardLayout clientCards = new CardLayout();
    private final JPanel cardHost = new JPanel(clientCards);
    private final CustomButton backBtn = new CustomButton("← Back", new Color(189, 195, 199));

    private ReportPaymentView checkOutPanelInstance;
    private LightPlexusBackgroundPanel plexusBackground;

    public void refreshData() {
        if (checkOutPanelInstance != null) {
            checkOutPanelInstance.refreshData();
        }
    }

    public ClientPortalPanel(Nav nav) {
        super(new BorderLayout());
        
        // Use LightPlexusBackgroundPanel as the main background
        plexusBackground = new LightPlexusBackgroundPanel();
        setLayout(new BorderLayout());
        add(plexusBackground, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(44, 62, 80));
        header.setBorder(new EmptyBorder(10, 16, 10, 16));
        header.setOpaque(true);

        // Left side: Home button
        CustomButton home = new CustomButton("Home", new Color(52, 152, 219));
        home.setPreferredSize(new Dimension(100, 32));
        home.addActionListener(e -> nav.goHome());

        // Center: Title
        JLabel label = new JLabel("Client Self-Service Portal", JLabel.CENTER);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 18));

        // Right side: Join Club button
        CustomButton joinClubBtn = new CustomButton("Join Club", new Color(46, 204, 113));
        joinClubBtn.setPreferredSize(new Dimension(120, 32));
        joinClubBtn.addActionListener(e -> nav.goClub());

        header.add(home, BorderLayout.WEST);
        header.add(label, BorderLayout.CENTER);
        header.add(joinClubBtn, BorderLayout.EAST);

        // Create transparent content panel over plexus background
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel hero = new JPanel();
        hero.setOpaque(false);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        JLabel welcome = new JLabel("Welcome to ParkWise - Touch to Start", JLabel.CENTER);
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 26));
        welcome.setForeground(new Color(44, 62, 80));
        welcome.setBorder(new EmptyBorder(0, 0, 20, 0));
        JLabel q = new JLabel("What would you like to do today?", JLabel.CENTER);
        q.setAlignmentX(Component.CENTER_ALIGNMENT);
        q.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        q.setForeground(new Color(80, 90, 100));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 10));
        buttons.setOpaque(false);
        
        // Check-in button with hover effect
        CustomButton pickCheckIn = new CustomButton("Check-in / Start Parking", new Color(52, 152, 219));
        pickCheckIn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { 
                plexusBackground.hoverCheckIn(); 
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { 
                plexusBackground.resetIdle(); 
            }
        });
        
        // Check-out button with hover effect
        CustomButton pickCheckOut = new CustomButton("Check-out / Pay", new Color(39, 174, 96));
        pickCheckOut.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { 
                plexusBackground.hoverCheckOut(); 
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { 
                plexusBackground.resetIdle(); 
            }
        });
        
        Dimension huge = new Dimension(280, 140);
        pickCheckIn.setPreferredSize(huge);
        pickCheckOut.setPreferredSize(huge);
        pickCheckIn.setFont(new Font("Segoe UI", Font.BOLD, 18));
        pickCheckOut.setFont(new Font("Segoe UI", Font.BOLD, 18));
        buttons.add(pickCheckIn);
        buttons.add(pickCheckOut);

        hero.add(Box.createVerticalGlue());
        hero.add(welcome);
        hero.add(q);
        hero.add(Box.createVerticalStrut(12));
        hero.add(buttons);
        hero.add(Box.createVerticalGlue());

        cardHost.setOpaque(false);
        JPanel intro = new JPanel(new BorderLayout());
        intro.setOpaque(false);
        intro.add(hero, BorderLayout.CENTER);

        JPanel checkInPanel = wrapFrameContent(boundary.EntrySimulationView::new, "Check-in / Kiosk");
        
        // Initialize checkOutPanelInstance to allow refreshing
        this.checkOutPanelInstance = new boundary.ReportPaymentView(boundary.ReportPaymentView.ViewMode.CLIENT_ONLY);
        JPanel checkOutPanel = wrapFrameContent(() -> checkOutPanelInstance, "Check-out / Payments");

        cardHost.add(intro, "intro");
        cardHost.add(checkInPanel, "checkin");
        cardHost.add(checkOutPanel, "checkout");

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topBar.setOpaque(false);
        backBtn.setPreferredSize(new Dimension(110, 32));
        backBtn.addActionListener(e -> {
            clientCards.show(cardHost, "intro");
            backBtn.setVisible(false);
        });
        backBtn.setVisible(false);
        topBar.add(backBtn);

        pickCheckIn.addActionListener(e -> { clientCards.show(cardHost, "checkin"); backBtn.setVisible(true); });
        pickCheckOut.addActionListener(e -> { 
            refreshData(); // Refresh before showing
            clientCards.show(cardHost, "checkout"); 
            backBtn.setVisible(true); 
        });

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(topBar, BorderLayout.NORTH);
        body.add(cardHost, BorderLayout.CENTER);

        content.add(body, BorderLayout.CENTER);
        
        // Layer the components
        plexusBackground.add(header, BorderLayout.NORTH);
        plexusBackground.add(content, BorderLayout.CENTER);
    }

    private JPanel wrapFrameContent(Supplier<JComponent> componentFactory, String title) {
        JComponent content = componentFactory.get();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(244, 247, 246));
        wrapper.setOpaque(true);
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
