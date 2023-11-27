package net.smartlogic.unitconverter.model;

public class Currency {
    String currencyName;        // Indian Rupees
    String currencyISOCode;     // INR
    String country;             // India
    String currencySymbol;      // R
    int flagImageResource;

    public Currency(String currencyName, String currencyISOCode, String country, String currencySymbol, int flagImageResource) {
        this.currencyName = currencyName;
        this.currencyISOCode = currencyISOCode;
        this.country = country;
        this.currencySymbol = currencySymbol;
        this.flagImageResource = flagImageResource;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public String getCurrencyISOCode() {
        return currencyISOCode;
    }

    public void setCurrencyISOCode(String currencyISOCode) {
        this.currencyISOCode = currencyISOCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public int getFlagImageResource() {
        return flagImageResource;
    }

    public void setFlagImageResource(int flagImageResource) {
        this.flagImageResource = flagImageResource;
    }
}




