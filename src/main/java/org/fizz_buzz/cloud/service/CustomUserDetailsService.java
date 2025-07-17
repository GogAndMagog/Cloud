package org.fizz_buzz.cloud.service;

import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.repository.UserRepository;
import org.fizz_buzz.cloud.security.ExtendedUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        var userEntity = userRepository.findByName(username);

        return userEntity
                .map(ExtendedUserDetails::new)
//                .map(user -> User
//                        .withUsername(userEntity.get().getName())
//                        .password(userEntity.get().getPassword())
//                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(username));
    }
}
