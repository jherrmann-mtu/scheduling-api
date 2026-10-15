package com.mtu.tsw.scheduling.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.mtu.tsw.scheduling.security.JwtService;

import io.jsonwebtoken.Clock;

public class JwtTests {

    private static Clock realClock;

    @BeforeAll
    public static void initRealClock() {
        realClock = new Clock() {

            @Override
            public Date now() {
                return new Date();
            }
            
        };
    }

    @Test 
    public void randomlyGeneratedSecretKey() throws NoSuchAlgorithmException {
        // use reflection to call private method
        JwtService jwt = new JwtService(realClock);
        try {
            Method genKey = JwtService.class.getDeclaredMethod("generateKey");
            genKey.setAccessible(true);

            String key1 = genKey.invoke(jwt).toString();
            String key2 = genKey.invoke(jwt).toString();

            assertEquals(key1.length(), 256, "Key should be 265 chars long");
            assertEquals(key2.length(), 256, "Key should be 265 chars long");
            assertNotEquals(key1, key2, "Keys should be randomly generated and not match");
            

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            fail("Caught exception while trying to reflect into JWT service", e);
        }
    }

    @Test 
    public void generatedTokenContainsUsername() throws NoSuchAlgorithmException {
        JwtService jwt = new JwtService(realClock);
        String token = jwt.generateToken("test-user");
        Optional<String> usernameOpt = jwt.getUsername(token);
        assertTrue(usernameOpt.isPresent(), "Username should be present");
        assertEquals(usernameOpt.get(), "test-user", "Username should match");
    }

    @Test 
    public void generatedTokenExpires() throws NoSuchAlgorithmException {
        Clock fakeClock = Mockito.mock(Clock.class);

        Date constructTime = new Date(Instant.parse("2026-10-14T01:00:00Z").toEpochMilli());
        Date halfwayTime   = new Date(Instant.parse("2026-10-14T01:30:00Z").toEpochMilli());
        Date almostExpired = new Date(Instant.parse("2026-10-14T01:59:59Z").toEpochMilli());
        Date expired       = new Date(Instant.parse("2026-10-14T02:00:00Z").toEpochMilli());
        Date wayLater      = new Date(Instant.parse("2026-10-14T03:30:00Z").toEpochMilli());
        
        
        when(fakeClock.now()).thenReturn(constructTime);
        
        JwtService jwt = new JwtService(fakeClock);
        String token = jwt.generateToken("test-user");

        assertTrue(jwt.isTokenUnexpired(token), "Token should be instantly available");

        when(fakeClock.now()).thenReturn(halfwayTime);
        assertTrue(jwt.isTokenUnexpired(token), "Token should be available within an hour");

        when(fakeClock.now()).thenReturn(almostExpired);
        assertTrue(jwt.isTokenUnexpired(token), "Token should be available within an hour exactly");
        
        when(fakeClock.now()).thenReturn(expired);
        assertFalse(jwt.isTokenUnexpired(token), "Token should be expired at exactly one hour");

        when(fakeClock.now()).thenReturn(wayLater);
        assertFalse(jwt.isTokenUnexpired(token), "Token should stay expired");
    }
    
}
