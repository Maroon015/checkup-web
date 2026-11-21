package com.checkup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.account.AccountService;
import com.checkup.account.DemoAccounts;
import com.checkup.account.RegistrationForm;
import com.checkup.patient.BloodType;
import com.checkup.patient.Patient;
import com.checkup.patient.PatientRepository;
import com.checkup.patient.PhoneNumber;
import com.checkup.patient.PhoneType;

/**
 * Patient record pages and the doctor's read access, through real HTTP requests with the
 * demo data. Alex: 1 personal phone, O+, appointments with Dr. Patel. Sam: 2 phones.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecordsWebTest {

    @Autowired MockMvc mvc;
    @Autowired PatientRepository patients;
    @Autowired AccountService accountService;

    private Patient alex() {
        return patients.findWithPhonesById(patients.findByEmailIgnoreCase(DemoAccounts.PATIENT_EMAIL).orElseThrow().getId())
                .orElseThrow();
    }

    private Patient sam() {
        return patients.findWithPhonesById(patients.findByEmailIgnoreCase("sam.lee@example.com").orElseThrow().getId())
                .orElseThrow();
    }

    // ------------------------------------------------------------------ patient: own record

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void patientSeesTheirRecordFilledIn() throws Exception {
        mvc.perform(get("/patient/profile"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Seasonal allergies.")))
                .andExpect(content().string(containsString("519-555-0101")))
                .andExpect(content().string(not(containsString("Knee surgery")))); // Sam's history
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void patientCanUpdateTheirRecord() throws Exception {
        mvc.perform(post("/patient/profile").with(csrf())
                        .param("firstName", "Alex")
                        .param("lastName", "Morgan")
                        .param("dateOfBirth", "1991-04-12")
                        .param("city", "London")
                        .param("street", "Oak Ave")
                        .param("unitNumber", "")
                        .param("bloodType", "AB_NEGATIVE")
                        .param("heightCm", "173.5")
                        .param("weightKg", "70")
                        .param("medicalHistory", "Seasonal allergies. Penicillin allergy."))
                .andExpect(redirectedUrl("/patient/profile"))
                .andExpect(flash().attributeExists("success"));

        Patient updated = alex();
        assertThat(updated.getStreet()).isEqualTo("Oak Ave");
        assertThat(updated.getUnitNumber()).isNull(); // blank becomes null, not ""
        assertThat(updated.getBloodType()).isEqualTo(BloodType.AB_NEGATIVE);
        assertThat(updated.getHeightCm()).isEqualTo(173.5);
        assertThat(updated.getMedicalHistory()).contains("Penicillin");
        assertThat(updated.getEmail()).isEqualTo(DemoAccounts.PATIENT_EMAIL); // not editable here
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void badValuesComeBackWithMessagesAndNothingIsSaved() throws Exception {
        mvc.perform(post("/patient/profile").with(csrf())
                        .param("firstName", "")
                        .param("lastName", "Morgan")
                        .param("dateOfBirth", "1991-04-12")
                        .param("heightCm", "-5")
                        .param("weightKg", "9999"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "firstName", "heightCm", "weightKg"));

        assertThat(alex().getFirstName()).isEqualTo("Alex");
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void phoneNumbersAreNormalisedToTenDigits() throws Exception {
        mvc.perform(post("/patient/profile/phones").with(csrf())
                        .param("type", "EMERGENCY").param("number", "(519) 555-0177"))
                .andExpect(flash().attribute("success", "Phone number added."));

        assertThat(alex().getPhones(PhoneType.EMERGENCY))
                .extracting(PhoneNumber::getNumber)
                .containsExactly("5195550177");
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void badOrDuplicatePhoneNumbersAreRejected() throws Exception {
        mvc.perform(post("/patient/profile/phones").with(csrf())
                        .param("type", "PERSONAL").param("number", "555-01"))
                .andExpect(flash().attribute("error", containsString("10 digits")));

        mvc.perform(post("/patient/profile/phones").with(csrf())
                        .param("type", "PERSONAL").param("number", "519 555 0101")) // Alex already has it
                .andExpect(flash().attribute("error", containsString("already have")));

        assertThat(alex().getPhones()).hasSize(1);
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void patientCanRemoveOwnPhoneButNotSomeoneElses() throws Exception {
        Long samsPhone = sam().getPhones().getFirst().getId();
        mvc.perform(post("/patient/profile/phones/" + samsPhone + "/delete").with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(sam().getPhones()).hasSize(2);

        Long alexsPhone = alex().getPhones().getFirst().getId();
        mvc.perform(post("/patient/profile/phones/" + alexsPhone + "/delete").with(csrf()))
                .andExpect(redirectedUrl("/patient/profile#phones"));
        assertThat(alex().getPhones()).isEmpty();
    }

    // ------------------------------------------------------------------ patient: find a clinic

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void findsClinicsAndDoctorsByCityIgnoringCase() throws Exception {
        mvc.perform(get("/patient/clinics").param("city", "london"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Northside Family Clinic")))
                .andExpect(content().string(containsString("Riverside Health Centre")))
                .andExpect(content().string(containsString("Dr. Daniel Okafor")))
                .andExpect(content().string(not(containsString("Lakeshore Medical")))); // Toronto
    }

    // ------------------------------------------------------------------ doctor: read access

    @Test
    @WithUserDetails(DemoAccounts.DOCTOR_EMAIL)
    void doctorListsOnlyTheirOwnPatients() throws Exception {
        RegistrationForm f = new RegistrationForm();
        f.setFirstName("Nina");
        f.setLastName("Stranger");
        f.setEmail("nina.stranger@example.com");
        f.setDateOfBirth(LocalDate.of(1985, 6, 1));
        f.setPassword("password-123");
        f.setConfirmPassword("password-123");
        accountService.registerPatient(f); // a patient with no appointments at all

        mvc.perform(get("/doctor/patients"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Morgan, Alex")))
                .andExpect(content().string(containsString("Lee, Sam")))
                .andExpect(content().string(not(containsString("Stranger"))));

        mvc.perform(get("/doctor/patients").param("lastName", "mor"))
                .andExpect(content().string(containsString("Morgan, Alex")))
                .andExpect(content().string(not(containsString("Lee, Sam"))));
    }

    @Test
    @WithUserDetails(DemoAccounts.DOCTOR_EMAIL)
    void doctorCanReadTheirPatientsRecord() throws Exception {
        mvc.perform(get("/doctor/patients/" + alex().getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("O+")))
                .andExpect(content().string(containsString("Seasonal allergies.")))
                .andExpect(content().string(containsString("Annual check-up")));
    }

    @Test
    @WithUserDetails(DemoAccounts.DOCTOR_EMAIL)
    void doctorGets404ForAPatientTheyHaveNeverSeen() throws Exception {
        RegistrationForm f = new RegistrationForm();
        f.setFirstName("Nina");
        f.setLastName("Stranger");
        f.setEmail("nina2.stranger@example.com");
        f.setDateOfBirth(LocalDate.of(1985, 6, 1));
        f.setPassword("password-123");
        f.setConfirmPassword("password-123");
        Long ninaId = accountService.registerPatient(f).getId();

        mvc.perform(get("/doctor/patients/" + ninaId)).andExpect(status().isNotFound());
        mvc.perform(get("/doctor/patients/999999")).andExpect(status().isNotFound()); // same answer
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void patientsCannotUseTheDoctorRecordPages() throws Exception {
        mvc.perform(get("/doctor/patients")).andExpect(status().isForbidden());
        mvc.perform(get("/doctor/patients/" + sam().getId())).andExpect(status().isForbidden());
    }
}
