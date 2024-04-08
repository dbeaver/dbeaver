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
package org.jkiss.dbeaver.test.platform.utils;

import org.jkiss.dbeaver.utils.GeneralUtils;
import org.junit.Assert;
import org.junit.Test;

public class GeneralUtilsTest {
    private static final String VALID_INPUT_STRING = "test_key_word"; //$NON-NLS-1$
    private static final String NON_VALID_INPUT_STRING_SPACE_BEGINAFTER = " test_key_word "; //$NON-NLS-1$

    private static final int[] VALID_STRING_ARRAY = { 116, 101, 115, 116, 95, 107, 101, 121, 95, 119, 111, 114, 100 };
    private static final int[] NON_VALID_STRING_ARRAY = { 160, 116, 101, 115, 160, 116, 95, 107, 101, 121, 95, 119, 111, 114, 100, 160 };

    @Test
    public void removeInvalidUnicodeSymbolFromStringPositiveTest() {
        String processedStr = GeneralUtils.removeInValidUnicodeSymbol(NON_VALID_INPUT_STRING_SPACE_BEGINAFTER);
        Assert.assertEquals(VALID_INPUT_STRING, processedStr);
    }

    @Test
    public void removeInvalidUnicodeSymbolFromCodeArrayPositiveTest() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < NON_VALID_STRING_ARRAY.length; i++) {
            builder.append(Character.toString(NON_VALID_STRING_ARRAY[i]));
        }
        String str = GeneralUtils.removeInValidUnicodeSymbol(builder.toString());
        Assert.assertEquals(VALID_INPUT_STRING, str);
        Assert.assertEquals(VALID_STRING_ARRAY, str.getBytes());
    }

}
