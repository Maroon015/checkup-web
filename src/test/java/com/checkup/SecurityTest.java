package com.checkup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.account.DemoAccounts;
import com.checkup.account.Role;
import com.checkup.account.UserAccount;
import com.checkup.account.UserAccountRepository;
import com.checkup.patient.PatientRepository;

/**
 * Tests the security rules from the outside, the way a browser (or an attacker) would hit
 * them. Uses the whole app with the demo data: Alex (patient), Dr. Patel (doctor), admin.
 *
 * @Transactional rolls back anything a test saves, e.g. a new registration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityTest {

    @Autowired MockMvc mvc;
    @Autowired UserAccountRepository accounts;
    @Autowired PatientRepository patients;
    @Autowired PasswordEncoder passwordEncoder;

    @Nested
    class LoggedOut {

        @Test
        void canSeePublicPages() throws Exception {
            mvc.perform(get("/")).andExpect(status().isOk());
            mvc.perform(get("/login"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Log in as demo patient")))
                    .andExpect(content().string(containsString("Log in as demo doctor")));
            mvc.perform(get("/register")).andExpect(status().isOk());
        }

        @Test
        void isSentToLoginForPrivatePages() throws Exception {
            for (String page : new String[] {"/patient", "/doctor", "/admin", "/dashboard"}) {
                mvc.perform(get(page))
                        .andExpect(status().is3xxRedirection())
                        .andExpect(redirectedUrl("/login"));
            }
        }
    }

    @Nested
    class Login {

        @Test
        void demoPatientCanLogIn() throws Exception {
            mvc.perform(formLogin("/login").userParameter("email")
                            .user(DemoAccounts.PATIENT_EMAIL).password(DemoAccounts.PASSWORD))
                    .andExpect(authenticated().withRoles("PATIENT"))
                    .andExpect(redirectedUrl("/dashboard"));
        }

        @Test
        void emailIsNotCaseSensitive() throws Exception {
            mvc.perform(formLogin("/login").userParameter("email")
                            .user("Jordan.Patel@Example.com").password(DemoAccounts.PASSWORD))
                    .andExpect(authenticated().withRoles("DOCTOR"));
        }

        @Test
        void wrongPasswordIsRejected() throws Exception {
            mvc.perform(formLogin("/login").userParameter("email")
                            .user(DemoAccounts.PATIENT_EMAIL).password("wrong-password"))
                    .andExpect(unauthenticated())
                    .andExpect(redirectedUrl("/login?error"));
        }

        @Test
        void passwordsAreStoredAsBcryptHashesNotPlainText() {
            UserAccount alex = accounts.findByEmailIgnoreCase(DemoAccounts.PATIENT_EMAIL).orElseThrow();
            assertThat(alex.getPasswordHash())
                    .startsWith("{bcrypt}")
                    .doesNotContain(DemoAccounts.PASSWORD);
            assertThat(passwordEncoder.matches(DemoAccounts.PASSWORD, alex.getPasswordHash())).isTrue();
        }
    }

    @Nested
    class AsDemoPatient {

        @Test
        @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
        void dashboardGoesToPatientArea() throws Exception {
            mvc.perform(get("/dashboard")).andExpect(redirectedUrl("/patient"));
        }

        @Test
        @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
        void seesOnlyTheirOwnAppointments() throws Exception {
            mvc.perform(get("/patient"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Hi, <span>Alex</span>")))
                    .andExpect(content().string(containsString("Annual check-up")))       // Alex's
                    .andExpect(content().string(not(containsString("Follow-up on chest pain")))) // Sam's
                    .andExpect(content().string(not(containsString("Knee pain"))));            // Sam's
        }

        @Test
        @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
        void cannotOpenDoctorOrAdminPages() throws Exception {
            mvc.perform(get("/doctor")).andExpect(status().isForbidden());
            mvc.perform(get("/admin")).andExpect(status().isForbidden());
        }
    }

    @Nested
    class AsDemoDoctor {

        @Test
        @WithUserDetails(DemoAccounts.DOCTOR_EMAIL)
        void seesOnlyAppointmentsBookedWithThem() throws Exception {
            mvc.perform(get("/doctor"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Annual check-up")))              // with Dr. Patel
                    .andExpect(content().string(not(containsString("Follow-up on chest pain")))); // with Dr. Okafor
        }

        @Test
        @WithUserDetails(DemoAccounts.DOCTOR_EMAIL)
        void cannotOpenPatientOrAdminPages() throws Exception {
            mvc.perform(get("/patient")).andExpect(status().isForbidden());
            mvc.perform(get("/admin")).andExpect(status().isForbidden());
        }
    }

    @Nested
    class AsAdmin {

        @Test
        @WithUserDetails(DemoAccounts.ADMIN_EMAIL)
        void canOpenAdminPage() throws Exception {
            mvc.perform(get("/admin")).andExpect(status().isOk());
        }
    }

    @Nested
    class Registration {

        @Test
        void createsPatientAccountWithHashedPassword() throws Exception {
            mvc.perform(post("/register").with(csrf())
                            .param("firstName", "Riley")
                            .param("lastName", "Nguyen")
                            .param("email", "Riley.Nguyen@Example.com")
                            .param("dateOfBirth", "1999-07-15")
                            .param("password", "correct-horse-battery")
                            .param("confirmPassword", "correct-horse-battery"))
                    .andExpect(redirectedUrl("/login"));

            UserAccount account = accounts.findByEmailIgnoreCase("riley.nguyen@example.com").orElseThrow();
            assertThat(account.getRole()).isEqualTo(Role.PATIENT);
            assertThat(account.getEmail()).isEqualTo("riley.nguyen@example.com");
            assertThat(account.getPasswordHash()).doesNotContain("correct-horse-battery");
            assertThat(patients.findByUser_Id(account.getId())).isPresent();

            // ...and the new account can log in.
            mvc.perform(formLogin("/login").userParameter("email")
                            .user("riley.nguyen@example.com").password("correct-horse-battery"))
                    .andExpect(authenticated().withRoles("PATIENT"));
        }

        @Test
        void rejectsAnEmailThatAlreadyHasAnAccount() throws Exception {
            mvc.perform(post("/register").with(csrf())
                            .param("firstName", "Alex")
                            .param("lastName", "Again")
                            .param("email", DemoAccounts.PATIENT_EMAIL.toUpperCase())
                            .param("dateOfBirth", "1990-01-01")
                            .param("password", "another-password")
                            .param("confirmPassword", "another-password"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("register"))
                    .andExpect(model().attributeHasFieldErrors("form", "email"));
        }

        @Test
        void rejectsBadInputWithMessagesPerField() throws Exception {
            mvc.perform(post("/register").with(csrf())
                            .param("firstName", "")
                            .param("lastName", "Nguyen")
                            .param("email", "not-an-email")
                            .param("dateOfBirth", "2999-01-01")
                            .param("password", "short")
                            .param("confirmPassword", "different"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("form",
                            "firstName", "email", "dateOfBirth", "password", "confirmPassword"));
        }

        @Test
        void formsWithoutTheCsrfTokenAreRefused() throws Exception {
            // A fake form on another website wouldn't have the token.
            mvc.perform(post("/register")
                            .param("firstName", "Evil")
                            .param("lastName", "Site")
                            .param("email", "evil@example.com")
                            .param("dateOfBirth", "1990-01-01")
                            .param("password", "password123")
                            .param("confirmPassword", "password123"))
                    .andExpect(status().isForbidden());
            assertThat(accounts.existsByEmailIgnoreCase("evil@example.com")).isFalse();
        }
    }
}
