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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClickhouseTypeParserTest extends DBeaverUnitTest {

    @Test
    void unwrapsMarkerTypes() {
        Assertions.assertEquals(
            "String",
            ClickhouseTypeParser.getTypeNameWithoutModifiers("Nullable(LowCardinality(String))")
        );
        Assertions.assertEquals(
            "DateTime64",
            ClickhouseTypeParser.getTypeNameWithoutModifiers("Nullable(DateTime64(3, 'UTC'))")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "JSON", "json", "JSON()", "JSON(a UInt64)",
        "JSON(a UInt64, items Array(Nullable(UInt64)))",
        "JSON(max_dynamic_types=1, max_dynamic_paths=0)",
        "JSON(SKIP a.b)", "JSON(SKIP REGEXP '^(foo|bar)$')",
        "JSON(SKIP REGEXP '[)]')", "JSON(SKIP REGEXP 'a\\'b')",
        "JSON(`a)b` UInt64)", "JSON(`a``)b` UInt64)", "JSON(\"a)b\" UInt64)",
        "JSON(a Tuple(x UInt64, y String))", "JSON(a Enum8('x(y)'=1))",
        "JSON(score BFloat16)", "JSON(payload Dynamic)", "JSON(a Nullable(BFloat16))",
        "JSON(a Array(Dynamic))", "JSON(a Tuple(x BFloat16, y String))",
        "JSON(a Variant(UInt64, String))", "JSON(a QBit(Float32, 8))",
        "Nullable(JSON)", "Nullable(JSON())", "Nullable(JSON(a UInt64, items Array(Nullable(UInt64))))",
        "Nullable(JSON(a UInt64)", "Nullable(JSON(a Array(Nullable(UInt64)))"
        // JDBC metadata can omit the outer Nullable close.
    })
    void recognizesScalarJsonTypes(String typeName) {
        Assertions.assertTrue(ClickhouseTypeParser.isScalarJsonType(typeName), typeName);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "JSONB", "JSONX(a UInt64)", "JSON(a UInt64)Extra", "JSON(a UInt64))",
        "JSON(a UInt64", "JSON(a Array(UInt64)", "JSON(SKIP REGEXP 'unterminated)",
        "Nullable(String)", "Nullable(JSON))", "Nullable(JSON(a UInt64)))",
        "Array(JSON)", "Array(Nullable(JSON))", "Map(String, JSON)",
        "LowCardinality(JSON)", "Nullable(Nullable(JSON))"
    })
    void excludesOtherAndMalformedTypes(String typeName) {
        Assertions.assertFalse(ClickhouseTypeParser.isScalarJsonType(typeName), typeName);
    }
}
