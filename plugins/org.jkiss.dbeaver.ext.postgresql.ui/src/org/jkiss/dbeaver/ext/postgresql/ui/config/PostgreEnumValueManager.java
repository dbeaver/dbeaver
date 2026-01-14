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
package org.jkiss.dbeaver.ext.postgresql.ui.config;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectEx;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.EnumValueManager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL ENUM value manager
 */
public class PostgreEnumValueManager extends EnumValueManager {

    private static final Log log = Log.getLog(PostgreEnumValueManager.class);

    @Override
    protected boolean isMultiValue(IValueController valueController) {
        return false;
    }

    @Override
    protected List<String> getEnumValues(IValueController valueController) {
        final DBSTypedObject attribute = valueController.getValueType();
        PostgreDataType dataType = null;
        if (attribute instanceof DBSDataType) {
            dataType = (PostgreDataType) attribute;
        } else if (attribute instanceof DBSTypedObjectEx) {
            dataType = (PostgreDataType) ((DBSTypedObjectEx) attribute).getDataType();
        }
        if (dataType == null) {
            return null;
        }
        PostgreDataType finalDataType = dataType;
        final Object[][] values = new Object[1][1];
        try {
            UIUtils.runInProgressService(monitor -> values[0] = finalDataType.getEnumValues(monitor));
        } catch (InvocationTargetException | InterruptedException e) {
            log.debug("Can't read enum values from " + dataType.getFullTypeName());
        }
        if (values[0] == null) {
            return null;
        }
        List<String> strValues = new ArrayList<>(values[0].length);
        for (Object value : values[0]) {
            strValues.add(DBValueFormatting.getDefaultValueDisplayString(value, DBDDisplayFormat.UI));
        }
        return strValues;
    }

    @Override
    protected List<String> getSetValues(IValueController valueController, Object value) {
        return null;
    }

}