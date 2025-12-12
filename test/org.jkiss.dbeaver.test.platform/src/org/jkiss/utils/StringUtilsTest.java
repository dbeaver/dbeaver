/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.utils;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest {

    @Test
    public void testUnderscoreToCamelCaseBasicConversion() {
        Assert.assertNull(StringUtils.underscoreToCamelCase(null));
        Assert.assertEquals("", StringUtils.underscoreToCamelCase(""));
        Assert.assertEquals("someField", StringUtils.underscoreToCamelCase("some_field"));
        Assert.assertEquals("somefield", StringUtils.underscoreToCamelCase("somefield"));
        Assert.assertEquals("someText", StringUtils.underscoreToCamelCase("SOME_TEXT"));
        Assert.assertEquals("Abc", StringUtils.underscoreToCamelCase("_abc"));
        Assert.assertEquals("abc", StringUtils.underscoreToCamelCase("abc_"));
        Assert.assertEquals("aB", StringUtils.underscoreToCamelCase("a_b_"));
        Assert.assertEquals("abc123Def", StringUtils.underscoreToCamelCase("abc_123_def"));
        Assert.assertEquals("a1B2", StringUtils.underscoreToCamelCase("a1_b2"));
        Assert.assertEquals("alreadycamel", StringUtils.underscoreToCamelCase("AlreadyCamel"));
        Assert.assertEquals("aB", StringUtils.underscoreToCamelCase("A_B"));
    }
}
