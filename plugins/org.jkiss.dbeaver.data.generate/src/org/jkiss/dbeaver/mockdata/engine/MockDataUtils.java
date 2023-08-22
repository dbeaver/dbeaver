// 
// Decompiled by Procyon v0.5.36
// 

package org.jkiss.dbeaver.mockdata.engine;

import org.jkiss.dbeaver.DBException;
import java.util.List;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import java.util.Iterator;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.code.NotNull;
import java.math.BigDecimal;
import org.jkiss.utils.IntKeyMap;
import java.util.Random;

public class MockDataUtils
{
    public static int LONG_PRECISION;
    public static int INTEGER_PRECISION;
    public static int SHORT_PRECISION;
    public static int BYTE_PRECISION;
    private static final Random random;
    private static IntKeyMap<Integer> degrees;
    
    static {
        MockDataUtils.LONG_PRECISION = String.valueOf(Long.MAX_VALUE).length();
        MockDataUtils.INTEGER_PRECISION = String.valueOf(Integer.MAX_VALUE).length();
        MockDataUtils.SHORT_PRECISION = String.valueOf(32767).length();
        MockDataUtils.BYTE_PRECISION = String.valueOf(127).length();
        random = new Random();
        MockDataUtils.degrees = (IntKeyMap<Integer>)new IntKeyMap();
    }
    
    public static Object generateNumeric(final Integer precision, final Integer scale, final Double min, final Double max) {
        if ((scale == null || scale == 0) && precision != null && precision != 0) {
            if (precision <= MockDataUtils.BYTE_PRECISION) {
                return (byte)randomInteger(degree(precision), min, max);
            }
            if (precision <= MockDataUtils.SHORT_PRECISION) {
                return (short)randomInteger(degree(precision), min, max);
            }
            if (precision <= MockDataUtils.INTEGER_PRECISION) {
                return randomInteger(degree(precision), min, max);
            }
            if (precision <= MockDataUtils.LONG_PRECISION) {
                return getRandomLong(min, max, MockDataUtils.random);
            }
            return null;
        }
        else {
            if (precision != null && precision > 0) {
                final int scl = (scale != null) ? scale : 0;
                final StringBuilder sb = new StringBuilder();
                if (precision <= scl) {
                    sb.append('0');
                }
                else {
                    sb.append(randomInteger(degree(precision - scl), 0.0, null));
                }
                if (scl > 0) {
                    sb.append('.');
                    sb.append(randomInteger(degree(scl), 0.0, null));
                }
                return new BigDecimal(sb.toString());
            }
            return new BigDecimal(getRandomLong(min, max, MockDataUtils.random));
        }
    }
    
    public static int getRandomInt(final int min, final int max, @NotNull final Random random) {
        if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
            return random.nextInt();
        }
        final long dif = max - (long)min;
        final float number = random.nextFloat();
        return (int)(min + number * dif);
    }
    
    public static double getRandomDouble(final double min, final double max, @NotNull final Random random) {
        final double dif = max - min;
        final double number = random.nextDouble();
        return min + number * dif;
    }
    
    private static long getRandomLong(final Double min, final Double max, final Random random) {
        long minimum = Long.MIN_VALUE;
        if (min != null && min > minimum) {
            minimum = Math.round(min);
        }
        long maximum = Long.MAX_VALUE;
        if (max != null && max < maximum) {
            maximum = Math.round(max);
        }
        return getRandomLong(minimum, maximum, random);
    }
    
    public static long getRandomLong(final long min, final long max, @NotNull final Random random) {
        if (min == Long.MIN_VALUE && max == Long.MAX_VALUE) {
            return random.nextLong();
        }
        final double dif = max - (double)min;
        final double number = random.nextDouble();
        return Math.round(min + number * dif);
    }
    
    public static int degree(final int d) {
        Integer value = (Integer)MockDataUtils.degrees.get(d);
        if (value == null) {
            int result = 10;
            for (int i = 0; i < d - 1; ++i) {
                result *= 10;
            }
            MockDataUtils.degrees.put(d, value = result);
        }
        return value;
    }
    
    private static int randomInteger(final int bound, final Double min, Double max) {
        int minimum = Integer.MIN_VALUE;
        int maximum = Integer.MAX_VALUE;
        if (min != null && min > minimum && min < 2.147483647E9) {
            minimum = (int)Math.round(min);
        }
        if (max == null || max > bound) {
            max = (double)bound;
        }
        if (max < maximum) {
            maximum = (int)Math.round(max);
        }
        return getRandomInt(minimum, maximum, MockDataUtils.random);
    }
    
    public static UNIQ_TYPE checkUnique(final DBRProgressMonitor monitor, final DBSEntity dbsEntity, final DBSAttributeBase attribute) throws DBException {
        for (final DBSEntityConstraint constraint : CommonUtils.safeCollection(dbsEntity.getConstraints(monitor))) {
            final DBSEntityConstraintType constraintType = constraint.getConstraintType();
            if (constraintType.isUnique()) {
                final DBSEntityAttributeRef constraintAttribute = DBUtils.getConstraintAttribute(monitor, (DBSEntityReferrer)constraint, attribute.getName());
                if (constraintAttribute == null || constraintAttribute.getAttribute() != attribute) {
                    continue;
                }
                final List<? extends DBSEntityAttributeRef> refColumns = (List<? extends DBSEntityAttributeRef>)((DBSEntityReferrer)constraint).getAttributeReferences(monitor);
                if (refColumns.size() > 1) {
                    return UNIQ_TYPE.MULTI;
                }
                return UNIQ_TYPE.SINGLE;
            }
        }
        return null;
    }
    
    public enum UNIQ_TYPE
    {
        SINGLE("SINGLE", 0), 
        MULTI("MULTI", 1);
        
        private UNIQ_TYPE(final String name, final int ordinal) {
        }
    }
}
