/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.data.aggregate;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

public class AggregateFunctionsTest extends DBeaverUnitTest {

    @Test
    public void testSumPreservesBigIntegerPrecision() {
        FunctionSum sum = new FunctionSum();
        BigInteger value = new BigInteger("18446744073709551615");

        sum.accumulate(value, false);

        Assertions.assertEquals(new BigDecimal("18446744073709551615"), sum.getResult(1));
    }

    @Test
    public void testSumPreservesBigDecimalPrecision() {
        FunctionSum sum = new FunctionSum();

        sum.accumulate(new BigDecimal("12345678901234567890.12345"), false);
        sum.accumulate(new BigDecimal("0.00005"), false);

        Assertions.assertEquals(new BigDecimal("12345678901234567890.12350"), sum.getResult(2));
    }

    @Test
    public void testAveragePreservesBigIntegerPrecision() {
        FunctionAvg average = new FunctionAvg();
        BigInteger value = new BigInteger("18446744073709551615");

        average.accumulate(value, false);

        Assertions.assertEquals(new BigDecimal("18446744073709551615"), average.getResult(1));
    }

    @Test
    public void testAverageWithBigDecimalValues() {
        FunctionAvg average = new FunctionAvg();

        average.accumulate(new BigDecimal("10.5"), false);
        average.accumulate(new BigDecimal("20.5"), false);

        Assertions.assertEquals(new BigDecimal("15.5"), average.getResult(2));
    }

    @Test
    public void testEmptySumReturnsNull() {
        FunctionSum sum = new FunctionSum();

        Assertions.assertNull(sum.getResult(0));
    }

    @Test
    public void testEmptyAverageReturnsNull() {
        FunctionAvg average = new FunctionAvg();

        Assertions.assertNull(average.getResult(0));
    }

    @Test
    public void testSumAcceptsNaNString() {
        FunctionSum sum = new FunctionSum();

        Assertions.assertTrue(sum.accumulate("NaN", false));

        Assertions.assertNull(sum.getResult(1));
    }

    @Test
    public void testSumAcceptsInfinityString() {
        FunctionSum sum = new FunctionSum();

        Assertions.assertTrue(sum.accumulate("Infinity", false));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, sum.getResult(1));
    }

    @Test
    public void testAverageAcceptsNaNString() {
        FunctionAvg average = new FunctionAvg();

        Assertions.assertTrue(average.accumulate("NaN", false));

        Assertions.assertNull(average.getResult(1));
    }

    @Test
    public void testAverageAcceptsInfinityString() {
        FunctionAvg average = new FunctionAvg();

        Assertions.assertTrue(average.accumulate("Infinity", false));

        Assertions.assertEquals(Double.POSITIVE_INFINITY, average.getResult(1));
    }

    @Test
    public void testSumWithMixedNumericTypes() {
        FunctionSum sum = new FunctionSum();

        sum.accumulate(10, false);
        sum.accumulate(new BigDecimal("20.50"), false);
        sum.accumulate(0.25, false);

        Assertions.assertEquals(
            new BigDecimal("30.75"),
            sum.getResult(3)
        );
    }

    @Test
    public void testAverageWithMixedNumericTypes() {
        FunctionAvg average = new FunctionAvg();

        average.accumulate(10, false);
        average.accumulate(new BigDecimal("20.50"), false);
        average.accumulate(0.25, false);

        Assertions.assertEquals(
            new BigDecimal("10.25"),
            average.getResult(3)
        );
    }

    @Test
    public void testAverageWithNonTerminatingDecimal() {
        FunctionAvg average = new FunctionAvg();

        average.accumulate(new BigDecimal("1"), false);
        average.accumulate(new BigDecimal("0"), false);
        average.accumulate(new BigDecimal("0"), false);

        Assertions.assertEquals(
            0,
            ((BigDecimal) average.getResult(3))
                .compareTo(new BigDecimal("0.3333333333333333333333333333333333"))
        );
    }

    @Test
    public void testSumPreservesPrecisionAcrossMultipleBigIntegers() {
        FunctionSum sum = new FunctionSum();

        sum.accumulate(new BigInteger("18446744073709551615"), false);
        sum.accumulate(new BigInteger("18446744073709551615"), false);

        Assertions.assertEquals(
            new BigDecimal("36893488147419103230"),
            sum.getResult(2)
        );
    }

    @Test
    public void testSumPreservesPrecisionWithNegativeBigInteger() {
        FunctionSum sum = new FunctionSum();

        sum.accumulate(new BigInteger("18446744073709551615"), false);
        sum.accumulate(new BigInteger("-18446744073709551614"), false);

        Assertions.assertEquals(
            BigDecimal.ONE,
            sum.getResult(2)
        );
    }

    @Test
    public void testSumWithLargeBigDecimalAndNegativeInfinity() {
        FunctionSum sum = new FunctionSum();

        sum.accumulate(new BigDecimal("1E+400"), false);
        sum.accumulate("-Infinity", false);

        Assertions.assertEquals(Double.NEGATIVE_INFINITY, sum.getResult(2));
    }
}
