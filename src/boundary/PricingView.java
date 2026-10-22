package boundary;

import control.PricingController;
import entity.PriceList;
import utils.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;

public class PricingView extends JFrame {
    private PricingController controller;
    private JTable priceTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> lotComboBox;
    private int currentLotID = 1;

    public PricingView() {
        this.controller = new PricingController();
        initializeUI();
        loadParkingLots();
        displayHistory(controller.getPriceHistory(currentLotID));
    }

    private void initializeUI() {
        setTitle("Pricing Management");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("Parking Lot:"));
        lotComboBox = new JComboBox<>();
        lotComboBox.addActionListener(e -> onLotChanged());
        topPanel.add(lotComboBox);

        JButton viewHistoryBtn = new JButton("View History");
        viewHistoryBtn.addActionListener(e -> onViewHistoryClick());
        topPanel.add(viewHistoryBtn);

        JButton importBtn = new JButton("Import Price List (JSON)");
        importBtn.addActionListener(e -> onImportPriceListClick());
        topPanel.add(importBtn);

        add(topPanel, BorderLayout.NORTH);

        String[] columnNames = {"List ID", "Year", "First Hour ($)", "Additional Hour ($)", "Full Day ($)", "Valid From"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        priceTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(priceTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JLabel infoLabel = new JLabel("Import JSON files to add new price lists");
        bottomPanel.add(infoLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadParkingLots() {
        List<entity.ParkingLot> lots = DatabaseConnection.getAllParkingLots();
        lotComboBox.removeAllItems();
        for (entity.ParkingLot lot : lots) {
            lotComboBox.addItem(lot.getLotNumber() + " - " + lot.getName());
        }
    }

    private void onLotChanged() {
        String selected = (String) lotComboBox.getSelectedItem();
        if (selected != null) {
            currentLotID = Integer.parseInt(selected.split(" - ")[0]);
            onViewHistoryClick();
        }
    }

    public void onViewHistoryClick() {
        List<PriceList> prices = controller.getPriceHistory(currentLotID);
        displayHistory(prices);
    }

    public void onImportPriceListClick() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".json");
            }

            @Override
            public String getDescription() {
                return "JSON Files (*.json)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            controller.importPriceList(selectedFile.getAbsolutePath(), currentLotID);
            JOptionPane.showMessageDialog(this, "Price list imported successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            onViewHistoryClick();
        }
    }

    public void displayHistory(List<PriceList> prices) {
        tableModel.setRowCount(0);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        for (PriceList price : prices) {
            Object[] row = {
                price.getListID(),
                price.getYear(),
                String.format("%.2f", price.getFirstHourPrice()),
                String.format("%.2f", price.getAdditionalHourPrice()),
                String.format("%.2f", price.getFullDayPrice()),
                dateFormat.format(price.getValidFrom())
            };
            tableModel.addRow(row);
        }
    }
}
