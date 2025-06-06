package org.fizz_buzz.cloud.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.request.UserRequestDTO;
import org.fizz_buzz.cloud.dto.response.UserResponseDTO;
import org.fizz_buzz.cloud.exception.UserAlreadyExists;
import org.fizz_buzz.cloud.model.User;
import org.fizz_buzz.cloud.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.hibernate.exception.ConstraintViolationException;


import java.sql.SQLException;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;


    public UserResponseDTO signUp(UserRequestDTO request) {

        User savedUser;

        try {
            savedUser = userRepository.save(new User(request.username(), passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException e) {

            if (e.getCause() instanceof ConstraintViolationException
                    && (((ConstraintViolationException) e.getCause()).getSQLState().equals("23505"))
                    && (Objects.equals(((ConstraintViolationException) e.getCause()).getConstraintName(), "users_name"))) {
                throw new UserAlreadyExists("User with name \"%s\" already exists".formatted(request.username()));
            } else {
                throw e;
            }
        }

        return new UserResponseDTO(savedUser.getName());
    }

    public UserResponseDTO signIn(UserRequestDTO request,
                                  HttpServletRequest httpServletRequest,
                                  HttpServletResponse httpServletResponse) {

        Authentication authenticationRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password());
        Authentication authenticationResponse =
                this.authenticationManager.authenticate(authenticationRequest);
        var securityContext = new SecurityContextImpl(authenticationResponse);

        securityContextRepository.saveContext(securityContext, httpServletRequest, httpServletResponse);

        return new UserResponseDTO(authenticationResponse.getName());
    }
}
