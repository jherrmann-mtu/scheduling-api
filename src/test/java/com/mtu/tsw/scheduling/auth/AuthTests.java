package com.mtu.tsw.scheduling.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collection;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.mtu.tsw.scheduling.Common;
import com.mtu.tsw.scheduling.AuthController.AuthRequest;
import com.mtu.tsw.scheduling.security.JwtService;
import com.mtu.tsw.scheduling.security.SecurityConfig;
import com.mtu.tsw.scheduling.security.UserDetailsServiceImpl;


@WebMvcTest
@Import({JwtService.class, Common.class, SecurityConfig.class})
public class AuthTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired 
    private PasswordEncoder encoder;

    @MockitoBean
    private UserDetailsServiceImpl userService;

    private JsonMapper mapper = new JsonMapper();

    @BeforeEach
    public void setup() {
        Mockito.when(userService.loadUserByUsername("testuser")).thenReturn(new UserDetails() {

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                throw new UnsupportedOperationException("Unimplemented method 'getAuthorities'");
            }

            @Override
            public @Nullable String getPassword() {
                return encoder.encode("testpwd");
            }

            @Override
            public String getUsername() {
                return "testuser";
            }
            
        });
    }

    @Test
    public void validLogin() throws Exception {
        mockMvc.perform(
            post("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AuthRequest("testuser", "testpwd")))
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.token").isString())
        ;
    }

    @Test
    public void invalidLogin_BadUser() throws Exception {
        mockMvc.perform(
            post("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AuthRequest("eviluser", "testpwd")))
        )
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Invalid credentials"))
        ;
    }

    @Test
    public void invalidLogin_BadPwd() throws Exception {
        mockMvc.perform(
            post("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new AuthRequest("testuser", "wrongpwd")))
        )
        .andExpect(status().isForbidden())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.message").value("Invalid credentials"))
        ;
    }

    @Test
    public void invalidLogin_Malformed() throws Exception {
        mockMvc.perform(
            post("/api/auth/login")
                .accept(MediaType.APPLICATION_JSON) // no body
        )
        .andExpect(status().isBadRequest())
        ;
    }

}
