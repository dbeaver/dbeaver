/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.EnumValueManager;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * MySQL ENUM value manager
 */
public class MySQLEnumValueManager extends EnumValueManager {
    public MySQLEnumValueManager() {
    }

    @Override
    protected boolean isMultiValue(IValueController valueController) {
        return valueController.getValueType().getTypeName().equalsIgnoreCase(MySQLConstants.TYPE_NAME_SET);
    }

    @Override
    protected List<String> getEnumValues(IValueController valueController) {
        DBSTypedObject valueType = valueController.getValueType();
        if (valueType instanceof MySQLTableColumn) {
            return ((MySQLTableColumn) valueType).getEnumValues();
        } else {
            return null;
        }
    }

    @Override
    protected List<String> getSetValues(IValueController valueController, Object value) {
        String setString = DBValueFormatting.getDefaultValueDisplayString(value, DBDDisplayFormat.UI);
        List<String> setValues = new ArrayList<String>();
        if (!CommonUtils.isEmpty(setString)) {
            StringTokenizer st = new StringTokenizer(setString, ",");
            while (st.hasMoreTokens()) {
                setValues.add(st.nextToken());
            }
        }
        return setValues;
    }

}