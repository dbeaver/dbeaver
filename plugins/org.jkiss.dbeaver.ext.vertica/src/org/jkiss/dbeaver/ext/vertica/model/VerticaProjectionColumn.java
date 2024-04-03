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
package org.jkiss.dbeaver.ext.vertica.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.vertica.VerticaUtils;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;

/**
 * VerticaProjectionColumn
 */
public class VerticaProjectionColumn extends JDBCTableColumn<VerticaProjection>
{
    private static final Log log = Log.getLog(VerticaProjectionColumn.class);
    private String description;

    protected VerticaProjectionColumn(VerticaProjection table, JDBCResultSet dbResult) {
        super(table, true);

        setName(JDBCUtils.safeGetString(dbResult, "projection_column_name"));
        setOrdinalPosition(JDBCUtils.safeGetInt(dbResult, "column_position"));
        this.typeName = JDBCUtils.safeGetString(dbResult, "data_type");
        this.description = JDBCUtils.safeGetString(dbResult, "comment");
        this.valueType = VerticaUtils.resolveValueType(this.typeName);

        {
            setMaxLength(0);
            int divPos = typeName.indexOf('(');
            if (divPos != -1) {
                int divPos2 = typeName.indexOf(')', divPos);
                if (divPos2 != -1) {
                    String length = typeName.substring(divPos + 1, divPos2);
                    boolean numericType = false;
                    String scale = null;
                    if (length.contains(",")) { // floats, numbers etc.
                        String[] numbers = length.split(",");
                        if (numbers.length == 2) {
                            numericType = true;
                            length = numbers[0];
                            scale = numbers[1];
                        }
                    }
                    try {
                        setMaxLength(Integer.parseInt(length));
                        if (numericType) {
                            setScale(Integer.parseInt(scale));
                        }
                    } catch (NumberFormatException e) {
                        log.warn(e);
                    }
                }
                typeName = typeName.substring(0, divPos);
            }
        }

        setRequired(false);
    }

    @Override
    public DBPDataSource getDataSource() {
        return getTable().getDataSource();
    }

    @NotNull
    @Override
    public DBPDataKind getDataKind() {
        return super.getDataKind();
    }

    @Override
    public boolean isRequired() {
        return super.isRequired();
    }

    @Override
    public boolean isAutoGenerated() {
        return super.isAutoGenerated();
    }

    @Override
    public String getDefaultValue() {
        return super.getDefaultValue();
    }

    @Nullable
    @Override
    public Integer getPrecision() {
        return super.getPrecision();
    }

    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 100)
    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
