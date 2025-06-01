package org.fizz_buzz.cloud.service;

import org.fizz_buzz.cloud.dto.UserDTO;
import org.fizz_buzz.cloud.model.User;
import org.fizz_buzz.cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserDTO signUp(UserDTO request){

        var savedUser = userRepository.save(new User(request.username(), passwordEncoder.encode(request.password())));

        return new UserDTO(
                savedUser.getName(),
                "");
    }
}
