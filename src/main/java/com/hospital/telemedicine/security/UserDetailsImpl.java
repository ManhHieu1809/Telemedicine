package com.hospital.telemedicine.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hospital.telemedicine.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Data
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private Long patientId;
    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;


    public static UserDetailsImpl build(User user,Long patientId) {
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRoles().name());

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                patientId,
                user.getPasswordHash(),
                Collections.singletonList(authority)
        );
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
