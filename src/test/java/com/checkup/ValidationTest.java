package com.checkup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.checkup.appointment.Appointment;
import com.checkup.clinic.Clinic;
import com.checkup.doctor.Doctor;
import com.checkup.patient.Patient;
import com.checkup.patient.PhoneNumber;
import com.checkup.patient.PhoneType;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Plain unit tests (no Spring, no database) for the rules that used to live in the old
 * setters. Each test names the field that should fail, so a broken rule is easy to spot.
 */
class ValidationTest {

    static ValidatorFactory factory;
    static Validator validator;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    /** Names of the fields that broke a rule, e.g. ["email", "phones[0].number"]. */
    private static Set<String> invalidFields(Object o) {
        Set<ConstraintViolation<Object>> violations = validator.validate(o);
        return violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }

    private static Patient validPatient() {
        Patient p = new Patient("Alex", "Morgan", "alex@example.com", LocalDate.of(1991, 4, 12));
        p.addPhone(new PhoneNumber(PhoneType.PERSONAL, "5195550101"));
        return p;
    }

    @Test
    void validPatientHasNoErrors() {
        assertThat(invalidFields(validPatient())).isEmpty();
    }

    @Test
    void patientEmailMustBeAnEmail() {
        Patient p = validPatient();
        p.setEmail("not-an-email");
        assertThat(invalidFields(p)).containsExactly("email");
    }

    @Test
    void emailIsStoredLowercase() {
        Patient p = validPatient();
        p.setEmail("  Alex.Morgan@Example.COM ");
        assertThat(p.getEmail()).isEqualTo("alex.morgan@example.com");
    }

    @Test
    void birthDateMustBeInThePast() {
        Patient p = validPatient();
        p.setDateOfBirth(LocalDate.now().plusDays(1));
        assertThat(invalidFields(p)).containsExactly("dateOfBirth");
    }

    @Test
    void heightAndWeightMustBePositive() {
        Patient p = validPatient();
        p.setHeightCm(0.0);
        p.setWeightKg(-5.0);
        assertThat(invalidFields(p)).containsExactlyInAnyOrder("heightCm", "weightKg");
    }

    @Test
    void phoneNumbersMustBeTenDigits() {
        Patient p = validPatient();
        p.addPhone(new PhoneNumber(PhoneType.EMERGENCY, "519-555-01")); // @Valid checks the list too
        assertThat(invalidFields(p)).containsExactly("phones[1].number");
    }

    @Test
    void cityAllowsRealCityNames() {
        // The old rule "[A-Za-z]+" rejected both of these.
        assertThat(invalidFields(new Clinic("A", "1 Main St", "Niagara Falls"))).isEmpty();
        assertThat(invalidFields(new Clinic("B", "2 Rue Principale", "Saint-Jérôme"))).isEmpty();
        assertThat(invalidFields(new Clinic("C", "3 Main St", "L0nd0n"))).containsExactly("city");
    }

    @Test
    void doctorFaxIsOptionalButMustBeTenDigitsIfGiven() {
        Clinic clinic = new Clinic("A", "1 Main St", "London");
        Doctor d = new Doctor("Jordan", "Patel", "Family Medicine", "jp@example.com", "5195550199", clinic);
        assertThat(invalidFields(d)).isEmpty();

        d.setFax("12345");
        assertThat(invalidFields(d)).containsExactly("fax");
    }

    @Test
    void appointmentMustEndAfterItStarts() {
        LocalDateTime nine = LocalDateTime.of(2030, 1, 1, 9, 0);
        assertThatThrownBy(() -> new Appointment(null, null, nine, nine, "Check-up"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
