package com.store;

import java.util.Arrays;
import java.util.Date;

public class transaction {
    private int transactionId;
    private double totalAmount;
    private String vendor;
    private Date transactionDate;
    private share[] shares;
    int shareCount;

    // Constructor
    public transaction() {
        shares = new share[10];
        shareCount = 0;
        totalAmount = 0.0;
    }

    // Getters and Setters
    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public java.sql.Date getTransactionDate() {
        return (java.sql.Date) transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public share[] getShares() {
        return shares;
    }

    @Override
    public String toString() {
        String ret = "transactionId=" + transactionId +
                ", totalAmount=" + totalAmount +
                ", vendor='" + vendor + '\'' +
                ", transactionDate=" + transactionDate +
                ", shares=" ;
        for (int i = 0; i < shareCount; i++) {
            ret = ret + shares[i];
        }
        return ret;
    }

    public void setShares(share[] shares) {
        this.shares = shares;
        recalculateTotalAmount();
    }

    public void addShare(share nshare) {
        if (shareCount == shares.length) {
            shares = resizeArray(shares, shares.length * 2);
        }
        shares[shareCount++] = nshare;
        recalculateTotalAmount();
    }

    public void addShare(int billId, float amount) {
        share nshare = new share(billId, amount);
        if (shareCount == shares.length) {
            shares = resizeArray(shares, shares.length * 2);
        }
        shares[shareCount++] = nshare;
        recalculateTotalAmount();
    }

    private share[] resizeArray(share[] array, int newSize) {
        share[] newArray = new share[newSize];
        System.arraycopy(array, 0, newArray, 0, array.length);
        return newArray;
    }

    private void recalculateTotalAmount() {
        double total = 0.0;
        for (int i = 0; i < shareCount; i++) {
            total += shares[i].getAmount();
        }
        setTotalAmount(total);
    }
}
