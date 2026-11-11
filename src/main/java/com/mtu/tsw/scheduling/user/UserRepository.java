package com.mtu.tsw.scheduling.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserEntity, UUID>{

    public Optional<UserEntity> findByUsername(String username);

    @Query ("SELECT u FROM Users u WHERE LOWER(u.username) LIKE LOWER(:usernameQuery)")
    public List<UserEntity> searchByUsername(String usernameQuery, Limit limit);
    
}
