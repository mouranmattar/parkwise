package entity;

import java.sql.Timestamp;

public class Receipt {
    private int receiptID;
    private Timestamp issueDate;
    private long overallHours;
    private String appliedRate;
    private double finalAmount;
    private int sessionID;

    public Receipt(int receiptID, Timestamp issueDate, long overallHours, String appliedRate, double finalAmount, int sessionID) {
        this.receiptID = receiptID;
        this.issueDate = issueDate;
        this.overallHours = overallHours;
        this.appliedRate = appliedRate;
        this.finalAmount = finalAmount;
        this.sessionID = sessionID;
    }

    public int getReceiptID() {
        return receiptID;
    }

    public Timestamp getIssueDate() {
        return issueDate;
    }

    public long getOverallHours() {
        return overallHours;
    }

    public String getAppliedRate() {
        return appliedRate;
    }

    public double getFinalAmount() {
        return finalAmount;
    }

    public int getSessionID() {
        return sessionID;
    }
}
