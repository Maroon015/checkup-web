package com.checkup.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.checkup.audit.AuditLogRepository;
import com.checkup.careers.JobApplicationRepository;

/** Admin-only pages (SecurityConfig locks /admin/** to the ADMIN role). */
@Controller
public class AdminController {

    private final AuditLogRepository auditLog;
    private final JobApplicationRepository applications;

    public AdminController(AuditLogRepository auditLog, JobApplicationRepository applications) {
        this.auditLog = auditLog;
        this.applications = applications;
    }

    /** The most recent 100 audit entries across the system. */
    @GetMapping("/admin/audit")
    public String audit(Model model) {
        model.addAttribute("entries", auditLog.findTop100ByOrderByOccurredAtDescIdDesc());
        model.addAttribute("total", auditLog.count());
        return "admin/audit";
    }

    @GetMapping("/admin/applications")
    public String applications(Model model) {
        model.addAttribute("applications", applications.findAllByOrderBySubmittedAtDesc());
        return "admin/applications";
    }
}
