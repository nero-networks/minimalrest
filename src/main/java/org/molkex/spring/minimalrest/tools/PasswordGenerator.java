package org.molkex.spring.minimalrest.tools;

import java.security.SecureRandom;
import java.util.*;

public class PasswordGenerator extends SecureRandom {
    static List<Character> UPPER = range(65, 90);
    static List<Character> LOWER = range(97, 122);
    static List<Character> NUMBERS = range(48, 57);
    static List<Character> SPECIAL = new ArrayList<Character>() {{
        addAll(range(33, 47));
        addAll(range(58, 64));
        addAll(range(91, 96));
        addAll(range(123, 126));
    }};

    static List<Character> range(int from, int to) {
        List<Character> chars = new ArrayList<>(1 + to - from);
        for (int i=from; i<=to; i++) chars.add((char)i);
        return chars;
    }

    int length = 12;
    int upperCount = 4;
    int numbersCount = 3;
    int specialCount = 2;
    int shuffleCount = 10;

    List<Character> upper = UPPER;
    List<Character> lower = LOWER;
    List<Character> numbers = NUMBERS;
    List<Character> special = SPECIAL;
    List<Character> excluded = Arrays.asList('"', '\'', '\\');

    public PasswordGenerator upper(List<Character> upper) {
        this.upper = upper;
        return this;
    }

    public PasswordGenerator lower(List<Character> lower) {
        this.lower = lower;
        return this;
    }

    public PasswordGenerator numbers(List<Character> numbers) {
        this.numbers = numbers;
        return this;
    }

    public PasswordGenerator special(List<Character> special) {
        this.special = special;
        return this;
    }

    public PasswordGenerator excluded(List<Character> excluded) {
        this.excluded = excluded;
        return this;
    }

    public PasswordGenerator length(int length) {
        this.length = length;
        return this;
    }

    public PasswordGenerator upperCount(int upperCount) {
        this.upperCount = upperCount;
        return this;
    }

    public PasswordGenerator numbersCount(int numbersCount) {
        this.numbersCount = numbersCount;
        return this;
    }

    public PasswordGenerator specialCount(int specialCount) {
        this.specialCount = specialCount;
        return this;
    }

    public PasswordGenerator shuffleCount(int shuffleCount) {
        this.shuffleCount = shuffleCount;
        return this;
    }

    public String next() {
        List<Character> chars = new LinkedList<>();

        for (int i=0; i<upperCount; i++) chars.add(next(UPPER));
        for (int i=0; i<numbersCount; i++) chars.add(next(NUMBERS));
        for (int i=0; i<specialCount; i++) chars.add(next(SPECIAL));
        for (int i=0; i<length-upperCount-numbersCount-specialCount; i++) chars.add(next(LOWER));

        for (int i=0; i<shuffleCount; i++) Collections.shuffle(chars);

        return chars.stream().collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
    }

    protected char next(List<Character> list) {
        char c = list.get(nextInt(list.size()));
        return excluded.contains(c) ? next(list) : c;
    }
}
