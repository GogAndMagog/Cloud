package org.fizz_buzz.cloud.repository;

import org.fizz_buzz.cloud.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
