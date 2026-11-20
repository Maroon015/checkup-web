package com.checkup.account;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The logged-in user, as Spring Security sees them. Stored in the session after login,
 * so controllers can ask "who is this?" with @AuthenticationPrincipal CurrentUser me.
 *
 * Holds the account id, which is the key to the security rule "a patient only ever sees
 * their own record": pages look up the patient BY THIS ID, never by an id typed in the URL.
 */
public class CurrentUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final boolean enabled;

    public CurrentUser(UserAccount account) {
        this.id = account.getId();
        this.email = account.getEmail();
        this.passwordHash = account.getPasswordHash();
        this.role = account.getRole();
        this.enabled = account.isEnabled();
    }

    public Long getId() { return id; }
    public Role getRole() { return role; }

    // Spring Security checks hasRole("PATIENT") against an authority named "ROLE_PATIENT".
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isEnabled() { return enabled; }
}
