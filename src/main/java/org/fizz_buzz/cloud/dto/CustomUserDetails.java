package org.fizz_buzz.cloud.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.fizz_buzz.cloud.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Setter
public class CustomUserDetails implements UserDetails {

    @JsonProperty("username")
    private String name;

    @JsonProperty("password")
    private String pass;

    @JsonProperty("authorities")
    private List<? extends GrantedAuthority> authorities = new ArrayList<>();


    public CustomUserDetails(User user) {
        this.name = user.getName();
        this.pass = user.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return pass;
    }

    @Override
    public String getUsername() {
        return name;
    }
}