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
package org.jkiss.dbeaver.ext.polardbx.mysql.model;

import org.jkiss.dbeaver.ext.mysql.model.MySQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.util.Locale;

public class PolarDBXFunctionDefinitionParserTest extends DBeaverUnitTest {
    @Test
    @ResourceLock(Resources.LOCALE)
    public void parsesLowercaseTypesUnderTurkishLocale() {
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            var parameters = FunctionDefinitionParser.parseFunctionDefinition(
                new MySQLDialect(),
                "create function f(p tinyint, q decimal(10)) returns tinyint begin return p; end"
            );
            Assertions.assertEquals(3, parameters.size());
            Assertions.assertEquals("TINYINT", parameters.get("p").getDataType());
            Assertions.assertEquals(3, parameters.get("p").getLength());
            Assertions.assertEquals("DECIMAL", parameters.get("q").getDataType());
            Assertions.assertEquals(10, parameters.get("q").getLength());
            Assertions.assertEquals("TINYINT", parameters.get("return").getDataType());
            Assertions.assertEquals(3, parameters.get("return").getLength());
        } finally {
            Locale.setDefault(previousLocale);
        }
    }
}
