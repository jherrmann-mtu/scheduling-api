package com.mtu.tsw.scheduling.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<UserEntity, UUID>{

    public Optional<UserEntity> findByUsername(String username);
    
}
