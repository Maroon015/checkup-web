package com.checkup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.account.DemoAccounts;
import com.checkup.account.Role;
import com.checkup.account.UserAccountDetailsService;
import com.checkup.audit.AuditAction;
import com.checkup.audit.AuditEntry;
import com.checkup.audit.AuditLogRepository;
import com.checkup.audit.AuditService;
import com.checkup.careers.JobApplication;
import com.checkup.careers.JobApplicationRepository;
import com.checkup.careers.Position;
import com.checkup.patient.Patient;
import com.checkup.patient.PatientRepository;

/**
 * Audit log, clinical notes and the careers page, through real HTTP requests with the
 * demo data. Each request says who is making it (asAlex(), asPatel(), asAdmin()), so one
 * test can show a doctor's action appearing on the patient's own page.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditAndCareersWebTest {

    @Autowired MockMvc mvc;
    @Autowired UserAccountDetailsService users;
    @Autowired PatientRepository patients;
    @Autowired AuditLogRepository auditLog;
    @Autowired AuditService auditService;
    @Autowired JobApplicationRepository applications;

    private RequestPostProcessor as(String email) {
        return user(users.loadUserByUsername(email));
    }
    private RequestPostProcessor asAlex() { return as(DemoAccounts.PATIENT_EMAIL); }
    private RequestPostProcessor asPatel() { return as(DemoAccounts.DOCTOR_EMAIL); }
    private RequestPostProcessor asAdmin() { return as(DemoAccounts.ADMIN_EMAIL); }

    private Patient alex() {
        return patients.findByEmailIgnoreCase(DemoAccounts.PATIENT_EMAIL).orElseThrow();
    }

    private List<AuditEntry> alexLog() {
        return auditLog.findByPatientIdOrderByOccurredAtDescIdDesc(alex().getId());
    }

    // ------------------------------------------------------------------ audit: doctor access

    @Test
    void doctorViewingARecordIsLoggedAndThePatientCanSeeIt() throws Exception {
        assertThat(alexLog()).isEmpty();

        mvc.perform(get("/doctor/patients/" + alex().getId()).with(asPatel())).andExpect(status().isOk());

        assertThat(alexLog()).singleElement().satisfies(e -> {
            assertThat(e.getAction()).isEqualTo(AuditAction.VIEW_RECORD);
            assertThat(e.getActorName()).isEqualTo("Dr. Jordan Patel");
            assertThat(e.getActorRole()).isEqualTo(Role.DOCTOR);
        });

        mvc.perform(get("/patient/access-log").with(asAlex()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dr. Jordan Patel")))
                .andExpect(content().string(containsString("Viewed record")));
    }

    @Test
    void deniedRecordAccessWritesNothing() throws Exception {
        long before = auditLog.count();
        mvc.perform(get("/doctor/patients/999999").with(asPatel())).andExpect(status().isNotFound());
        assertThat(auditLog.count()).isEqualTo(before);
    }

    // ------------------------------------------------------------------ audit: patient changes

    @Test
    void profileChangesLogWhichFieldsChangedButNotTheValues() throws Exception {
        mvc.perform(post("/patient/profile").with(asAlex()).with(csrf())
                        .param("firstName", "Alex").param("lastName", "Morgan")
                        .param("dateOfBirth", "1991-04-12")
                        .param("city", "London").param("street", "Maple Ave").param("unitNumber", "12")
                        .param("bloodType", "O_POSITIVE")
                        .param("heightCm", "172.0").param("weightKg", "71.5")
                        .param("medicalHistory", "Seasonal allergies."))
                .andExpect(redirectedUrl("/patient/profile"));

        assertThat(alexLog()).singleElement().satisfies(e -> {
            assertThat(e.getAction()).isEqualTo(AuditAction.UPDATE_RECORD);
            assertThat(e.getDetails()).isEqualTo("Changed: weight");
            assertThat(e.getDetails()).doesNotContain("71.5");
        });
    }

    @Test
    void savingWithoutChangesIsNotLogged() throws Exception {
        mvc.perform(post("/patient/profile").with(asAlex()).with(csrf())
                .param("firstName", "Alex").param("lastName", "Morgan")
                .param("dateOfBirth", "1991-04-12")
                .param("city", "London").param("street", "Maple Ave").param("unitNumber", "12")
                .param("bloodType", "O_POSITIVE")
                .param("heightCm", "172.0").param("weightKg", "68.0")
                .param("medicalHistory", "Seasonal allergies."));

        assertThat(alexLog()).isEmpty();
    }

    @Test
    void phoneChangesAreLogged() throws Exception {
        mvc.perform(post("/patient/profile/phones").with(asAlex()).with(csrf())
                .param("type", "EMERGENCY").param("number", "519-555-0177"));

        assertThat(alexLog()).extracting(AuditEntry::getAction).containsExactly(AuditAction.ADD_PHONE);
        assertThat(alexLog().getFirst().getDetails()).isEqualTo("emergency number").doesNotContain("555");
    }

    @Test
    void auditEntriesCanOnlyBeWrittenInsideTheChangesTransaction() {
        // Called on its own (no surrounding transaction), the audit service refuses.
        // So no code path can log something separately from the change it describes.
        assertThatThrownBy(() -> runWithoutTransaction(() ->
                auditService.record(1L, "Someone", Role.ADMIN, AuditAction.VIEW_RECORD, 1L, null)))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Autowired NoTransactionRunner noTx;

    private void runWithoutTransaction(Runnable r) {
        noTx.run(r);
    }

    /** Helper bean that runs code with any current transaction suspended. */
    @org.springframework.boot.test.context.TestComponent
    static class NoTransactionRunner {
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        public void run(Runnable r) {
            r.run();
        }
    }

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.context.annotation.Import(NoTransactionRunner.class)
    static class Config {
    }

    // ------------------------------------------------------------------ clinical notes

    @Test
    void doctorAddsANoteThatThePatientCanReadAndItIsLogged() throws Exception {
        mvc.perform(post("/doctor/patients/" + alex().getId() + "/notes").with(asPatel()).with(csrf())
                        .param("body", "Allergies well controlled with antihistamines."))
                .andExpect(redirectedUrl("/doctor/patients/" + alex().getId() + "#notes"))
                .andExpect(flash().attribute("success", "Note added."));

        mvc.perform(get("/patient/profile").with(asAlex()))
                .andExpect(content().string(containsString("Allergies well controlled with antihistamines.")))
                .andExpect(content().string(containsString("Dr. Jordan Patel")));

        assertThat(alexLog()).extracting(AuditEntry::getAction).containsExactly(AuditAction.ADD_NOTE);
        assertThat(alexLog().getFirst().getDetails()).isNull(); // the note text is not copied into the log
    }

    @Test
    void emptyNotesAreRejected() throws Exception {
        mvc.perform(post("/doctor/patients/" + alex().getId() + "/notes").with(asPatel()).with(csrf())
                        .param("body", "   "))
                .andExpect(flash().attribute("error", "The note is empty."));
        assertThat(alexLog()).isEmpty();
    }

    @Test
    void doctorsCannotWriteNotesForPatientsWhoArentTheirs() throws Exception {
        mvc.perform(post("/doctor/patients/999999/notes").with(asPatel()).with(csrf()).param("body", "x"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ admin pages

    @Test
    void adminSeesTheAuditLogAndApplications() throws Exception {
        mvc.perform(get("/doctor/patients/" + alex().getId()).with(asPatel()));

        mvc.perform(get("/admin/audit").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Viewed record")));
        mvc.perform(get("/admin/applications").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Jamie Rivera")));
    }

    @Test
    void onlyAdminsCanOpenAdminPages() throws Exception {
        mvc.perform(get("/admin/audit").with(asPatel())).andExpect(status().isForbidden());
        mvc.perform(get("/admin/applications").with(asPatel())).andExpect(status().isForbidden());
        mvc.perform(get("/admin/audit").with(asAlex())).andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ careers (public)

    @Test
    void anyoneCanOpenTheCareersPage() throws Exception {
        mvc.perform(get("/careers")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Doctor&#39;s assistant")));
    }

    @Test
    void validApplicationIsSaved() throws Exception {
        mvc.perform(post("/careers").with(csrf())
                        .param("fullName", "Priya Shah")
                        .param("email", "Priya.Shah@Example.com")
                        .param("phone", "(519) 555-0199")
                        .param("position", "SURGEON")
                        .param("yearsExperience", "8")
                        .param("availability", "Evenings"))
                .andExpect(redirectedUrl("/careers"))
                .andExpect(flash().attributeExists("success"));

        JobApplication saved = applications.findAllByOrderBySubmittedAtDesc().stream()
                .filter(a -> a.getFullName().equals("Priya Shah")).findFirst().orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("priya.shah@example.com");
        assertThat(saved.getPhone()).isEqualTo("5195550199");
        assertThat(saved.getPosition()).isEqualTo(Position.SURGEON);
    }

    @Test
    void applicationRulesFromTheOriginalAppStillApply() throws Exception {
        long before = applications.count();
        mvc.perform(post("/careers").with(csrf())
                        .param("fullName", "New Grad")
                        .param("email", "grad@school.ca") // allowed now: any valid email
                        .param("phone", "555-01")         // too short
                        .param("position", "PHARMACIST")
                        .param("yearsExperience", "1")    // old rule: at least 2
                        .param("availability", "Anytime"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "phone", "yearsExperience"));
        assertThat(applications.count()).isEqualTo(before);
    }

    @Test
    void careersFormNeedsTheCsrfToken() throws Exception {
        mvc.perform(post("/careers")
                        .param("fullName", "Bot").param("email", "bot@example.com").param("phone", "5195550100")
                        .param("position", "SURGEON").param("yearsExperience", "5").param("availability", "Now"))
                .andExpect(status().isForbidden());
    }
}
