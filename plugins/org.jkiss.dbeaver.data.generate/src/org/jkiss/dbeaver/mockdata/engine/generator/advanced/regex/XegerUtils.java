// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine.generator.advanced.regex;

import java.util.Random;

public class XegerUtils
{
    public static final int getRandomInt(final int min, final int max, final Random random) {
        final int dif = max - min;
        final float number = random.nextFloat();
        return min + Math.round(number * dif);
    }
}
