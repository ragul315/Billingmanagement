package com.store;

import java.sql.*;
import java.sql.Date;
import java.text.*;
import java.util.*;
import com.dbcon.dbcon;

public class vendor {
    private int vendorId;
    private String name;
    private double pendingAmount;
    private bill[] bills;
    private int billCount;

    public void setBills(bill[] bills) {
        this.bills = bills;
    }

    public transaction[] getTransactions() {
        return transactions;
    }

    public void setTransactions(transaction[] transactions) {
        this.transactions = transactions;
    }

    private transaction[] transactions;
    private int transactionCount;

    public vendor(int vendorId, String name) {
        this.vendorId = vendorId;
        this.name = name;
        pendingAmount = 0.0;
        bills = new bill[10];
        billCount = 0;
        transactions = new transaction[10]; 
        transactionCount = 0;
        getBillsFromDB();
        getTransactionsFromDB();
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

    public bill[] getBills() {
        getBillsFromDB();
        return bills;
    }

    public void getBillsFromDB() {
        billCount = 0;
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "SELECT billid, amount, pendingamount, billdate, vendor FROM bill WHERE vendor = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, this.name);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                bill bill = new bill();
                bill.setBillid(rs.getInt("billid"));
                bill.setAmount(rs.getFloat("amount"));
                bill.setPendingAmount(rs.getFloat("pendingamount"));
                bill.setBillDate(rs.getDate("billdate"));
                bill.setVendor(rs.getString("vendor"));

                if (billCount == bills.length) {
                    bills = resizeArray(bills, bills.length * 2);
                }
                bills[billCount++] = bill;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }

        updatePendingAmount();
    }

    private void updatePendingAmount() {
        double totalPending = 0.0;
        for (int i = 0; i < billCount; i++) {
            totalPending += bills[i].getPendingAmount();
        }
        setPendingAmount(totalPending);
    }

    public void delete() {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "DELETE FROM bill WHERE vendor = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, this.name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }
    }

    public void addBill(bill newBill) {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "INSERT INTO bill (amount, pendingamount, billdate, vendor) VALUES (?, ?, ?, ?)";
        String generatedColumns[] = { "billid" };

        try (PreparedStatement stmt = con.prepareStatement(sql, generatedColumns)) {
            stmt.setFloat(1, newBill.getAmount());
            stmt.setFloat(2, newBill.getPendingAmount());
            stmt.setDate(3, new java.sql.Date(newBill.getBillDate().getTime()));
            stmt.setString(4, this.name);
            stmt.executeUpdate();

            // Retrieve the generated billid
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int billId = rs.getInt(1);
                newBill.setBillid(billId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }

        if (billCount == bills.length) {
            bills = resizeArray(bills, billCount * 2);
        }
        bills[billCount++] = newBill;
        updatePendingAmount();
    }

    public void deleteBill(int billId) {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
    
        try {
            con.setAutoCommit(false); // Start transaction
    
            // Retrieve the amount of the bill to be deleted
            String getBillAmountSql = "SELECT amount FROM bill WHERE billid = ?";
            float billAmount = 0;
            
            try (PreparedStatement stmt = con.prepareStatement(getBillAmountSql)) {
                stmt.setInt(1, billId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    billAmount = rs.getFloat("amount");
                }
            }
    
            // Delete from share table
            String deleteShareSql = "DELETE FROM share WHERE billid = ?";
            try (PreparedStatement stmt = con.prepareStatement(deleteShareSql)) {
                stmt.setInt(1, billId);
                stmt.executeUpdate();
            }
    
            // Delete from bill table
            String deleteBillSql = "DELETE FROM bill WHERE billid = ? AND vendor = ?";
            try (PreparedStatement stmt = con.prepareStatement(deleteBillSql)) {
                stmt.setInt(1, billId);
                stmt.setString(2, name);
                stmt.executeUpdate();
            }
    
            // Update transactions table by subtracting the bill amount
            String updateTransactionSql = "UPDATE transaction SET amount = totalamount - ? WHERE vendor = ?";
            try (PreparedStatement stmt = con.prepareStatement(updateTransactionSql)) {
                stmt.setFloat(1, billAmount);
                stmt.setString(2, name);
                stmt.executeUpdate();
            }
    
            con.commit(); // Commit transaction
        } catch (SQLException e) {
            try {
                con.rollback(); // Rollback transaction in case of error
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                con.setAutoCommit(true); // Restore default auto-commit mode
                db.closecon();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    
        int index = -1;
        for (int i = 0; i < billCount; i++) {
            if (bills[i].getBillid() == billId) {
                index = i;
                break;
            }
        }
    
        if (index != -1) {
            for (int i = index; i < billCount - 1; i++) {
                bills[i] = bills[i + 1];
            }
            bills[--billCount] = null;
        }
    
        updatePendingAmount();
    }
    
    private bill[] resizeArray(bill[] array, int newSize) {
        bill[] newArray = new bill[newSize];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    private transaction[] resizeArray(transaction[] array, int newSize) {
        transaction[] newArray = new transaction[newSize];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public void getTransactionsFromDB() {
        transactionCount = 0;
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "SELECT transactionId, totalAmount, vendor, transactionDate FROM transaction WHERE vendor = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setString(1, this.name);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                transaction transaction = new transaction();
                transaction.setTransactionId(rs.getInt("transactionId"));
                transaction.setTotalAmount(rs.getDouble("totalAmount"));
                transaction.setVendor(rs.getString("vendor"));
                transaction.setTransactionDate(rs.getDate("transactionDate"));

                // Fetch shares for this transaction
                getSharesFromDB(transaction);

                if (transactionCount == transactions.length) {
                    transactions = resizeArray(transactions, transactions.length * 2);
                }
                transactions[transactionCount++] = transaction;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }
    }

    private void getSharesFromDB(transaction transaction) {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "SELECT billid, amount FROM share WHERE transactionid = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, transaction.getTransactionId());

            ResultSet rs = stmt.executeQuery();
            List<share> shares = new ArrayList<>();
            while (rs.next()) {
                int billId = rs.getInt("billid");
                float amount = rs.getFloat("amount");
                share share = new share(billId, amount);
                shares.add(share);
            }
            transaction.setShares(shares.toArray(new share[0]));
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }
    }

    // Method to add a transaction
    public void addTransaction(transaction newTransaction) {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "INSERT INTO transaction (totalAmount, vendor, transactionDate) VALUES (?, ?, ?)";
        String generatedColumns[] = { "transactionId" };

        try (PreparedStatement stmt = con.prepareStatement(sql, generatedColumns)) {
            stmt.setDouble(1, newTransaction.getTotalAmount());
            stmt.setString(2, this.name);
            stmt.setDate(3, new java.sql.Date(newTransaction.getTransactionDate().getTime()));
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int transactionId = rs.getInt(1);
                newTransaction.setTransactionId(transactionId);
            }

            // Insert shares into share table
            insertShare(newTransaction, con);

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }

        if (transactionCount == transactions.length) {
            transactions = resizeArray(transactions, transactionCount * 2);
        }
        transactions[transactionCount++] = newTransaction;
    }

    public void removeTransaction(int transactionId) {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "DELETE FROM transaction WHERE transactionId = ? AND vendor = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, transactionId);
            stmt.setString(2, this.name);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                // Retrieve the shares associated with this transaction
                sql = "SELECT billid, amount FROM share WHERE transactionid = ?";
                try (PreparedStatement selectStmt = con.prepareStatement(sql)) {
                    selectStmt.setInt(1, transactionId);
                    ResultSet rs = selectStmt.executeQuery();

                    // Iterate through each share and update the bill's pending amount
                    while (rs.next()) {
                        int billId = rs.getInt("billid");
                        float amount = rs.getFloat("amount");

                        // Update the bill's pending amount
                        sql = "UPDATE bill SET pendingamount = pendingamount + ? WHERE billid = ?";
                        try (PreparedStatement updateStmt = con.prepareStatement(sql)) {
                            updateStmt.setFloat(1, amount);
                            updateStmt.setInt(2, billId);
                            updateStmt.executeUpdate();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                // Delete shares associated with this transaction
                sql = "DELETE FROM share WHERE transactionid = ?";
                try (PreparedStatement deleteStmt = con.prepareStatement(sql)) {
                    deleteStmt.setInt(1, transactionId);
                    deleteStmt.executeUpdate();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }
        getTransactionsFromDB();
        getBillsFromDB();
    }

    private void insertShare(transaction newTransaction, Connection con) throws SQLException {
        String sql = "INSERT INTO share (billid, transactionid, amount) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            share[] s = newTransaction.getShares();
            int sharecnt=newTransaction.shareCount;
            for (int i=0;i<sharecnt;i++) {
                stmt.setInt(1, s[i].getBillId());
                stmt.setInt(2, newTransaction.getTransactionId());
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
        getBillsFromDB();
    }

    public void showAllTransactions() {
        System.out.println("All Transactions for Vendor: " + this.name);
        for (int i = 0; i < transactionCount; i++) {
            System.out.println(transactions[i]);
        }
    }

    public void showAllTransactions(Date sdate, Date edate) {
        System.out.println("Transactions for Vendor: " + this.name);
        for (int i = 0; i < transactionCount; i++) {
            transaction txn = transactions[i];
            Date txnDate = txn.getTransactionDate();
            if (txnDate.compareTo(sdate) >= 0 && txnDate.compareTo(edate) <= 0) {
                System.out.println(txn);
            }
        }
    }

    public void showAllTransactionsPMonth() {
        // Calculate the date range for the previous month
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date sdate = new Date(calendar.getTimeInMillis());

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date edate = new Date(calendar.getTimeInMillis());

        // Call the method to show transactions by date range
        showAllTransactions(sdate, edate);
    }

    public void menu() {
        Scanner in = new Scanner(System.in);
        boolean loop = true;
        while (loop) {
            System.out.println("\nEnter the number to perform a vendor operation:");
            System.out.println("1) View bills for vendor: " + this.name);
            System.out.println("2) Add a new bill for vendor: " + this.name);
            System.out.println("3) Delete a bill");
            System.out.println("4) View pending bills for vendor: " + this.name);
            System.out.println("5) Add a new transaction for vendor: " + this.name);
            System.out.println("6) Remove a transaction");
            System.out.println("7) Return to main menu");
            System.out.println("8) Show all transactions");
            System.out.println("9) Show transactions between two dates");
            System.out.println("10) Show transactions for the previous month");
    

            int ch = in.nextInt();
            switch (ch) {
                case 1:
                    System.out.println("\nBills for Vendor: " + this.name);
                    for (int i = 0; i < billCount; i++) {
                        System.out.println(bills[i]);
                    }
                    break;
                case 2:
                    System.out.println("Enter bill details:");
                    System.out.print("Amount: ");
                    float amount = in.nextFloat();
                    float pendingAmount = amount;
                    System.out.print("Bill Date (YYYY-MM-DD): ");
                    String dateStr = in.next();

                    Date billDate = Date.valueOf(dateStr);

                    bill newBill = new bill();
                    newBill.setAmount(amount);
                    newBill.setPendingAmount(pendingAmount);
                    newBill.setBillDate(billDate);
                    newBill.setVendor(this.name);

                    addBill(newBill);

                    System.out.println("Bill added successfully.");
                    break;
                case 3:
                    System.out.print("Enter bill id to delete: ");
                    int billId = in.nextInt();

                    deleteBill(billId);

                    System.out.println("Bill with id " + billId + " deleted successfully.");
                    break;
                case 4:
                    System.out.println("Pending Bills for Vendor: " + this.name );
                    for (int i = 0; i < billCount; i++) {
                        if (bills[i].getPendingAmount() > 0) {
                            System.out.println(bills[i]);
                        }
                    }
                    break;
                case 5:
                    System.out.println("Enter transaction details:");

                    System.out.print("Transaction Date (YYYY-MM-DD): ");
                    dateStr = in.next();

                    java.sql.Date transactionDate = java.sql.Date.valueOf(dateStr);

                    transaction newTransaction = new transaction();
                    System.out.println("Enter number of bills to be added: ");
                    int count = in.nextInt();
                    for (int i = 0; i < count; i++) {
                        System.out.print("Enter bill ID: ");
                        int id = in.nextInt();
                        System.out.print("Enter bill amount: ");
                        float amountShare = in.nextFloat();
                        newTransaction.addShare(id, amountShare);
                    }
                    newTransaction.setVendor(name);
                    newTransaction.setTransactionDate(transactionDate);
                    addTransaction(newTransaction);

                    System.out.println("Transaction added successfully.");
                    break;
                case 6:
                    System.out.print("Enter transaction ID to remove: ");
                    int transId = in.nextInt();
                    removeTransaction(transId);
                    System.out.println("Transaction with ID " + transId + " removed successfully.");
                    break;
                case 7:
                    loop = false;
                    System.out.println("Returned to main menu");
                    break;
                case 8:
                    showAllTransactions();
                    break;
                case 9:
                    System.out.print("Enter start date (YYYY-MM-DD): ");
                    String startDateStr = in.next();
                    System.out.print("Enter end date (YYYY-MM-DD): ");
                    String endDateStr = in.next();

                    try {
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date sdate = new Date(dateFormat.parse(startDateStr).getTime());
                        Date edate = new Date(dateFormat.parse(endDateStr).getTime());

                        showAllTransactions(sdate, edate);
                    } catch (ParseException e) {
                        System.out.println("Invalid date format. Please use YYYY-MM-DD.");
                    }
                    break;
                case 10:
                    showAllTransactionsPMonth();
                    break;

                default:
                    System.out.println("Invalid ch. Please enter a number between 1 and 7.");
                    break;
            }
        }
        in.close();
    }


}
