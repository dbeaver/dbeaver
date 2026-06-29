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
package org.jkiss.dbeaver.tools.transfer;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.exporter.DataExporterCSV;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.ArrayUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataExporterCSVTest extends DBeaverUnitTest {

    private DataExporterCSV dataExporterCSV;
    private StringWriter stringWriter;
    private IStreamDataExporterSite site;

    private Map<String, Object> properties;

    private DBDAttributeBinding[] columns;

    @BeforeEach
    public void setUp() {
        properties = new HashMap<>();
        stringWriter = new StringWriter();
        columns = new DBDAttributeBinding[]{};
        PrintWriter printWriter = new PrintWriter(stringWriter);

        site = mock(IStreamDataExporterSite.class);
        when(site.getWriter()).thenReturn(printWriter);
        when(site.getProperties()).thenReturn(properties);
        when(site.getAttributes()).thenReturn(columns);
    }

    @Test
    public void testExportHeader() throws DBException, IOException {
        initExporter();
        // given
        addColumn("ID", "Identifier");
        addColumn("NAME", "Name");
        addColumn("AGE", "Age");
        // when
        dataExporterCSV.exportHeader(mock(DBCSession.class));
        // then
        String expectedHeader = "\"IDENTIFIER\",\"NAME\",\"AGE\",";
        Assertions.assertEquals(expectedHeader, stringWriter.toString());
    }

    @NotNull
    private DBDAttributeBinding addColumn(@NotNull String name, @NotNull String label) {
        DBDAttributeBinding dbdAttributeBinding = mock(DBDAttributeBinding.class);
        when(dbdAttributeBinding.getName()).thenReturn(name);
        when(dbdAttributeBinding.getLabel()).thenReturn(label);
        columns = ArrayUtils.add(DBDAttributeBinding.class, columns, dbdAttributeBinding);
        when(site.getAttributes()).thenReturn(columns);
        return dbdAttributeBinding;
    }


    private void initExporter() throws DBException {
        dataExporterCSV = new DataExporterCSV();
        dataExporterCSV.init(site);
    }

    private void setProperty(@NotNull String key, @NotNull Object value) {
        properties.put(key, value);
    }
}
