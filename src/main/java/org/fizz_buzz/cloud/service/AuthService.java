package org.fizz_buzz.cloud.service;

import org.fizz_buzz.cloud.dto.request.RequestSignUpDTO;
import org.fizz_buzz.cloud.dto.response.ResponseSignUpDTO;
import org.fizz_buzz.cloud.model.User;
import org.fizz_buzz.cloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    public ResponseSignUpDTO signUp(RequestSignUpDTO request){

        return new ResponseSignUpDTO(userRepository.save(new User(request.username(), request.password())).getName());
    }
}
