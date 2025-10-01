/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataExporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.exporter.DataExporterGeoJSON;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.junit.osgi.annotation.RunnerProxy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

@RunnerProxy(MockitoJUnitRunner.Silent.class)
public class DataExporterGeoJSONTest extends DBeaverUnitTest {

    private DataExporterGeoJSON dataExporterGeoJSON;
    private StringWriter writer;
    private IStreamDataExporterSite site;

    @Before
    public void setUp() {
        writer = new StringWriter();
        site = Mockito.mock(IStreamDataExporterSite.class);
        Mockito.when(site.getWriter()).thenReturn(new PrintWriter(writer));
        dataExporterGeoJSON = new DataExporterGeoJSON();
        try {
            dataExporterGeoJSON.init(site);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExportHeader() {
        // Mocking attributes
        DBDAttributeBinding[] columns = new DBDAttributeBinding[2];
        columns[0] = Mockito.mock(DBDAttributeBinding.class);
        Mockito.when(columns[0].getName()).thenReturn("geometry");
        Mockito.when(columns[0].getTypeName()).thenReturn("GEOMETRY");

        columns[1] = Mockito.mock(DBDAttributeBinding.class);
        Mockito.when(columns[1].getName()).thenReturn("NAME");
        Mockito.when(columns[1].getTypeName()).thenReturn("VARCHAR");

        Mockito.when(site.getAttributes()).thenReturn(columns);
        Mockito.when(site.getProperties()).thenReturn(new HashMap<>());

        try {
            dataExporterGeoJSON.exportHeader(Mockito.mock(DBCSession.class));
            String result = writer.toString();
            String expectedHeader = "{\"type\":\"FeatureCollection\",\"features\":[";
            Assert.assertEquals(expectedHeader, result);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("Exception occurred: " + e.getMessage());
        }
    }
}
