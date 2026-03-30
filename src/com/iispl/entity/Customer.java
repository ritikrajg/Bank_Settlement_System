package com.iispl.entity;

import java.time.LocalDate;

import com.iispl.enums.KycStatus;

/**
 * Bank customer. Extends BaseEntity to get audit fields and PK.
 */
public class Customer extends BaseEntity implements Validatable {

    private String     firstName;
    private String     lastName;
    private String     email;
    private KycStatus  kycStatus;
    private String     customerTier;   // e.g. RETAIL / CORPORATE / PREMIUM
    private LocalDate  onboardingDate;

    public Customer() {}

    public Customer(String firstName, String lastName, String email,
                    KycStatus kycStatus, String customerTier, LocalDate onboardingDate) {
        this.firstName      = firstName;
        this.lastName       = lastName;
        this.email          = email;
        this.kycStatus      = kycStatus;
        this.customerTier   = customerTier;
        this.onboardingDate = onboardingDate;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // ------------------------------------------------------------------ //
    //  Validatable                                                         //
    // ------------------------------------------------------------------ //

    @Override
    public boolean isValid() {
        return firstName != null && !firstName.isBlank()
            && lastName  != null && !lastName.isBlank()
            && email     != null && email.contains("@")
            && kycStatus != null;
    }

    @Override
    public String validationErrors() {
        StringBuilder sb = new StringBuilder();
        if (firstName == null || firstName.isBlank())   sb.append("firstName required; ");
        if (lastName  == null || lastName.isBlank())    sb.append("lastName required; ");
        if (email     == null || !email.contains("@"))  sb.append("valid email required; ");
        if (kycStatus == null)                          sb.append("kycStatus required; ");
        return sb.toString();
    }

    // ------------------------------------------------------------------ //
    //  Getters / Setters                                                   //
    // ------------------------------------------------------------------ //

    public String    getFirstName()                      { return firstName; }
    public void      setFirstName(String firstName)      { this.firstName = firstName; }

    public String    getLastName()                       { return lastName; }
    public void      setLastName(String lastName)        { this.lastName = lastName; }

    public String    getEmail()                          { return email; }
    public void      setEmail(String email)              { this.email = email; }

    public KycStatus getKycStatus()                      { return kycStatus; }
    public void      setKycStatus(KycStatus kycStatus)   { this.kycStatus = kycStatus; }

    public String    getCustomerTier()                         { return customerTier; }
    public void      setCustomerTier(String customerTier)      { this.customerTier = customerTier; }

    public LocalDate getOnboardingDate()                       { return onboardingDate; }
    public void      setOnboardingDate(LocalDate onboardingDate){ this.onboardingDate = onboardingDate; }

    @Override
    public String toString() {
        return "Customer{id=" + getId() + ", name=" + getFullName()
             + ", kyc=" + kycStatus + ", tier=" + customerTier + "}";
    }
}
