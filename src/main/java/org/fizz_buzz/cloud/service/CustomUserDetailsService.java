package org.fizz_buzz.cloud.service;

import org.fizz_buzz.cloud.dto.CustomUserDetails;
import org.fizz_buzz.cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        var user = userRepository.findByName(username);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException(username);
        }

        return new CustomUserDetails(user.get());
    }
}
