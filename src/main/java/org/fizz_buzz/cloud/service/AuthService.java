package org.fizz_buzz.cloud.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
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

import java.sql.SQLException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;


    public UserResponseDTO signUp(UserRequestDTO request) {

//        if (userRepository.existsByName(request.username())){
//            throw new UserAlreadyExists("User with name %s already exists".formatted(request.username()));
//        }

        User savedUser = null;
        try {
            savedUser = userRepository.save(new User(request.username(), passwordEncoder.encode(request.password())));
        } catch (DataIntegrityViolationException e) {

            if (e.getRootCause() instanceof SQLException){
                if (((SQLException) e.getRootCause()).getSQLState().equals("23505")){
                    throw new UserAlreadyExists("User with name %s already exists".formatted(request.username()));
                }
            }

            throw e;
        }

        return new UserResponseDTO(
                savedUser.getName());
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
