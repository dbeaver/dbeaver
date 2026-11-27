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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.utils.csv.CSVParser;
import org.jkiss.utils.csv.CSVReader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;

public class PostgreCopyLoaderTest {
    @ParameterizedTest
    @MethodSource("stringCells")
    public void encodesQuotesAndBackslashes(@NotNull String value, @NotNull String expected) throws IOException {
        String encoded = PostgreCopyLoader.convertStringValueToCell(value);

        Assertions.assertEquals(expected, encoded);
        CSVParser parser = new CSVParser(
            ",", "\"", "\\", CSVParser.DEFAULT_STRICT_QUOTES, CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE
        );
        Assertions.assertArrayEquals(new String[] {value, "next"}, parser.parseLine(encoded + ",\"next\""));
    }

    @NotNull
    private static Stream<Arguments> stringCells() {
        return Stream.of(
            Arguments.of("", "\"\""),
            Arguments.of("one and two", "\"one and two\""),
            Arguments.of("a,b", "\"a,b\""),
            Arguments.of("a\"b", "\"a\\\"b\""),
            Arguments.of("\\", "\"\\\\\""),
            Arguments.of("value\\", "\"value\\\\\""),
            Arguments.of("a\\\\b", "\"a\\\\\\\\b\""),
            Arguments.of("value\\\"test", "\"value\\\\\\\"test\""),
            Arguments.of("\\n", "\"\\\\n\""),
            Arguments.of("a\nb", "\"a\nb\""),
            Arguments.of("a\rb", "\"a\rb\""),
            Arguments.of("a\r\nb", "\"a\r\nb\"")
        );
    }

    @Test
    public void buildsCopyColumnListInTableOrder() {
        DBSAttributeBase id = column("col_id");
        DBSAttributeBase value = column("col_value");

        String columnList = PostgreCopyLoader.buildCopyColumnList(
            List.of(value, id),
            mockDataSource()
        );

        Assertions.assertEquals("col_value, col_id", columnList);
    }

    @Test
    public void quotesMixedCaseCopyColumnNames() {
        DBSAttributeBase mixedCase = column("MixedCase");

        String columnList = PostgreCopyLoader.buildCopyColumnList(
            List.of(mixedCase),
            mockDataSource()
        );

        Assertions.assertEquals("\"MixedCase\"", columnList);
    }

    @Test
    public void returnsEmptyColumnListWhenNoColumnsAreMapped() {
        String columnList = PostgreCopyLoader.buildCopyColumnList(
            List.of(),
            mockDataSource()
        );

        Assertions.assertTrue(columnList.isEmpty());
    }

    @NotNull
    private static DBSAttributeBase column(@NotNull String name) {
        DBSAttributeBase column = Mockito.mock(DBSAttributeBase.class);
        Mockito.when(column.getName()).thenReturn(name);
        return column;
    }

    @NotNull
    private static DBPDataSource mockDataSource() {
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        return dataSource;
    }

    @Test
    public void preservesRowsAfterParsingWithCustomEscapeCharacter() throws IOException {
        String input = """
            "id","text_value"
            4,"value\\""test"
            5,"one and two"
            """;
        StringBuilder copyData = new StringBuilder();
        try (CSVReader reader = new CSVReader(new StringReader(input), ",", "\"", "~")) {
            Assertions.assertArrayEquals(new String[] {"id", "text_value"}, reader.readNext());
            String[] row;
            while ((row = reader.readNext()) != null) {
                copyData.append(row[0]).append(',')
                    .append(PostgreCopyLoader.convertStringValueToCell(row[1])).append('\n');
            }
        }

        try (CSVReader reader = new CSVReader(new StringReader(copyData.toString()), ",", "\"", "\\")) {
            Assertions.assertArrayEquals(new String[] {"4", "value\\\"test"}, reader.readNext());
            Assertions.assertArrayEquals(new String[] {"5", "one and two"}, reader.readNext());
            Assertions.assertNull(reader.readNext());
            Assertions.assertFalse(reader.getParser().isPending());
        }
    }
}
