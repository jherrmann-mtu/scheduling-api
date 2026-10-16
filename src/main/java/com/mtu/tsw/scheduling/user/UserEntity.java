package com.mtu.tsw.scheduling.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.mtu.tsw.scheduling.Common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Represents a user in the database.
 */
@Entity(name="user")
public class UserEntity implements UserDetails {

    public UserEntity() {}

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true, length=64)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 10)
    private String role;

    public UUID getId() {
        return id;
    }

    @Override 
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    public void setRole(boolean isAdmin) {
        if (isAdmin)
            this.role = Common.ADMIN_ROLE;
        else
            this.role = "";
    } 

    public Optional<String> getRole() {
        if (role == null || role.isBlank())
            return Optional.empty();
        return Optional.of(role);
    }

}
