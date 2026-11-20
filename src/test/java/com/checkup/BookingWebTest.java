package com.checkup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.checkup.account.DemoAccounts;
import com.checkup.appointment.Appointment;
import com.checkup.appointment.AppointmentRepository;
import com.checkup.appointment.AppointmentStatus;
import com.checkup.appointment.SchedulingService;
import com.checkup.appointment.TimeSlot;
import com.checkup.doctor.Doctor;
import com.checkup.doctor.DoctorRepository;
import com.checkup.patient.PatientRepository;

/**
 * The booking pages through real HTTP requests, with the demo data:
 * Alex (patient), Sam (patient), Dr. Patel (doctor with a login), Dr. Okafor (no login).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingWebTest {

    @Autowired MockMvc mvc;
    @Autowired DoctorRepository doctors;
    @Autowired PatientRepository patients;
    @Autowired AppointmentRepository appointments;
    @Autowired SchedulingService scheduling;

    private Doctor patel() {
        return doctors.findByEmailIgnoreCase(DemoAccounts.DOCTOR_EMAIL).orElseThrow();
    }

    /** The first free slot with Dr. Patel in the next two weeks. */
    private TimeSlot firstFreeSlotWithPatel() {
        LocalDate today = scheduling.today();
        return IntStream.range(0, 14).mapToObj(today::plusDays)
                .flatMap(d -> scheduling.freeSlots(patel().getId(), d).stream())
                .findFirst().orElseThrow();
    }

    private Appointment appointmentWithReason(String reason) {
        return appointments.findAll().stream().filter(a -> a.getReason().equals(reason)).findFirst().orElseThrow();
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void patientCanBrowseDoctorsAndTimes() throws Exception {
        mvc.perform(get("/patient/book"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dr. Jordan Patel")));

        TimeSlot slot = firstFreeSlotWithPatel();
        mvc.perform(get("/patient/book/" + patel().getId()).param("date", slot.start().toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("start=" + slot.param())));
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void patientCanBookAFreeSlot() throws Exception {
        TimeSlot slot = firstFreeSlotWithPatel();

        mvc.perform(post("/patient/book/" + patel().getId()).with(csrf())
                        .param("start", slot.param())
                        .param("reason", "Booked from the web test"))
                .andExpect(redirectedUrl("/patient"))
                .andExpect(flash().attribute("success", containsString("requested")));

        Appointment booked = appointmentWithReason("Booked from the web test");
        assertThat(booked.getStartTime()).isEqualTo(slot.start());
        assertThat(booked.getStatus()).isEqualTo(AppointmentStatus.REQUESTED);
        assertThat(booked.getPatient().getEmail()).isEqualTo(DemoAccounts.PATIENT_EMAIL);
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void bookingATakenSlotSendsYouBackWithAReason() throws Exception {
        TimeSlot slot = firstFreeSlotWithPatel();
        mvc.perform(post("/patient/book/" + patel().getId()).with(csrf())
                .param("start", slot.param()).param("reason", "First"));

        mvc.perform(post("/patient/book/" + patel().getId()).with(csrf())
                        .param("start", slot.param()).param("reason", "Second"))
                .andExpect(redirectedUrl("/patient/book/" + patel().getId() + "?date=" + slot.start().toLocalDate()))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void patientGets404ForSomeoneElsesAppointment() throws Exception {
        Appointment samsAppointment = appointmentWithReason("Follow-up on chest pain");
        mvc.perform(post("/patient/appointments/" + samsAppointment.getId() + "/cancel").with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(appointments.findById(samsAppointment.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.REQUESTED);
    }

    @Test
    @WithUserDetails(DemoAccounts.DOCTOR_EMAIL)
    void doctorCanConfirmTheirOwnRequestedAppointment() throws Exception {
        Appointment samsWithPatel = appointmentWithReason("Knee pain follow-up");
        mvc.perform(post("/doctor/appointments/" + samsWithPatel.getId() + "/confirm").with(csrf()))
                .andExpect(redirectedUrl("/doctor"))
                .andExpect(flash().attribute("success", "Appointment confirmed."));
        assertThat(appointments.findById(samsWithPatel.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.CONFIRMED);
    }

    @Test
    @WithUserDetails(DemoAccounts.DOCTOR_EMAIL)
    void doctorGets404ForAnotherDoctorsAppointment() throws Exception {
        Appointment withOkafor = appointmentWithReason("Follow-up on chest pain");
        mvc.perform(post("/doctor/appointments/" + withOkafor.getId() + "/cancel").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails(DemoAccounts.DOCTOR_EMAIL)
    void doctorSeesTheirWeeklyHours() throws Exception {
        mvc.perform(get("/doctor/hours"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Monday")))
                .andExpect(content().string(containsString("30 min")));
    }

    @Test
    @WithUserDetails(DemoAccounts.PATIENT_EMAIL)
    void patientsCannotManageDoctorHours() throws Exception {
        mvc.perform(get("/doctor/hours")).andExpect(status().isForbidden());
    }
}
