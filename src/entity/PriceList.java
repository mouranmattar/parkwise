package entity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PriceList {
    private int listID;
    private int parkingLotID;
    private int year;
    private double firstHourPrice;
    private double additionalHourPrice;
    private double fullDayPrice;
    private Date validFrom;

    public PriceList(int listID, int parkingLotID, int year, double firstHourPrice, 
                     double additionalHourPrice, double fullDayPrice, Date validFrom) {
        this.listID = listID;
        this.parkingLotID = parkingLotID;
        this.year = year;
        this.firstHourPrice = firstHourPrice;
        this.additionalHourPrice = additionalHourPrice;
        this.fullDayPrice = fullDayPrice;
        this.validFrom = validFrom;
    }

    public int getListID() {
        return listID;
    }

    public void setListID(int listID) {
        this.listID = listID;
    }

    public int getParkingLotID() {
        return parkingLotID;
    }

    public void setParkingLotID(int parkingLotID) {
        this.parkingLotID = parkingLotID;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public double getFirstHourPrice() {
        return firstHourPrice;
    }

    public void setFirstHourPrice(double firstHourPrice) {
        this.firstHourPrice = firstHourPrice;
    }

    public double getAdditionalHourPrice() {
        return additionalHourPrice;
    }

    public void setAdditionalHourPrice(double additionalHourPrice) {
        this.additionalHourPrice = additionalHourPrice;
    }

    public double getFullDayPrice() {
        return fullDayPrice;
    }

    public void setFullDayPrice(double fullDayPrice) {
        this.fullDayPrice = fullDayPrice;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Map<String, Double> getRates() {
        Map<String, Double> rates = new HashMap<>();
        rates.put("firstHour", firstHourPrice);
        rates.put("additionalHour", additionalHourPrice);
        rates.put("fullDay", fullDayPrice);
        return rates;
    }

    @Override
    public String toString() {
        return "PriceList [Year: " + year + ", First Hour: $" + firstHourPrice + 
               ", Additional Hour: $" + additionalHourPrice + ", Full Day: $" + fullDayPrice + "]";
    }
}
