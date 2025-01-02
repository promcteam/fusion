package studio.magemonkey.fusion.util;

import org.junit.jupiter.api.Test;

public class CharacterIncrementerTest {

    @Test
    public void testNextChar() {
        // Test the getNextChar method
        char testChar1 = '0'; // Should return '1'
        char testChar2 = '9'; // Should return 'a'
        char testChar3 = 'z'; // Should return 'A'
        char testChar4 = 'Z'; // Should return 'Z' (no wrap around)

        char generatedChar1 = CharacterIncrementer.getNextChar(testChar1);
        char generatedChar2 = CharacterIncrementer.getNextChar(testChar2);
        char generatedChar3 = CharacterIncrementer.getNextChar(testChar3);
        char generatedChar4 = CharacterIncrementer.getNextChar(testChar4);

        System.out.println("Next char for '0': " + generatedChar1);
        assert generatedChar1 == '1' : "Expected '1' but got " + generatedChar1;
        System.out.println("Next char for '9': " + generatedChar2);
        assert generatedChar2 == 'a' : "Expected 'a' but got " + generatedChar2;
        System.out.println("Next char for 'z': " + generatedChar3);
        assert generatedChar3 == 'A' : "Expected 'A' but got " + generatedChar3;
        System.out.println("Next char for 'Z': " + generatedChar4);
        assert generatedChar4 == 'Z' : "Expected 'Z' but got " + generatedChar4;
    }
}
