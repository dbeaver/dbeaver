// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced.regex;

public class XegerTest
{
    public static void main(final String[] args) {
        doTest("[a-z]{5,15}\\@[a-z]{5,10}\\.com");
        doTest("[ab]{1,2}|[xyz]{5}");
        doTest("[0-9]+");
        doTest("[A-z]?");
        doTest("[A-z]*");
        doTest("[a-zA-Z0-9]{3,}");
        doTest("$[0-9]{1,5}\\.[0-9]{2}");
    }
    
    private static void doTest(final String regex) {
        final Xeger generator = new Xeger(regex);
        final String text = generator.generate();
        System.out.printf("%35s --> %-25s %s\n", regex, text, String.valueOf(text.matches(regex)).toUpperCase());
    }
}
