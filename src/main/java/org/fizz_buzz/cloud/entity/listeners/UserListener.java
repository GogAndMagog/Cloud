package org.fizz_buzz.cloud.entity.listeners;

import jakarta.persistence.PostPersist;
import lombok.RequiredArgsConstructor;
import org.fizz_buzz.cloud.entity.User;
import org.fizz_buzz.cloud.service.StorageService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserListener {

    private final StorageService storageService;

    @PostPersist
    public void newUserListener(User user) {
        storageService.createUserDirectory(user.getId());
    }
}
