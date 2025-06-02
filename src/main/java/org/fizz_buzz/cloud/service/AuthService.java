package org.fizz_buzz.cloud.service;

import jakarta.servlet.http.HttpSession;
import org.fizz_buzz.cloud.dto.UserDTO;
import org.fizz_buzz.cloud.model.User;
import org.fizz_buzz.cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;


    public UserDTO signUp(UserDTO request){

        var savedUser = userRepository.save(new User(request.username(), passwordEncoder.encode(request.password())));

        return new UserDTO(
                savedUser.getName(),
                "");
    }

    public UserDTO signIn(UserDTO request,
                          HttpSession session){

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
        Authentication authenticationResponse =
                this.authenticationManager.authenticate(authenticationRequest);

        if (authenticationResponse != null &&
                authenticationResponse.isAuthenticated()) {
            UserDTO responseDto = new UserDTO(authenticationResponse.getName(), "");

            SecurityContextHolder.getContext().setAuthentication(authenticationResponse);

            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            return responseDto;
        } else {
            throw new AuthenticationCredentialsNotFoundException("Credentials not found!");
        }
    }
}
