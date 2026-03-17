package com.example.smsexpensetracker;

public class Transaction {

    private int    id;
    private String datetime;
    private double amount;
    private String type;
    private String description;
    private String party;
    private String reference;
    private String smsId;
    private long   smsDate;    // epoch milliseconds — used for sorting

    public Transaction() {}

    public Transaction(String datetime, double amount, String type,
                       String description, String party,
                       String reference, String smsId, long smsDate) {
        this.datetime    = datetime;
        this.amount      = amount;
        this.type        = type;
        this.description = description;
        this.party       = party;
        this.reference   = reference;
        this.smsId       = smsId;
        this.smsDate     = smsDate;
    }

    public int    getId()                          { return id; }
    public void   setId(int id)                    { this.id = id; }

    public String getDatetime()                    { return datetime; }
    public void   setDatetime(String v)            { this.datetime = v; }

    public double getAmount()                      { return amount; }
    public void   setAmount(double v)              { this.amount = v; }

    public String getType()                        { return type; }
    public void   setType(String v)                { this.type = v; }

    public String getDescription()                 { return description; }
    public void   setDescription(String v)         { this.description = v; }

    public String getParty()                       { return party; }
    public void   setParty(String v)               { this.party = v; }

    public String getReference()                   { return reference; }
    public void   setReference(String v)           { this.reference = v; }

    public String getSmsId()                       { return smsId; }
    public void   setSmsId(String v)               { this.smsId = v; }

    public long   getSmsDate()                     { return smsDate; }
    public void   setSmsDate(long v)               { this.smsDate = v; }

    @Override
    public String toString() {
        return "Transaction{id=" + id + ", datetime='" + datetime
                + "', amount=" + amount + ", type='" + type + "'}";
    }
}