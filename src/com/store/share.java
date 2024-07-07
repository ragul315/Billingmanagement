package com.store;

public class share {
    private int billId;
    private float amount;

    public share(int billId, float amount) {
        this.billId = billId;
        this.amount = amount;
    }

    public int getBillId() {
        return billId;
    }

    public void setBillId(int billId) {
        this.billId = billId;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Bill ID: " + billId + ", Amount: " + amount+"\n";
    }
}
