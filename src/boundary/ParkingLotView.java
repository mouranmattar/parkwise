package boundary;

import control.ParkingLotController;
import entity.ParkingLot;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ParkingLotView extends JFrame {
    private JTable lotTable;
    private DefaultTableModel tableModel;
    private JTextField nameField;
    private JTextField addressField;
    private JTextField cityField;
    private JTextField spacesField;
    private final ParkingLotController controller = new ParkingLotController();

    public ParkingLotView() {
        initializeUI();
        loadLots();
    }

    public void initializeUI() {
        setTitle("Parking Lot Management");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> loadLots());
        topPanel.add(refreshBtn);
        JButton addBtn = new JButton("Add");
        addBtn.addActionListener(e -> onAdd());
        topPanel.add(addBtn);
        JButton updateBtn = new JButton("Update Selected");
        updateBtn.addActionListener(e -> onUpdate());
        topPanel.add(updateBtn);
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> onDelete());
        topPanel.add(deleteBtn);
        add(topPanel, BorderLayout.NORTH);

        String[] columns = {"Lot ID", "Name", "Address", "City", "Spaces"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        lotTable = new JTable(tableModel);
        add(new JScrollPane(lotTable), BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formPanel.add(new JLabel("Name:"));
        nameField = new JTextField(12);
        formPanel.add(nameField);
        formPanel.add(new JLabel("Address:"));
        addressField = new JTextField(15);
        formPanel.add(addressField);
        formPanel.add(new JLabel("City:"));
        cityField = new JTextField(12);
        formPanel.add(cityField);
        formPanel.add(new JLabel("Spaces:"));
        spacesField = new JTextField(8);
        formPanel.add(spacesField);
        add(formPanel, BorderLayout.SOUTH);
    }

    public void loadLots() {
        tableModel.setRowCount(0);
        List<ParkingLot> lots = controller.getAllLots();
        for (ParkingLot lot : lots) {
            tableModel.addRow(new Object[]{
                lot.getLotNumber(),
                lot.getName(),
                lot.getAddress(),
                lot.getCity(),
                lot.getAvailableSpaces()
            });
        }
    }

    public void onAdd() {
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();
        String spacesText = spacesField.getText().trim();
        if (name.isEmpty() || address.isEmpty() || city.isEmpty() || spacesText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name, Address, City, and Spaces are required", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int spaces = Integer.parseInt(spacesText);
        controller.addLot(name, address, city, spaces);
        loadLots();
        nameField.setText("");
        addressField.setText("");
        cityField.setText("");
        spacesField.setText("");
    }

    public void onUpdate() {
        int row = lotTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a row to update", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        String name = nameField.getText().trim();
        String address = addressField.getText().trim();
        String city = cityField.getText().trim();
        String spacesText = spacesField.getText().trim();
        if (name.isEmpty() || address.isEmpty() || city.isEmpty() || spacesText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name, Address, City, and Spaces are required", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int spaces = Integer.parseInt(spacesText);
        controller.updateLot(id, name, address, city, spaces);
        loadLots();
    }

    public void onDelete() {
        int row = lotTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a row to delete", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure to delete?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        int id = Integer.parseInt(tableModel.getValueAt(row, 0).toString());
        controller.deleteLot(id);
        loadLots();
    }
}
