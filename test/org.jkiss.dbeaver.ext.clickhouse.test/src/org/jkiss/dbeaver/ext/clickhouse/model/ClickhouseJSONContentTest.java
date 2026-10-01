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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.dbeaver.ext.clickhouse.model.data.ClickhouseContentJSON;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClickhouseJSONContentTest extends DBeaverUnitTest {

    @Test
    public void jsonContentUsesJsonMimeType() {
        // CONTENT data kind + text/json content type is what routes the cell to the JSON viewer/editor.
        final ClickhouseContentJSON content = new ClickhouseContentJSON(null, "{\"a\":1}");
        Assertions.assertEquals(MimeTypes.TEXT_JSON, content.getContentType());
    }

    @Test
    public void jsonContentPreservesRawText() {
        // The viewer/editor must show the canonical JSON text verbatim, not a mangled toString().
        final String json = "{\"nested\":{\"deep\":[1,2,3]},\"n\":42}";
        final ClickhouseContentJSON content = new ClickhouseContentJSON(null, json);
        Assertions.assertEquals(json, content.getDisplayString(DBDDisplayFormat.EDIT));
    }

    @Test
    public void jsonContentHandlesNull() {
        final ClickhouseContentJSON content = new ClickhouseContentJSON(null, null);
        Assertions.assertTrue(content.isNull());
    }

    @Test
    public void jsonContentPreservesMalformedText() {
        // The handler does not validate JSON; ClickHouse rejects malformed input on save (Code 117).
        // Whatever text the driver returned must be shown unchanged so the user can fix it.
        final String malformed = "{not valid json";
        final ClickhouseContentJSON content = new ClickhouseContentJSON(null, malformed);
        Assertions.assertEquals(malformed, content.getDisplayString(DBDDisplayFormat.EDIT));
    }

    @Test
    public void jsonContentHandlesEmptyString() {
        // An empty string is a value, not SQL NULL.
        final ClickhouseContentJSON content = new ClickhouseContentJSON(null, "");
        Assertions.assertFalse(content.isNull());
        Assertions.assertEquals("", content.getDisplayString(DBDDisplayFormat.EDIT));
    }
}
