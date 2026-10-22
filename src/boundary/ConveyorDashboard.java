package boundary;

import control.ConveyorController;
import control.AllocationService;
import control.AllocationService.AllocationRecord;
import entity.Conveyor;
import entity.ParkingLot;
import utils.DatabaseConnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ConveyorDashboard extends JFrame {
    private ConveyorController controller;
    private JTable conveyorTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> lotComboBox;
    private int currentLotID = 1;
    private JTable allocationTable;
    private DefaultTableModel allocationModel;

    public ConveyorDashboard() {
        this.controller = new ConveyorController();
        initializeUI();
        loadParkingLots();
        updateConveyorList(DatabaseConnection.getConveyorsByLot(currentLotID));
    }

    private void initializeUI() {
        setTitle("Conveyor Management Dashboard");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        topPanel.add(new JLabel("Parking Lot:"));
        lotComboBox = new JComboBox<>();
        lotComboBox.addActionListener(e -> onLotChanged());
        topPanel.add(lotComboBox);

        JButton turnOnBtn = new JButton("Turn On Conveyors");
        turnOnBtn.addActionListener(e -> onTurnOnConveyorsClick());
        topPanel.add(turnOnBtn);

        JButton turnOffBtn = new JButton("Turn Off Conveyors");
        turnOffBtn.addActionListener(e -> onTurnOffConveyorsClick());
        topPanel.add(turnOffBtn);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshConveyorList());
        topPanel.add(refreshBtn);

        add(topPanel, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Floor", "Position (X,Y)", "Max Weight (kg)", "Status", "Failed Attempts"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        conveyorTable = new JTable(tableModel);
        conveyorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(conveyorTable);
        add(scrollPane, BorderLayout.CENTER);

        // Allocation tracking panel
        String[] allocCols = {"Vehicle", "Conveyor", "Spot", "Floor", "Status"};
        allocationModel = new DefaultTableModel(allocCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        allocationTable = new JTable(allocationModel);
        allocationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane allocScroll = new JScrollPane(allocationTable);
        allocScroll.setBorder(BorderFactory.createTitledBorder("Active Allocations"));
        add(allocScroll, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        JButton restartBtn = new JButton("Restart Selected Conveyor");
        restartBtn.addActionListener(e -> onRestartConveyorClick());
        bottomPanel.add(restartBtn);

        JButton updateWeightBtn = new JButton("Update Max Weight");
        updateWeightBtn.addActionListener(e -> onUpdateMaxWeightClick());
        bottomPanel.add(updateWeightBtn);

        JButton confirmWeightBtn = new JButton("Confirm Weight Change");
        confirmWeightBtn.addActionListener(e -> onConfirmWeightChange());
        bottomPanel.add(confirmWeightBtn);

        JButton refreshAllocBtn = new JButton("Refresh Allocations");
        refreshAllocBtn.addActionListener(e -> refreshAllocations());
        bottomPanel.add(refreshAllocBtn);

        JButton markParkedBtn = new JButton("Mark Parked");
        markParkedBtn.addActionListener(e -> onMarkParked());
        bottomPanel.add(markParkedBtn);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void loadParkingLots() {
        List<ParkingLot> lots = DatabaseConnection.getAllParkingLots();
        lotComboBox.removeAllItems();
        for (ParkingLot lot : lots) {
            lotComboBox.addItem(lot.getLotNumber() + " - " + lot.getName());
        }
    }

    private void onLotChanged() {
        String selected = (String) lotComboBox.getSelectedItem();
        if (selected != null) {
            currentLotID = Integer.parseInt(selected.split(" - ")[0]);
            refreshConveyorList();
        }
    }

    public void onTurnOnConveyorsClick() {
        controller.activateAllConveyors(currentLotID);
        showAlert("Activating all conveyors for Parking Lot #" + currentLotID);
        
        Timer timer = new Timer(2000, e -> refreshConveyorList());
        timer.setRepeats(false);
        timer.start();
    }

    public void onTurnOffConveyorsClick() {
        controller.deactivateAllConveyors(currentLotID);
        showAlert("Deactivating all conveyors for Parking Lot #" + currentLotID);
        refreshConveyorList();
    }

    public void onRestartConveyorClick() {
        int selectedRow = conveyorTable.getSelectedRow();
        if (selectedRow >= 0) {
            int conveyorID = (int) tableModel.getValueAt(selectedRow, 0);
            controller.handleRestart(conveyorID);
            showAlert("Restarting Conveyor #" + conveyorID);
            
            Timer timer = new Timer(2000, e -> refreshConveyorList());
            timer.setRepeats(false);
            timer.start();
        } else {
            showAlert("Please select a conveyor first");
        }
    }

    public void onUpdateMaxWeightClick() {
        int selectedRow = conveyorTable.getSelectedRow();
        if (selectedRow >= 0) {
            int conveyorID = (int) tableModel.getValueAt(selectedRow, 0);
            String input = JOptionPane.showInputDialog(this, "Enter new max weight (kg):", "Update Max Weight", JOptionPane.QUESTION_MESSAGE);
            
            if (input != null && !input.trim().isEmpty()) {
                try {
                    double newWeight = Double.parseDouble(input);
                    controller.changeMaxWeight(conveyorID, newWeight);
                    showAlert("Max weight updated for Conveyor #" + conveyorID + ". Please confirm the change.");
                } catch (NumberFormatException e) {
                    showAlert("Invalid weight value");
                }
            }
        } else {
            showAlert("Please select a conveyor first");
        }
    }

    public void onConfirmWeightChange() {
        int selectedRow = conveyorTable.getSelectedRow();
        if (selectedRow >= 0) {
            int conveyorID = (int) tableModel.getValueAt(selectedRow, 0);
            controller.confirmWeightChange(conveyorID);
            showAlert("Weight change confirmed for Conveyor #" + conveyorID);
            refreshConveyorList();
        } else {
            showAlert("Please select a conveyor first");
        }
    }

    public void showAlert(String message) {
        JOptionPane.showMessageDialog(this, message, "Alert", JOptionPane.INFORMATION_MESSAGE);
    }

    public void updateConveyorList(List<Conveyor> conveyors) {
        tableModel.setRowCount(0);
        
        for (Conveyor conveyor : conveyors) {
            Object[] row = {
                conveyor.getId(),
                conveyor.getCurrentFloor(),
                "(" + conveyor.getCurrentX() + "," + conveyor.getCurrentY() + ")",
                conveyor.getMaxWeight(),
                conveyor.getStatus().name(),
                conveyor.getFailedAttempts()
            };
            tableModel.addRow(row);
        }
    }

    private void refreshConveyorList() {
        List<Conveyor> conveyors = DatabaseConnection.getConveyorsByLot(currentLotID);
        updateConveyorList(conveyors);
        refreshAllocations();
    }

    private void refreshAllocations() {
        allocationModel.setRowCount(0);
        for (AllocationRecord r : AllocationService.getActiveAllocations()) {
            allocationModel.addRow(new Object[]{r.vehicleNumber, r.conveyorId, r.spotId, r.floor, r.status});
        }
    }

    private void onMarkParked() {
        int row = allocationTable.getSelectedRow();
        if (row < 0) {
            showAlert("Select a vehicle in allocations first");
            return;
        }
        int vehicle = (int) allocationModel.getValueAt(row, 0);
        AllocationService.markParked(vehicle);
        refreshAllocations();
    }

    public ConveyorController getController() {
        return controller;
    }
}
