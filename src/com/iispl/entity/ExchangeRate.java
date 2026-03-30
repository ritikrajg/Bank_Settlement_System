package com.iispl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * FX rate snapshot for a currency pair on a specific date.
 */
public class ExchangeRate extends BaseEntity {

    private String     baseCurrency;
    private String     quoteCurrency;
    private BigDecimal rate;
    private BigDecimal bidRate;
    private BigDecimal askRate;
    private LocalDate  rateDate;
    private String     source;       // e.g. "RBI", "ECB", "REUTERS"
    private boolean    active;

    public ExchangeRate() {}

    public ExchangeRate(String baseCurrency, String quoteCurrency,
                        BigDecimal rate, BigDecimal bidRate, BigDecimal askRate,
                        LocalDate rateDate, String source) {
        this.baseCurrency  = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate          = rate;
        this.bidRate       = bidRate;
        this.askRate       = askRate;
        this.rateDate      = rateDate;
        this.source        = source;
        this.active        = true;
    }

    /**
     * Converts an amount from baseCurrency to quoteCurrency using mid rate.
     */
    public BigDecimal convert(BigDecimal amount) {
        if (amount == null || rate == null) return BigDecimal.ZERO;
        return amount.multiply(rate);
    }

    // Getters / Setters
    public String     getBaseCurrency()                          { return baseCurrency; }
    public void       setBaseCurrency(String baseCurrency)       { this.baseCurrency = baseCurrency; }

    public String     getQuoteCurrency()                         { return quoteCurrency; }
    public void       setQuoteCurrency(String quoteCurrency)     { this.quoteCurrency = quoteCurrency; }

    public BigDecimal getRate()                                  { return rate; }
    public void       setRate(BigDecimal rate)                   { this.rate = rate; }

    public BigDecimal getBidRate()                               { return bidRate; }
    public void       setBidRate(BigDecimal bidRate)             { this.bidRate = bidRate; }

    public BigDecimal getAskRate()                               { return askRate; }
    public void       setAskRate(BigDecimal askRate)             { this.askRate = askRate; }

    public LocalDate  getRateDate()                              { return rateDate; }
    public void       setRateDate(LocalDate rateDate)            { this.rateDate = rateDate; }

    public String     getSource()                                { return source; }
    public void       setSource(String source)                   { this.source = source; }

    public boolean    isActive()                                 { return active; }
    public void       setActive(boolean active)                  { this.active = active; }

    @Override
    public String toString() {
        return "ExchangeRate{" + baseCurrency + "/" + quoteCurrency
             + "=" + rate + ", date=" + rateDate + ", src=" + source + "}";
    }
}
