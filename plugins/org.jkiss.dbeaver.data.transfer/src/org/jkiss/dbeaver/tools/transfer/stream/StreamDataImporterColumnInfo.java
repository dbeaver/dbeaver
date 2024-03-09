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
package org.jkiss.dbeaver.tools.transfer.stream;


import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.internal.DTActivator;

public class StreamDataImporterColumnInfo extends AbstractAttribute implements DBSEntityAttribute {

    private StreamEntityMapping entityMapping;
    private DBPDataKind dataKind;

    // Determines whether the mapping metadata,
    // such as the column name, is present or not.
    private boolean mappingMetadataPresent;

    public StreamDataImporterColumnInfo(StreamEntityMapping entity, int columnIndex, String columnName, String typeName, int maxLength, DBPDataKind dataKind) {
        super(columnName, typeName, -1, columnIndex, maxLength, null, null, false, false);
        this.entityMapping = entity;
        this.dataKind = dataKind;
    }

    /**
     *
     * Updates current data type max length if needed
     *
     * @param dataSource to search data source specific options
     * @param maxLengthFromData the required length for correct data storing from foreign sources for this data type
     */
    public void updateMaxLength(@Nullable DBPDataSource dataSource, long maxLengthFromData) {
        long maxLength = getMaxLength();
        DBPPreferenceStore globalPreferenceStore = DTActivator.getDefault().getPreferences();
        if (dataSource != null) {
            // First check data source settings for max data type length
            DBPPreferenceStore dataSourcePreferenceStore = dataSource.getContainer().getPreferenceStore();
            int maxTypeLengthFromPref = dataSourcePreferenceStore.getInt(DTConstants.PREF_MAX_TYPE_LENGTH);
            if (dataSourcePreferenceStore.contains(DTConstants.PREF_MAX_TYPE_LENGTH) && maxLength > maxTypeLengthFromPref) {
                setMaxLength(maxTypeLengthFromPref);
                return;
            }
        }
        if (globalPreferenceStore.contains(DTConstants.PREF_MAX_TYPE_LENGTH) &&
            maxLength > globalPreferenceStore.getInt(DTConstants.PREF_MAX_TYPE_LENGTH)
        ) {
            // Also change if global settings have max data type value
            setMaxLength(globalPreferenceStore.getInt(DTConstants.PREF_MAX_TYPE_LENGTH));
        } else if (maxLength < maxLengthFromData) {
            setMaxLength(roundToNextPowerOf2(maxLengthFromData));
        }
    }

    public void updateType(@NotNull DBPDataKind kind, @NotNull String name) {
        if (getDataKind().getCommonality() < kind.getCommonality()) {
            setDataKind(kind);
            setTypeName(name);
        }
    }

    @NotNull
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

    @Override
    public void setTypeName(@NotNull String typeName) {
        this.typeName = typeName;
    }

    public boolean isMappingMetadataPresent() {
        return mappingMetadataPresent;
    }

    public void setMappingMetadataPresent(boolean mappingMetadataPresent) {
        this.mappingMetadataPresent = mappingMetadataPresent;
    }

    private static long roundToNextPowerOf2(long value) {
        int result = 1;
        while (result < value) {
            result <<= 1;
        }
        return result;
    }
}
