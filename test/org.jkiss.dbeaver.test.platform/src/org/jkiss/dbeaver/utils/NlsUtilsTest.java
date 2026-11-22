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
package org.jkiss.dbeaver.utils;

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NlsUtilsTest {
    public static class Messages extends NLS {
        public static String message = "Quotes: '\"; slash: \\; dollars: $1; newline: \n; Unicode: 日本語\u2028\u2029";
        public static String nested = "%message";
        public static String html = "<&>\"'\n` ${value} \\ $1";
        public static String missing;
        public static int number;
        public String instance;
    }

    @Test
    void replacesRepeatedPlaceholdersLiterally() {
        assertEquals(Messages.message + " / " + Messages.message,
            NlsUtils.formatNlsMessages("%message / %message", Messages.class));
        assertEquals("%message", NlsUtils.formatNlsMessages("%nested", Messages.class));
        assertEquals("100% / % incomplete", NlsUtils.formatNlsMessages("100% / % incomplete", Messages.class));
    }

    @Test
    void escapesForJavaScriptStrings() {
        String result = NlsUtils.formatNlsMessages("'%#message'", Messages.class);
        String escaped = result.substring(1, result.length() - 1);
        assertFalse(escaped.contains("'"));
        assertFalse(escaped.contains("\n"));
        assertFalse(escaped.contains("\u2028"));
        assertFalse(escaped.contains("\u2029"));
        assertEquals(Messages.message, JSONUtils.GSON.fromJson('"' + escaped + '"', String.class));
    }

    @Test
    void preservesUnresolvedMessages() {
        for (String name : new String[]{"unknown", "missing", "number", "instance"}) {
            assertEquals('%' + name, NlsUtils.formatNlsMessages('%' + name, Messages.class));
            assertEquals("%!" + name, NlsUtils.formatNlsMessages("%!" + name, Messages.class));
            assertEquals("%#" + name, NlsUtils.formatNlsMessages("%#" + name, Messages.class));
            assertEquals("%!#" + name, NlsUtils.formatNlsMessages("%!#" + name, Messages.class));
        }
        assertEquals("%unknown / " + Messages.message,
            NlsUtils.formatNlsMessages("%unknown / %message", Messages.class));
    }

    @Test
    void escapesOnlyMarkedMessages() {
        assertEquals(Messages.html + " / &lt;&amp;&gt;&quot;&#39;\n` ${value} \\ $1",
            NlsUtils.formatNlsMessages("%html / %!html", Messages.class));
        String result = NlsUtils.formatNlsMessages("\"%!#html\"", Messages.class);
        assertFalse(result.contains("`"));
        assertFalse(result.contains("${"));
        assertEquals("&lt;&amp;&gt;&quot;&#39;\n` ${value} \\ $1", JSONUtils.GSON.fromJson(result, String.class));
        String plain = NlsUtils.formatNlsMessages("\"%#html\"", Messages.class);
        assertFalse(plain.contains("`"));
        assertFalse(plain.contains("${"));
        assertEquals(Messages.html, JSONUtils.GSON.fromJson(plain, String.class));
    }
}
