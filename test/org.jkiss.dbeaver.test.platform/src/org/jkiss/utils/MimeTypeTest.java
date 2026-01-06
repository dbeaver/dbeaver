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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MimeTypeTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void testParse() {
        Assert.assertEquals("application/*", new MimeType("application").toString());
        Assert.assertEquals("/*", new MimeType(";application").toString());
        Assert.assertEquals("application/json", new MimeType("application/json").toString());
        Assert.assertEquals("application/js", new MimeType("application/js;on").toString());

        thrown.expect(IllegalArgumentException.class);
        new MimeType("application;/json");
    }

    @Test
    public void testMatch() {
        Assert.assertTrue(new MimeType().match(new MimeType()));
        Assert.assertFalse(new MimeType().match(new MimeType("text", "json")));
        Assert.assertTrue(new MimeType("application", "json").match(new MimeType("application", "*")));
        Assert.assertFalse(new MimeType("application", "json").match(new MimeType("application", "text")));

        Assert.assertTrue(new MimeType("application", "json").match("application/json"));
    }
}
