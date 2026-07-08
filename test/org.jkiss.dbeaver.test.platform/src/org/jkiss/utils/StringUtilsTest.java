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
package org.jkiss.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StringUtilsTest {

    @Test
    public void testUnderscoreToCamelCaseBasicConversion() {
        Assertions.assertNull(StringUtils.underscoreToCamelCase(null));
        Assertions.assertEquals("", StringUtils.underscoreToCamelCase(""));
        Assertions.assertEquals("someField", StringUtils.underscoreToCamelCase("some_field"));
        Assertions.assertEquals("somefield", StringUtils.underscoreToCamelCase("somefield"));
        Assertions.assertEquals("someText", StringUtils.underscoreToCamelCase("SOME_TEXT"));
        Assertions.assertEquals("Abc", StringUtils.underscoreToCamelCase("_abc"));
        Assertions.assertEquals("abc", StringUtils.underscoreToCamelCase("abc_"));
        Assertions.assertEquals("aB", StringUtils.underscoreToCamelCase("a_b_"));
        Assertions.assertEquals("abc123Def", StringUtils.underscoreToCamelCase("abc_123_def"));
        Assertions.assertEquals("a1B2", StringUtils.underscoreToCamelCase("a1_b2"));
        Assertions.assertEquals("alreadycamel", StringUtils.underscoreToCamelCase("AlreadyCamel"));
        Assertions.assertEquals("aB", StringUtils.underscoreToCamelCase("A_B"));
    }
}
