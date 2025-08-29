package org.fizz_buzz.cloud.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.dto.request.UserRequestDTO;
import org.fizz_buzz.cloud.dto.response.UserResponseDTO;
import org.fizz_buzz.cloud.exception.UserAlreadyExists;
import org.fizz_buzz.cloud.model.User;
import org.fizz_buzz.cloud.model.db.Constraints;
import org.fizz_buzz.cloud.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    private final HttpServletRequest httpServletRequest;
    private final HttpServletResponse httpServletResponse;


    public User signUp(UserRequestDTO request) {
        try {
            String encodedPassword = passwordEncoder.encode(request.password());
            User user = new User(request.username(), encodedPassword);

            userRepository.save(user);

            Authentication authenticationResponse = UsernamePasswordAuthenticationToken.authenticated(
                    request.username(), request.password(), List.of()
            );
            saveAuthentication(authenticationResponse);

            return user;
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException constraintException) {
                assert constraintException.getConstraintName() != null;
                if (constraintException.getConstraintName().equals(Constraints.USERNAME_UNIQUE)) {
                    throw new UserAlreadyExists("User with name \"%s\" already exists".formatted(request.username()));
                }
            }
            throw e;
        }
    }

    private void saveAuthentication(Authentication authentication) {
        SecurityContext context = SecurityContextHolder.getContext();
        context.setAuthentication(authentication);

        securityContextRepository.saveContext(context, httpServletRequest, httpServletResponse);
    }

    public UserResponseDTO signIn(UserRequestDTO request) {

        Authentication authenticationRequest = new UsernamePasswordAuthenticationToken(
                request.username(), request.password()
        );
        Authentication authenticationResponse = authenticationManager.authenticate(authenticationRequest);
        saveAuthentication(authenticationResponse);

        return new UserResponseDTO(authenticationResponse.getName());
    }
}
