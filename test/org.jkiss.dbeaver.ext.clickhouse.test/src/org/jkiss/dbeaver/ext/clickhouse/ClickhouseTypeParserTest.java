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
package org.jkiss.dbeaver.ext.clickhouse;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class ClickhouseTypeParserTest extends DBeaverUnitTest {

    @Test
    void resolvesBaseTypeName() {
        Map.of(
            "UInt32", "UInt32",
            "Decimal(10, 2)", "Decimal",
            "Nullable(UInt32)", "UInt32",
            "Nullable(UInt64)", "UInt64",
            "LowCardinality(String)", "String",
            "Nullable(LowCardinality(String))", "String",
            "Nullable(DateTime64(3, 'UTC'))", "DateTime64",
            "Nullable(Array(String))", "Array"
        ).forEach((fullTypeName, expectedTypeName) -> Assertions.assertEquals(
            expectedTypeName,
            ClickhouseTypeParser.getTypeNameWithoutModifiers(fullTypeName)
        ));
    }

    @Test
    void recognizesMarkerTypesAsComplex() {
        Assertions.assertTrue(ClickhouseTypeParser.isComplexType("Nullable(UInt32)"));
        Assertions.assertTrue(ClickhouseTypeParser.isComplexType("LowCardinality(String)"));
        Assertions.assertFalse(ClickhouseTypeParser.isComplexType("UInt32"));
    }
}
