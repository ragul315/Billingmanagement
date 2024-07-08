package com.store;

import java.sql.*;
import java.sql.Date;
import java.util.*;

import com.dbcon.dbcon;

public class manager {
    private dbcon db;
    private Connection con;

    public manager() {
        db = new dbcon();
        con = db.getConnection();
    }

    /* Operation on vendors */
    public void displayVendors() {

        String sql = "SELECT id, vendorname, pendingamount FROM vendors;";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("id");
                String vendorName = rs.getString("vendorname");
                float pendingAmount = rs.getFloat("pendingamount");
                System.out.println(id + ", " + vendorName + ", " + pendingAmount);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addVendor(String vname) {
        String sql = "INSERT INTO vendors (vendorname) VALUES (?)";

        try (PreparedStatement stmt = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, vname);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("New Vendor Added Successfully\nVendor Name:" + vname + " id:" + id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteVendor(int id) {
        vendor nvendor = null;
        String sql = "SELECT vendorname FROM vendors WHERE id = ? ;";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String vname = rs.getString("vendorname");
                nvendor = new vendor(id, vname);
                nvendor.delete();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        sql = "DELETE FROM vendors WHERE id = ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void selectVendor(int id) {
        vendor nvendor = null;
        String sql = "SELECT vendorname FROM vendors WHERE id = ? ;";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String vname = rs.getString("vendorname");
                nvendor = new vendor(id, vname);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (nvendor != null) {
            nvendor.menu();
        } else {
            System.out.println("Vendor with ID " + id + " not found.");
        }
    }

    public void showAllTransactions() {
        String sql = "SELECT transactionid, transactiondate, totalamount, vendor FROM transaction";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("transactionid");
                Date day = rs.getDate("transactiondate");
                float amount = rs.getFloat("totalamount");
                String vname = rs.getString("vendor");

                System.out.println("Transaction ID:" + id + ", Date: " + day + ", Amount: "
                        + amount + ", Vendor Name: " + vname);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* Operation on Transaction */
    // Function to show transactions by date range for all vendors
    public void showAllTransactions(Date sdate, Date edate) {
        String sql = "SELECT transactionid, transactiondate, totalamount, vendor FROM transaction WHERE transactiondate BETWEEN ? AND ?";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            stmt.setDate(1, new java.sql.Date(sdate.getTime()));
            stmt.setDate(2, new java.sql.Date(edate.getTime()));
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int id = rs.getInt("transactionid");
                Date day = rs.getDate("transactiondate");
                float amount = rs.getFloat("totalamount");
                String vname = rs.getString("vendor");

                System.out.println("Transaction ID: " + id + ", Date: " + day + ", Amount: "
                        + amount + ", Vendor Name: " + vname);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Function to show transactions of previous month for all vendors
    public void showAllTransactionsPMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date sdate = new Date(calendar.getTimeInMillis());

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date edate = new Date(calendar.getTimeInMillis());

        showAllTransactions(sdate, edate);
    }

    public void findTopPaidVendor() {
        String sql = "SELECT vendor, SUM(totalamount) AS totalAmount FROM transaction GROUP BY vendor ORDER BY totalAmount DESC LIMIT 1";

        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String vname = rs.getString("vendor");
                float totalAmount = rs.getFloat("totalAmount");

                System.out.println("\nTop Paid Vendor: " + vname + " Total Amount: " + totalAmount);
            } else {
                System.out.println("\nNo vendors found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
                    String vname = in.next();
                    addVendor(vname);
                    System.out.println("Vendor added successfully.");
                    break;
                case 3:
                    System.out.print("Enter vendor ID to delete: ");
                    int vid = in.nextInt();
                    deleteVendor(vid);
                    System.out.println("Vendor deleted successfully.");
                    break;
                case 4:
                    System.out.print("Enter vendor ID to select: ");
                    int vids = in.nextInt();
                    selectVendor(vids);
                    break;
                case 5:
                    showAllTransactions();
                    break;
                case 6:
                    System.out.print("Enter start date (YYYY-MM-DD): ");
                    String sdatestr = in.next();
                    System.out.print("Enter end date (YYYY-MM-DD): ");
                    String edatestr = in.next();
                    Date sdate = java.sql.Date.valueOf(sdatestr);
                    Date edate = java.sql.Date.valueOf(edatestr);
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
                    db.closecon();
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
