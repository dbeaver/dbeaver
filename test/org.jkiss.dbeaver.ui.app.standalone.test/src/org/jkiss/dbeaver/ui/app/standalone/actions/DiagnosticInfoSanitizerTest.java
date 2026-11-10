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
package org.jkiss.dbeaver.ui.app.standalone.actions;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiagnosticInfoSanitizerTest extends DBeaverUnitTest {
    @Test
    void preservesSystemPropertyNames() {
        String properties = "dbeaver.distribution.type=community\n"
            + "ds.delayed.keepInstances.default=true\r\n"
            + "  eclipse.application = org.jkiss.dbeaver.core.application\n"
            + "custom.host: db.example.com\n";
        assertEquals(
            "dbeaver.distribution.type=community\n"
                + "ds.delayed.keepInstances.default=true\r\n"
                + "  eclipse.application = [REDACTED]\n"
                + "custom.host: [REDACTED]\n",
            new DiagnosticInfoSanitizer(Map.of()).sanitize(properties)
        );
    }

    @Test
    void preservesPropertyNamesMatchingEnvironmentValues() {
        DiagnosticInfoSanitizer sanitizer = new DiagnosticInfoSanitizer(Map.of(
            "LONG_VALUE", "eclipse.application",
            "SHORT_VALUE", "ds"
        ));
        assertEquals(
            "eclipse.application=[REDACTED]\nds.delayed.keepInstances.default=[REDACTED]",
            sanitizer.sanitize("eclipse.application=eclipse.application\nds.delayed.keepInstances.default=ds")
        );
    }

    @Test
    void sanitizesSensitivePropertyValues() {
        assertEquals(
            "db.password=[REDACTED]\nuser.home=[REDACTED]\nserver.address=[REDACTED]\n"
                + "db.url=[REDACTED]\njava.version=21.0.1.2",
            new DiagnosticInfoSanitizer(Map.of()).sanitize(
                "db.password=secret\nuser.home=/home/alice\nserver.address=192.168.1.10\n"
                    + "db.url=jdbc:postgresql://db.example.com/test\njava.version=21.0.1.2"
            )
        );
    }

    @Test
    void sanitizesDomainsOutsidePropertyNames() {
        assertEquals(
            "Connecting to [REDACTED]:5432\nhost=[REDACTED]\n"
                + "!ENTRY org.jkiss.dbeaver.model 4 0 2026-11-10\n"
                + "\tat org.jkiss.dbeaver.model.Test.run(Test.java:42)",
            new DiagnosticInfoSanitizer(Map.of()).sanitize(
                "Connecting to db.example.com:5432\nhost=localhost\n"
                    + "!ENTRY org.jkiss.dbeaver.model 4 0 2026-11-10\n"
                    + "\tat org.jkiss.dbeaver.model.Test.run(Test.java:42)"
            )
        );
    }
}
