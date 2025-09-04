package org.fizz_buzz.cloud.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fizz_buzz.cloud.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(force = true)
@Setter
public class CustomUserDetails implements UserDetails {

    @Serial
    private static final long serialVersionUID = 42L;

    @Getter
    @JsonProperty("id")
    private final long id;
    @JsonProperty("username")
    private final String username;
    @JsonProperty("password")
    private final String password;
    @JsonProperty("authorities")
    private final List<GrantedAuthority> authorities = new ArrayList<>();

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getName();
        this.password = user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
