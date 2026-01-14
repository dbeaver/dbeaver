/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;


@RunWith(value = Parameterized.class)
public class FunctionCountNullsTest extends DBeaverUnitTest {

    @Parameter(value = 0)
    public List<Integer> values;

    @Parameter(value = 1)
    public Long expectedCount;

    /**
     * Test data
     *
     * @return parameters for test
     */
    @Parameters(name = "{index}: Test count nulls in {0} Should be {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Arrays.asList(1, 2, 3, 4), 0L},
                {Arrays.asList(1, 2, 3, null), 1L},
                {Arrays.asList(null, null), 2L}
        });
    }

    @Test
    public void shouldGetZeroCountWhenNoNullsPresent() {
        var nullsCountFunc = new FunctionCountNulls();
        values.forEach(value -> nullsCountFunc.accumulate(value, false));
        MatcherAssert.assertThat(nullsCountFunc.getResult(0), CoreMatchers.is(expectedCount));
    }

}
