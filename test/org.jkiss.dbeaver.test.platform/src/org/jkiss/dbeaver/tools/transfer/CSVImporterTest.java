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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataImporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.StreamDataImporterColumnInfo;
import org.jkiss.dbeaver.tools.transfer.stream.StreamEntityMapping;
import org.jkiss.dbeaver.tools.transfer.stream.importer.DataImporterCSV;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVImporterTest  extends DBeaverUnitTest {

    private static final Path DUMMY_FILE = Path.of("dummy");
    private final DataImporterCSV importer = new DataImporterCSV();
    private final StreamEntityMapping mapping = new StreamEntityMapping(DUMMY_FILE);
    private final Map<String, Object> properties = new HashMap<>();

    @Mock
    private IStreamDataImporterSite site;

    @Before
    public void init() throws DBException {
        importer.init(site);
        Mockito.when(site.getProcessorProperties()).thenReturn(properties);
    }

    @Test
    public void generateColumnNames() throws DBException, IOException {
        List<StreamDataImporterColumnInfo> columnsInfo = readColumnsInfo("a,b,c,d", false);
        Assert.assertEquals(4, columnsInfo.size());
        Assert.assertEquals("Column1", columnsInfo.get(0).getName());
        Assert.assertEquals("Column2", columnsInfo.get(1).getName());
        Assert.assertEquals("Column3", columnsInfo.get(2).getName());
        Assert.assertEquals("Column4", columnsInfo.get(3).getName());
    }

    @Test
    public void readColumnNames() throws DBException, IOException {
        List<StreamDataImporterColumnInfo> columnsInfo = readColumnsInfo("a,b,c,d", true);
        Assert.assertEquals(4, columnsInfo.size());
        Assert.assertEquals("a", columnsInfo.get(0).getName());
        Assert.assertEquals("b", columnsInfo.get(1).getName());
        Assert.assertEquals("c", columnsInfo.get(2).getName());
        Assert.assertEquals("d", columnsInfo.get(3).getName());
    }

    @Test
    public void guessColumnTypes() throws DBException, IOException {
        List<StreamDataImporterColumnInfo> columnsInfo = readColumnsInfo("1,2.0,abc,false", false);
        Assert.assertEquals(4, columnsInfo.size());
        Assert.assertEquals(DBPDataKind.NUMERIC, columnsInfo.get(0).getDataKind());
        Assert.assertEquals("INTEGER", columnsInfo.get(0).getTypeName());
        Assert.assertEquals(DBPDataKind.NUMERIC, columnsInfo.get(1).getDataKind());
        Assert.assertEquals("REAL", columnsInfo.get(1).getTypeName());
        Assert.assertEquals(DBPDataKind.STRING, columnsInfo.get(2).getDataKind());
        Assert.assertEquals(DBPDataKind.BOOLEAN, columnsInfo.get(3).getDataKind());
    }
  
    @Test
    public void guessColumnTypesWithLongData() throws DBException, IOException {
    	List<StreamDataImporterColumnInfo> columnsInfo = readColumnsInfo("2147483648,-9223372036854775808,1", false);
    	Assert.assertEquals(3,  columnsInfo.size());
    	Assert.assertEquals(DBPDataKind.NUMERIC, columnsInfo.get(0).getDataKind());
    	Assert.assertEquals("BIGINT", columnsInfo.get(0).getTypeName());
    	Assert.assertEquals(DBPDataKind.NUMERIC, columnsInfo.get(1).getDataKind());
    	Assert.assertEquals("BIGINT", columnsInfo.get(1).getTypeName());
        Assert.assertEquals(DBPDataKind.NUMERIC, columnsInfo.get(2).getDataKind());
        Assert.assertEquals("INTEGER", columnsInfo.get(2).getTypeName());
    }
    
    @Test
    public void returnsEmptyListWithEmptyFile() throws DBException, IOException {
    	List<StreamDataImporterColumnInfo> columnsInfo = readColumnsInfo("", false);
    	Assert.assertEquals(0,  columnsInfo.size());
    }
    

    @Test
    public void guessColumnTypesOverSamples() throws DBException, IOException {
        List<StreamDataImporterColumnInfo> columnsInfo = readColumnsInfo("1\n\n2\n3\ntest", false);
        Assert.assertEquals(1, columnsInfo.size());
        Assert.assertEquals(DBPDataKind.STRING, columnsInfo.get(0).getDataKind());
    }

    @Test
    public void guessColumnTypesDefault() throws DBException, IOException {
        List<StreamDataImporterColumnInfo> columnsInfo = readColumnsInfo(",", false);
        Assert.assertEquals(2, columnsInfo.size());
        Assert.assertEquals(DBPDataKind.STRING, columnsInfo.get(0).getDataKind());
        Assert.assertEquals(DBPDataKind.STRING, columnsInfo.get(1).getDataKind());
    }

    private List<StreamDataImporterColumnInfo> readColumnsInfo(String data, boolean isHeaderPresent) throws DBException, IOException {
        properties.put("header", isHeaderPresent ? DataImporterCSV.HeaderPosition.top : DataImporterCSV.HeaderPosition.none);
        try (ByteArrayInputStream is = new ByteArrayInputStream(data.getBytes())) {
            return importer.readColumnsInfo(mapping, is);
        }
    }
}
