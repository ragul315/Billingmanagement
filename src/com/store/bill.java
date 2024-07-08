package com.store;

import java.util.Date;

public class bill {
    private int billid;
    private float amount;
    private float pendingAmount;
    private Date billDate;
    private String vendor;

    // Getters and Setters
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getVendor() {
        return vendor;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public int getBillid() {
        return billid;
    }

    public void setBillid(int billid) {
        this.billid = billid;
    }

    public float getPendingAmount() {
        return pendingAmount;
    }

    public void setPendingAmount(float pendingAmount) {
        this.pendingAmount = pendingAmount;
    }

    public Date getBillDate() {
        return billDate;
    }

    public void setBillDate(Date billDate) {
        this.billDate = billDate;
    }

    @Override
    public String toString() {
        return "billid=" + billid +
                ", amount=" + amount +
                ", pendingAmount=" + pendingAmount +
                ", billDate=" + billDate +
                ", vendor=" + vendor;
    }
}
