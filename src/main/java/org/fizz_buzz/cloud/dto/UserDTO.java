package org.fizz_buzz.cloud.dto;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.fizz_buzz.cloud.dto.view.UserViews;

public record UserDTO(

        @NotNull
        @NotEmpty
        @Size(min = 5, max = 20)
        @JsonView(UserViews.Response.class)
        String username,

        @NotNull
        @NotEmpty
        @Size(min = 8)
        String password) {
}
