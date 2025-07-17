package org.fizz_buzz.cloud.security;

import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
public class ExtendedUserDetails implements UserDetails {

    private final User currentUser;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return currentUser.getPassword();
    }

    @Override
    public String getUsername() {
        return currentUser.getName();
    }

    public long getId(){
        return currentUser.getId();
    }
}
