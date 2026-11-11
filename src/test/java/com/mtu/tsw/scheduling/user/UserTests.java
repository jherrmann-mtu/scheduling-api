package com.mtu.tsw.scheduling.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Limit;

@DataJpaTest()
public class UserTests {
    
    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository user;

    private final Limit BIG_LIMIT = Limit.of(50);

    @Test 
    public void testUserFuzzySearch() {
        var u1 = em.persist(new UserEntity("John Smith", "pwd1"));
        var u2 = em.persist(new UserEntity("Alice Smith", "pwd1"));
        var u3 = em.persist(new UserEntity("David Martinez", "pwd1"));
        var u4 = em.persist(new UserEntity("John Silver", "pwd1"));
        em.flush();

        assertEquals(List.of(u1, u4), user.searchByUsername("%John%", BIG_LIMIT));
        assertEquals(List.of(u1, u2), user.searchByUsername("%SMITH%", BIG_LIMIT));
        assertEquals(List.of(u3), user.searchByUsername("%mar%", BIG_LIMIT));
        assertEquals(List.of(u2), user.searchByUsername("%Alice Smith%", BIG_LIMIT));
        assertEquals(List.of(), user.searchByUsername("%Uecker%", BIG_LIMIT));
    }

}
