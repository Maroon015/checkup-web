package com.checkup.account;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security calls this when someone submits the login form. It loads the account by
 * email; Spring then checks the typed password against the stored BCrypt hash itself.
 *
 * Old version: Patient.patientLogin() and Doctor.checkPassword() read text files line by
 * line and compared plain-text passwords with equals().
 */
@Service
public class UserAccountDetailsService implements UserDetailsService {

    private final UserAccountRepository accounts;

    public UserAccountDetailsService(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return accounts.findByEmailIgnoreCase(email.trim())
                .map(CurrentUser::new)
                // Same message whether the email or the password is wrong, so the login
                // page never tells an attacker which emails have accounts.
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));
    }
}
