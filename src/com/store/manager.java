package com.store;

import java.sql.*;
import java.sql.Date;
import java.util.*;

import com.dbcon.dbcon;

public class manager {
    private vendor[] vendors;
    private int n;

    public manager() {
        vendors = new vendor[10];
        n = 0;
        getVendorsFromDB();
    }

    public void getVendorsFromDB() {
        n = 0;
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "SELECT id, vendorname FROM vendors;";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String vendorName = rs.getString("vendorname");

                vendor v = new vendor(id, vendorName);

                if (n == vendors.length) {
                    vendors = resizearray(vendors, vendors.length * 2);
                }
                vendors[n++] = v;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }
    }

    private vendor[] resizearray(vendor[] array, int newSize) {
        vendor[] newArray = new vendor[newSize];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    public void displayVendors() {
        System.out.println("Vendors List");
        for (int i = 0; i < n; i++) {
            System.out.println(vendors[i].getVendorId() + ": " + vendors[i].getName());
        }
    }

    public void addVendor(String vendorName) {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "INSERT INTO vendors (vendorname) VALUES (?)";

        try (PreparedStatement stmt = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, vendorName);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                vendor v = new vendor(id, vendorName);

                if (n == vendors.length) {
                    vendors = resizearray(vendors, vendors.length * 2);
                }
                vendors[n++] = v;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }
    }

    public void deleteVendor(int id) {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "DELETE FROM vendors WHERE id = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }

        int i;
        for (i = 0; i < n; i++) {
            if (vendors[i].getVendorId() == id) {
                vendors[i].delete();

                break;
            }
        }

        for (; i < n - 1; i++) {
            vendors[i] = vendors[i + 1];
        }
        vendors[--n] = null;

    }

    public void selectVendor(int id) {
        vendor nvendor = null;
        for (int i = 0; i < n; i++) {
            if (vendors[i].getVendorId() == id) {
                nvendor = vendors[i];
                break;
            }
        }

        if (nvendor != null) {
            nvendor.menu();
        } else {
            System.out.println("Vendor with ID " + id + " not found.");
        }
    }

    public void showAllTransactions() {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "SELECT transactionid, transactiondate, totalamount, vendor FROM transaction";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int transactionId = rs.getInt("transactionid");
                Date transactionDate = rs.getDate("transactiondate");
                float amount = rs.getFloat("totalamount");

                String vendorName = rs.getString("vendor");

                System.out.println("Transaction ID:"+ transactionId + ", Date: " + transactionDate +", Amount: " + amount + ", Vendor Name: " + vendorName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }
    }

    // Function to show transactions by date range for all vendors
    public void showAllTransactions(Date startDate, Date endDate) {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "SELECT transactionid, transactiondate, totalamount, vendor FROM transaction WHERE transactiondate BETWEEN ? AND ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setDate(1, new java.sql.Date(startDate.getTime()));
            stmt.setDate(2, new java.sql.Date(endDate.getTime()));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int transactionId = rs.getInt("transactionid");
                Date transactionDate = rs.getDate("transactiondate");
                float amount = rs.getFloat("totalamount");

                String vendorName = rs.getString("vendor");

                System.out.println("Transaction ID: " + transactionId + ", Date: " + transactionDate +", Amount: " + amount +  ", Vendor Name: " + vendorName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }
    }

    // Function to show transactions of previous month for all vendors
    public void showAllTransactionsPMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date startDate = (Date) calendar.getTime();

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endDate = (Date) calendar.getTime();

        showAllTransactions(startDate, endDate);
    }

    // Function to find the top paid vendor
    public void findTopPaidVendor() {
        dbcon db = new dbcon();
        Connection con = db.getConnection();
        String sql = "SELECT vendor, SUM(totalamount) AS totalAmount FROM transaction GROUP BY vendor ORDER BY totalAmount DESC LIMIT 1";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String vendorName = rs.getString("vendor");
                float totalAmount = rs.getFloat("totalAmount");

                System.out.println("\nTop Paid Vendor: " + vendorName + " Total Amount: " + totalAmount);
            } else {
                System.out.println("\nNo vendors found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            db.closecon();
        }
    }

    public void menu() {
        Scanner in = new Scanner(System.in);
        boolean loop = true;
        while (loop) {
            System.out.println("\nEnter the number to perform operation:");
            System.out.println("1) List all vendors");
            System.out.println("2) Add a vendor");
            System.out.println("3) Delete a vendor");
            System.out.println("4) Select a vendor");
            System.out.println("5) Show all transactions");
            System.out.println("6) Show transactions by date range");
            System.out.println("7) Show transactions of previous month");
            System.out.println("8) Find top paid vendor");
            System.out.println("9) Exit");
    
            int ch = in.nextInt();
            switch (ch) {
                case 1:
                    displayVendors();
                    break;
                case 2:
                    System.out.print("Enter vendor name: ");
                    String vendorName = in.next();
                    addVendor(vendorName);
                    System.out.println("Vendor added successfully.");
                    break;
                case 3:
                    System.out.print("Enter vendor ID to delete: ");
                    int vendorIdToDelete = in.nextInt();
                    deleteVendor(vendorIdToDelete);
                    System.out.println("Vendor deleted successfully.");
                    break;
                case 4:
                    System.out.print("Enter vendor ID to select: ");
                    int vendorIdToSelect = in.nextInt();
                    selectVendor(vendorIdToSelect);
                    break;
                case 5:
                    showAllTransactions();
                    break;
                case 6:
                    System.out.print("Enter start date (YYYY-MM-DD): ");
                    String startDateStr = in.next();
                    System.out.print("Enter end date (YYYY-MM-DD): ");
                    String endDateStr = in.next();
                    Date sdate = java.sql.Date.valueOf(startDateStr);
                    Date edate = java.sql.Date.valueOf(endDateStr);
                    showAllTransactions(sdate, edate);
                    break;
                case 7:
                    showAllTransactionsPMonth();
                    break;
                case 8:
                    findTopPaidVendor();
                    break;
                case 9:
                    loop = false;
                    System.out.println("Exiting manager menu.");
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 9.");
                    break;
            }
        }
        in.close();
    }
    
}
