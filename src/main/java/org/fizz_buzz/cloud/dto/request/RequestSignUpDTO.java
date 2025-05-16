package org.fizz_buzz.cloud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RequestSignUpDTO(

        @NotNull
        @NotEmpty
        @Size(min = 5, max = 20)
        String username,

        @NotNull
        @NotEmpty
        @Size(min = 8)
        String password) {
}
