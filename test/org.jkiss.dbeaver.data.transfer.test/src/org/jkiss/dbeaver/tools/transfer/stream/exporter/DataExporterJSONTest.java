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
package org.jkiss.dbeaver.tools.transfer.stream.exporter;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public class DataExporterJSONTest {

    private static final String TABLE_NAME = "test_table";
    private static final String COLUMN_NAME = "test_column";

    @Test
    public void finiteNumbersAreWrittenAsNumbers() throws DBException, IOException {
        Assertions.assertEquals("42", exportCell(42));
        Assertions.assertEquals("1.5", exportCell(1.5d));
        Assertions.assertEquals("-0.25", exportCell(-0.25f));
        Assertions.assertEquals("1.0E308", exportCell(1.0E308d));
        // Too big for a double, but still a valid JSON number
        Assertions.assertEquals("1E+400", exportCell(new BigDecimal("1E+400")));
    }

    @Test
    public void notANumberIsWrittenAsString() throws DBException, IOException {
        Assertions.assertEquals("\"NaN\"", exportCell(Double.NaN));
        Assertions.assertEquals("\"NaN\"", exportCell(Float.NaN));
    }

    @Test
    public void infinityIsWrittenAsString() throws DBException, IOException {
        Assertions.assertEquals("\"Infinity\"", exportCell(Double.POSITIVE_INFINITY));
        Assertions.assertEquals("\"-Infinity\"", exportCell(Double.NEGATIVE_INFINITY));
        Assertions.assertEquals("\"Infinity\"", exportCell(Float.POSITIVE_INFINITY));
        Assertions.assertEquals("\"-Infinity\"", exportCell(Float.NEGATIVE_INFINITY));
    }

    @Test
    public void booleansAndStringsKeepTheirFormat() throws DBException, IOException {
        Assertions.assertEquals("true", exportCell(true));
        Assertions.assertEquals("\"NaN\"", exportCell("NaN"));
        Assertions.assertEquals("null", exportCell(null));
    }

    /**
     * Exports a single cell and returns its value exactly as it was written to the JSON output.
     */
    @NotNull
    private static String exportCell(@Nullable Object value) throws DBException, IOException {
        StringWriter writer = new StringWriter();
        DataExporterJSON exporter = new DataExporterJSON();
        exporter.init(ExporterTestsUtils.getIStreamDataExporterSiteMock(
            TABLE_NAME,
            COLUMN_NAME,
            writer,
            StandardCharsets.UTF_8.name()
        ));
        exporter.exportHeader(Mockito.mock(DBCSession.class));
        exporter.exportRow(Mockito.mock(DBCSession.class), Mockito.mock(DBCResultSet.class), new Object[]{value});
        exporter.exportFooter(null);

        String cellPrefix = "\t\t\"" + COLUMN_NAME + "\" : ";
        return writer.toString().lines()
            .filter(line -> line.startsWith(cellPrefix))
            .map(line -> line.substring(cellPrefix.length()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No cell in the output: " + writer));
    }
}
