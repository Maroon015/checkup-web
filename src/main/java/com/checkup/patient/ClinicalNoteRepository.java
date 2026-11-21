package com.checkup.patient;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.Repository;

/** Notes can be added and read, never edited or deleted (no such methods exist). */
public interface ClinicalNoteRepository extends Repository<ClinicalNote, Long> {

    ClinicalNote save(ClinicalNote note);

    /** A patient's notes, newest first, with the writing doctor loaded. */
    @EntityGraph(attributePaths = "doctor")
    List<ClinicalNote> findByPatient_IdOrderByCreatedAtDescIdDesc(Long patientId);
}
