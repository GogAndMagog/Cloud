package org.fizz_buzz.cloud.repository;

import org.fizz_buzz.cloud.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByName(String name);
}
