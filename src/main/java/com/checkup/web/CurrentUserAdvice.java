package com.checkup.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.checkup.account.CurrentUser;

/**
 * Runs before every controller and puts "currentUser" into the page model (null when
 * logged out), so the navigation bar on every page can show "Log in" or "Log out".
 */
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("currentUser")
    public CurrentUser currentUser(@AuthenticationPrincipal CurrentUser me) {
        return me;
    }
}
