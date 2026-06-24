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

public class MimeTypeTest {

    @Test
    public void testParse() {
        Assertions.assertEquals("application/*", new MimeType("application").toString());
        Assertions.assertEquals("/*", new MimeType(";application").toString());
        Assertions.assertEquals("application/json", new MimeType("application/json").toString());
        Assertions.assertEquals("application/js", new MimeType("application/js;on").toString());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new MimeType("application;/json"));
    }

    @Test
    public void testMatch() {
        Assertions.assertTrue(new MimeType().match(new MimeType()));
        Assertions.assertFalse(new MimeType().match(new MimeType("text", "json")));
        Assertions.assertTrue(new MimeType("application", "json").match(new MimeType("application", "*")));
        Assertions.assertFalse(new MimeType("application", "json").match(new MimeType("application", "text")));

        Assertions.assertTrue(new MimeType("application", "json").match("application/json"));
    }
}
