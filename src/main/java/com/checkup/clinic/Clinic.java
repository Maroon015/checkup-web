package com.checkup.clinic;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A clinic where doctors work. One row in the "clinics" table.
 *
 * Old version: Clinic.java wrote "City of Clinic: ..." lines to clinicInfo.txt and
 * findClinic() scanned the file line by line. Now each clinic is a database row and
 * "find clinics in my city" is one repository method (see ClinicRepository).
 */
@Entity
@Table(name = "clinics")
public class Clinic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // the database picks the next id
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    // Old rule was "\\d+\\s[A-Za-z]+\\s[A-Za-z]+", which rejected real addresses like
    // "1151 Richmond St N". Keeping it simple: required, sensible length.
    @NotBlank
    @Size(max = 200)
    @Column(name = "street_address")
    private String streetAddress;

    // Old rule was "[A-Za-z]+", which rejected "Niagara Falls" and "Saint-Jérôme".
    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "[\\p{L} .'-]+", message = "City can only contain letters, spaces, '.', ''' and '-'")
    private String city;

    protected Clinic() {
        // JPA needs a no-argument constructor to build objects from rows
    }

    public Clinic(String name, String streetAddress, String city) {
        this.name = name;
        this.streetAddress = streetAddress;
        this.city = city;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getStreetAddress() { return streetAddress; }
    public String getCity() { return city; }

    public void setName(String name) { this.name = name; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }
    public void setCity(String city) { this.city = city; }
}
