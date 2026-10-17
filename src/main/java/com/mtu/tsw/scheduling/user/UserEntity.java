package com.mtu.tsw.scheduling.user;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
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

    /**
     * Create new user.
     * @param username Username
     * @param password Encrypted password
     */
    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
    }


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

     @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity other = (UserEntity) o;
        return Objects.equals(username, other.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

}
