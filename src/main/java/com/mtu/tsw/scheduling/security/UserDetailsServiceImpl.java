package com.mtu.tsw.scheduling.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.mtu.tsw.scheduling.user.UserRepository;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepo;

    public UserDetailsServiceImpl(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var userOpt = userRepo.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new UsernameNotFoundException("Could not find user '" + username + "'");
        }
        return userOpt.get();
    }
    
}
