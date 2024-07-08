package com.store;

import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.dbcon.dbcon;

public class vendor {
    private dbcon db = new dbcon();
    private Connection con = db.getConnection();
    private int vendorId;
    private String name;
    private double pendingAmount = 0.0;

    public vendor() {
        updatePendingAmount();
    }

    public vendor(int vendorId, String name) {
        this.vendorId = vendorId;
        this.name = name;
        updatePendingAmount();
    }

    public int getVendorId() {
        return vendorId;
    }

    public void setVendorId(int vendorId) {
        this.vendorId = vendorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(double pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    public void updatePendingAmount() {
        String sumSql = "SELECT COALESCE(SUM(pendingamount), 0) AS totalPending FROM bill WHERE vendor = ?";
        String updateSql = "UPDATE vendors SET pendingamount = ? WHERE vendorname = ?";

        try (PreparedStatement sumStmt = con.prepareStatement(sumSql);
             PreparedStatement updateStmt = con.prepareStatement(updateSql)) {

            sumStmt.setString(1, this.name);
            ResultSet rs = sumStmt.executeQuery();

            if (rs.next()) {
                double totalPending = rs.getDouble("totalPending");
                setPendingAmount(totalPending);

                updateStmt.setDouble(1, totalPending);
                updateStmt.setString(2, this.name);
                updateStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void displayBills() {
        String sql = "SELECT billid, amount, pendingamount, billdate, vendor FROM bill WHERE vendor = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, this.name);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                bill nbill = new bill();
                nbill.setBillid(rs.getInt("billid"));
                nbill.setAmount(rs.getFloat("amount"));
                nbill.setPendingAmount(rs.getFloat("pendingamount"));
                nbill.setBillDate(rs.getDate("billdate"));
                nbill.setVendor(rs.getString("vendor"));
                System.out.println(nbill);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete() {
        try {
            con.setAutoCommit(false);

            String sql = "DELETE FROM share WHERE billid IN (SELECT billid FROM bill WHERE vendor = ?)";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, this.name);
                stmt.executeUpdate();
            }

            sql = "DELETE FROM bill WHERE vendor = ?";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, this.name);
                stmt.executeUpdate();
            }

            sql = "DELETE FROM transaction WHERE vendor = ?";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, this.name);
                stmt.executeUpdate();
            }

            sql = "DELETE FROM vendors WHERE vendorname = ?";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setString(1, this.name);
                stmt.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public void addBill(bill nbill) {
        String sql = "INSERT INTO bill (amount, pendingamount, billdate, vendor) VALUES (?, ?, ?, ?)";
        String generatedColumns[] = { "billid" };
        try (PreparedStatement stmt = con.prepareStatement(sql, generatedColumns)) {
            stmt.setFloat(1, nbill.getAmount());
            stmt.setFloat(2, nbill.getPendingAmount());
            stmt.setDate(3, new java.sql.Date(nbill.getBillDate().getTime()));
            stmt.setString(4, this.name);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int billId = rs.getInt(1);
                nbill.setBillid(billId);
                setPendingAmount(getPendingAmount() + nbill.getPendingAmount());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteBill(int billId) {
        try {
            con.setAutoCommit(false);

            String sql = "SELECT amount FROM bill WHERE billid = ?";
            float billAmount = 0;

            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, billId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    billAmount = rs.getFloat("amount");
                }
            }

            sql = "DELETE FROM share WHERE billid = ?";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, billId);
                stmt.executeUpdate();
            }

            sql = "DELETE FROM bill WHERE billid = ? AND vendor = ?";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setInt(1, billId);
                stmt.setString(2, name);
                stmt.executeUpdate();
            }

            sql = "UPDATE transaction SET totalamount = totalamount - ? WHERE vendor = ?";
            try (PreparedStatement stmt = con.prepareStatement(sql)) {
                stmt.setFloat(1, billAmount);
                stmt.setString(2, name);
                stmt.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        updatePendingAmount();
    }

    /* Operation on Transaction */
    public void showAllTransactions() {
        String sql = "SELECT transactionid, transactiondate, totalamount, vendor FROM transaction WHERE vendor = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, this.name);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transaction ntransaction = new transaction();
                ntransaction.setTransactionId(rs.getInt("transactionid"));
                ntransaction.setTotalAmount(rs.getFloat("totalamount"));
                ntransaction.setVendor(rs.getString("vendor"));
                ntransaction.setTransactionDate(rs.getDate("transactiondate"));
                getSharesFromDB(ntransaction);
                System.out.println(ntransaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void getSharesFromDB(transaction ntransaction) {
        String sql = "SELECT billid, amount FROM share WHERE transactionid = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, ntransaction.getTransactionId());

            ResultSet rs = stmt.executeQuery();
            ;
            while (rs.next()) {
                int billId = rs.getInt("billid");
                float amount = rs.getFloat("amount");
                ntransaction.addShare(billId, amount);

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addTransaction(transaction ntransaction) {
        String sql = "INSERT INTO transaction (totalAmount, vendor, transactionDate) VALUES (?, ?, ?)";
        String generatedColumns[] = { "transactionId" };
        try (PreparedStatement stmt = con.prepareStatement(sql, generatedColumns)) {
            stmt.setDouble(1, ntransaction.getTotalAmount());
            stmt.setString(2, this.name);
            stmt.setDate(3, new java.sql.Date(ntransaction.getTransactionDate().getTime()));
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int transactionId = rs.getInt(1);
                ntransaction.setTransactionId(transactionId);
            }

            insertShare(ntransaction);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertShare(transaction ntransaction) throws SQLException {
        String sql = "INSERT INTO share (billid, transactionid, amount) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            share[] s = ntransaction.getShares();
            int sharecnt = ntransaction.shareCount;
            for (int i = 0; i < sharecnt; i++) {
                stmt.setInt(1, s[i].getBillId());
                stmt.setInt(2, ntransaction.getTransactionId());
                stmt.setFloat(3, s[i].getAmount());
                stmt.executeUpdate();

                // Update pending amount of bill
                sql = "UPDATE bill SET pendingamount = pendingamount - ? WHERE billid = ?";
                try (PreparedStatement updateStmt = con.prepareStatement(sql)) {
                    updateStmt.setFloat(1, s[i].getAmount());
                    updateStmt.setInt(2, s[i].getBillId());
                    updateStmt.executeUpdate();
                }
            }
        }

    }

    public void removeTransaction(int transactionId) {
        String sql = "DELETE FROM transaction WHERE transactionId = ? AND vendor = ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            stmt.setString(2, this.name);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                sql = "SELECT billid, amount FROM share WHERE transactionid = ?";
                try (PreparedStatement selectStmt = con.prepareStatement(sql)) {
                    selectStmt.setInt(1, transactionId);
                    ResultSet rs = selectStmt.executeQuery();

                    while (rs.next()) {
                        int billId = rs.getInt("billid");
                        float amount = rs.getFloat("amount");

                        sql = "UPDATE bill SET pendingamount = pendingamount + ? WHERE billid = ?";
                        try (PreparedStatement updateStmt = con.prepareStatement(sql)) {
                            updateStmt.setFloat(1, amount);
                            updateStmt.setInt(2, billId);
                            updateStmt.executeUpdate();
                        }
                    }

                    sql = "DELETE FROM share WHERE transactionid = ?";
                    try (PreparedStatement deleteShareStmt = con.prepareStatement(sql)) {
                        deleteShareStmt.setInt(1, transactionId);
                        deleteShareStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        updatePendingAmount();
    }

    // Function to show transactions by date range for all vendors
    public void showAllTransactions(Date sdate, Date edate) {
        String sql = "SELECT transactionId, totalAmount, vendor, transactionDate FROM transaction WHERE vendor = ? AND transactionDate BETWEEN ? AND ?";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, this.name);
            stmt.setDate(2, sdate);
            stmt.setDate(3, edate);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transaction ntransaction = new transaction();
                ntransaction.setTransactionId(rs.getInt("transactionId"));
                ntransaction.setTotalAmount(rs.getDouble("totalAmount"));
                ntransaction.setVendor(rs.getString("vendor"));
                ntransaction.setTransactionDate(rs.getDate("transactionDate"));
                getSharesFromDB(ntransaction);
                System.out.println(ntransaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Function to show transactions of previous month for all vendors
    public void showAllTransactionsPMonth() {
        String sql = "SELECT transactionId, totalAmount, vendor, transactionDate FROM transaction WHERE vendor = ? AND MONTH(transactionDate) = MONTH(CURRENT_DATE - INTERVAL 1 MONTH) AND YEAR(transactionDate) = YEAR(CURRENT_DATE - INTERVAL 1 MONTH)";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, this.name);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transaction ntransaction = new transaction();
                ntransaction.setTransactionId(rs.getInt("transactionId"));
                ntransaction.setTotalAmount(rs.getDouble("totalAmount"));
                ntransaction.setVendor(rs.getString("vendor"));
                ntransaction.setTransactionDate(rs.getDate("transactionDate"));
                getSharesFromDB(ntransaction);
                System.out.println(ntransaction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void menu() {
        Scanner in = new Scanner(System.in);
        int choice = 0;

        do {
            System.out.println("\nVendor Menu");
            System.out.println("1. Display Bills");
            System.out.println("2. Add Bill");
            System.out.println("3. Delete Bill");
            System.out.println("4. Display All Transactions");
            System.out.println("5. Add Transaction");
            System.out.println("6. Remove Transaction");
            System.out.println("7. Display Transactions within a Date Range");
            System.out.println("8. Display Previous Month's Transactions");
            System.out.println("9. Exit");
            System.out.print("Enter your choice: ");
            choice = in.nextInt();

            switch (choice) {
                case 1:
                    displayBills();
                    break;
                case 2:
                    System.out.println("Enter bill details:");
                    System.out.print("Amount: ");
                    float amount = in.nextFloat();
                    float pendingAmount = amount;
                    System.out.print("Bill Date (YYYY-MM-DD): ");
                    String dateStr = in.next();

                    Date billDate = Date.valueOf(dateStr);

                    bill nbill = new bill();
                    nbill.setAmount(amount);
                    nbill.setPendingAmount(pendingAmount);
                    nbill.setBillDate(billDate);
                    nbill.setVendor(this.name);

                    addBill(nbill);

                    System.out.println("Bill added successfully.");
                    break;
                case 3:
                    System.out.print("Enter Bill ID to delete: ");
                    int billId = in.nextInt();
                    deleteBill(billId);
                    break;
                case 4:
                    showAllTransactions();
                    break;
                case 5:
                    System.out.println("Enter transaction details:");

                    System.out.print("Transaction Date (YYYY-MM-DD): ");
                    dateStr = in.next();

                    java.sql.Date transactionDate = java.sql.Date.valueOf(dateStr);

                    transaction ntransaction = new transaction();
                    System.out.println("Enter number of bills to be added: ");
                    int count = in.nextInt();
                    for (int i = 0; i < count; i++) {
                        System.out.print("Enter bill ID: ");
                        int id = in.nextInt();
                        System.out.print("Enter bill amount: ");
                        float amountShare = in.nextFloat();
                        ntransaction.addShare(id, amountShare);
                    }
                    ntransaction.setVendor(name);
                    ntransaction.setTransactionDate(transactionDate);
                    addTransaction(ntransaction);

                    System.out.println("Transaction added successfully.");
                    break;
                case 6:
                    System.out.print("Enter Transaction ID to remove: ");
                    int transactionId = in.nextInt();
                    removeTransaction(transactionId);
                    break;
                case 7:
                    System.out.print("Enter start date (yyyy-mm-dd): ");
                    String startDateStr = in.next();
                    System.out.print("Enter end date (yyyy-mm-dd): ");
                    String endDateStr = in.next();

                    try {
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                        Date startDate = new Date(format.parse(startDateStr).getTime());
                        Date endDate = new Date(format.parse(endDateStr).getTime());
                        showAllTransactions(startDate, endDate);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    break;
                case 8:
                    showAllTransactionsPMonth();
                    break;
                case 9:
                    System.out.println("Exiting Vendor Menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a valid option.");
                    break;
            }
        } while (choice != 9);

        in.close();
    }
}
