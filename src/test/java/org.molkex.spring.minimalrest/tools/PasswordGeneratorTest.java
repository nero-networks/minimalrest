package org.molkex.spring.minimalrest.tools;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class PasswordGeneratorTest {

    @Test
    public void testPassword() {
        String passwd = new PasswordGenerator()
                .length(32)
                .upperCount(9)
                .numbersCount(9)
                .specialCount(5)
                .shuffleCount(10)
                .next();

        assertEquals(32, passwd.length());
        assertEquals("Wrong number of upper case letters", 9, count(PasswordGenerator.UPPER, passwd));
        assertEquals("Wrong number of numbers", 9, count(PasswordGenerator.NUMBERS, passwd));
        assertEquals("Wrong number of special characters", 5, count(PasswordGenerator.SPECIAL, passwd));
    }

    private long count(List<Character> chars, String passwd) {
        return Arrays.stream(passwd.split(""))
                    .filter(s -> chars.contains(s.charAt(0)))
                    .count();
    }

}
