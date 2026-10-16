package com.mtu.tsw.scheduling;

import java.util.Date;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Clock;

@Component
/**
 * Hold shared constants and Beans
 */
public class Common {
    
    public static final String ADMIN_ROLE = "_ADMIN";

    @Bean
    public Clock clock() {
        return new Clock() {

            @Override
            public Date now() {
                return new Date();
            }
            
        };
    }

}
