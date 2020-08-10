/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataImporter;
import org.jkiss.dbeaver.tools.transfer.stream.IStreamDataImporterSite;
import org.jkiss.dbeaver.tools.transfer.stream.StreamDataImporterColumnInfo;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferResultSet;
import org.jkiss.utils.CommonUtils;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Abstract stream importer
 */
public abstract class StreamImporterAbstract implements IStreamDataImporter {

    private static final Log log = Log.getLog(StreamImporterAbstract.class);

    private IStreamDataImporterSite site;

    public IStreamDataImporterSite getSite()
    {
        return site;
    }

    @Override
    public void init(@NotNull IStreamDataImporterSite site) throws DBException
    {
        this.site = site;
    }

    @Override
    public void dispose()
    {
        // do nothing
    }

    @Nullable
    protected DateTimeFormatter getTimeStampFormat(Map<String, Object> properties, String propName) {
        DateTimeFormatter tsFormat = null;

        String tsFormatPattern = CommonUtils.toString(properties.get(propName));
        if (!CommonUtils.isEmpty(tsFormatPattern)) {
            try {
                tsFormat = DateTimeFormatter.ofPattern(tsFormatPattern);
            } catch (Exception e) {
                log.error("Wrong timestamp format: " + tsFormatPattern, e);
            }
        }
        return tsFormat;
    }

    protected void applyTransformHints(StreamTransferResultSet resultSet, IDataTransferConsumer consumer, DateTimeFormatter tsFormat) throws DBException {
        if (tsFormat != null) {
            resultSet.setDateTimeFormat(tsFormat);
        }

        // Try to find source/target attributes
        // Modify source data type and data kind for timestamps and numerics
        // Do it only for valid String mappings
        if (consumer instanceof DatabaseTransferConsumer) {
            for (DatabaseTransferConsumer.ColumnMapping cm : ((DatabaseTransferConsumer) consumer).getColumnMappings()) {
                for (StreamDataImporterColumnInfo attributeMapping : resultSet.getAttributeMappings()) {
                    if (cm.targetAttr.getMappingType().isValid()) {
                        if (cm.sourceAttr.getDataKind() == DBPDataKind.STRING && cm.sourceAttr.getName().equals(attributeMapping.getName())) {
                            // Gotcha
                            DBSEntityAttribute targetAttr = cm.targetAttr.getTarget();
                            if (targetAttr != null) {
                                switch (targetAttr.getDataKind()) {
                                    case DATETIME:
                                    case NUMERIC:
                                    case BOOLEAN:
                                        attributeMapping.setDataKind(targetAttr.getDataKind());
                                        break;
                                }
                            }
                        }
                    }
                }
            }
                 Object targetObject = consumer.getTargetObject();
            if (targetObject instanceof DBSEntity) {

            }
        }
    }

}