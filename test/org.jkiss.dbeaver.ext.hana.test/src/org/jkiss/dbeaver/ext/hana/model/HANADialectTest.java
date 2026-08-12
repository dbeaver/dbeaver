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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HANADialectTest extends DBeaverUnitTest {
    @Test
    public void quoteStatusStringTest() {
        final HANASQLDialect dialect = new HANASQLDialect();

        Assertions.assertFalse(dialect.mustBeQuoted("test", false));
        Assertions.assertFalse(dialect.mustBeQuoted("testNAME", false));
        Assertions.assertFalse(dialect.mustBeQuoted("Test", false));
        Assertions.assertFalse(dialect.mustBeQuoted("TEST", false));
        Assertions.assertTrue(dialect.mustBeQuoted("тест", false));
        Assertions.assertTrue(dialect.mustBeQuoted("  test  ", false));
        Assertions.assertTrue(dialect.mustBeQuoted("њен", false));
        Assertions.assertTrue(dialect.mustBeQuoted("भिडियोहरू", false));
        Assertions.assertTrue(dialect.mustBeQuoted("名错误", false));
        Assertions.assertTrue(dialect.mustBeQuoted("თახვი", false));
        Assertions.assertTrue(dialect.mustBeQuoted("+%$*@#", false));
    }
}
