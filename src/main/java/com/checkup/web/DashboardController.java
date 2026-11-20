package com.checkup.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.checkup.account.CurrentUser;

/** After login everyone lands on /dashboard, which sends them to their own area. */
@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal CurrentUser me) {
        return switch (me.getRole()) {
            case PATIENT -> "redirect:/patient";
            case DOCTOR -> "redirect:/doctor";
            case ADMIN -> "redirect:/admin";
        };
    }
}
