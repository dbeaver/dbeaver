/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.stream.importer;

import au.com.bytecode.opencsv.CSVReader;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.tools.transfer.stream.StreamDataImporterColumnInfo;
import org.jkiss.dbeaver.tools.transfer.stream.StreamProducerSettings;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CSV importer
 */
public class DataImporterCSV extends StreamImporterAbstract {

    private static final Log log = Log.getLog(DataImporterCSV.class);

    public static final String PROP_ENCODING = "encoding";
    public static final String PROP_HEADER = "header";

    enum HeaderPosition {
        none,
        top,
        both
    }

    public DataImporterCSV() {
    }

    @Override
    public List<StreamDataImporterColumnInfo> readColumnsInfo(InputStream inputStream, StreamProducerSettings settings, Map<Object, Object> processorProperties) throws DBException {
        List<StreamDataImporterColumnInfo> columnsInfo = new ArrayList<>();

        String encoding = CommonUtils.toString(processorProperties.get(PROP_ENCODING), GeneralUtils.UTF8_ENCODING);
        String header = CommonUtils.toString(processorProperties.get(PROP_HEADER), HeaderPosition.top.name());
        HeaderPosition headerPosition = HeaderPosition.none;
        try {
            headerPosition = HeaderPosition.valueOf(header);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid header position: " + header);
        }
        try (Reader reader = new InputStreamReader(inputStream, encoding)) {
            try (CSVReader csvReader = new CSVReader(reader)) {
                for (;;) {
                    String[] line = csvReader.readNext();
                    if (line == null) {
                        break;
                    }
                    if (line.length == 0) {
                        continue;
                    }
                    for (int i = 0; i < line.length; i++) {
                        String column = line[i];
                        if (headerPosition == HeaderPosition.none) {
                            column = null;
                        }
                        columnsInfo.add(new StreamDataImporterColumnInfo(i, column));
                    }
                    break;
                }
            }
        } catch (IOException e) {
            throw new DBException("IO error reading CSV", e);
        }

        return columnsInfo;
    }
}