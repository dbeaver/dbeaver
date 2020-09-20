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
package org.jkiss.dbeaver.tools.transfer.stream;


import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

public class StreamDataImporterColumnInfo extends AbstractAttribute implements DBSEntityAttribute {

    private StreamEntityMapping entityMapping;
    private DBPDataKind dataKind;

    public StreamDataImporterColumnInfo(StreamEntityMapping entity, int columnIndex, String columnName, String typeName, int maxLength, DBPDataKind dataKind) {
        super(columnName, typeName, -1, columnIndex, maxLength, null, null, false, false);
        this.entityMapping = entity;
        this.dataKind = dataKind;
    }

    @Override
    public DBPDataKind getDataKind() {
        return dataKind;
    }

    @Nullable
    @Override
    public String getDefaultValue() {
        return null;
    }

    @NotNull
    @Override
    public StreamEntityMapping getParentObject() {
        return entityMapping;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return entityMapping.getDataSource();
    }

    public void setDataKind(DBPDataKind dataKind) {
        this.dataKind = dataKind;
    }
}
