/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreAttribute;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.EnumValueManager;

import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL ENUM value manager
 */
public class PostgreEnumValueManager extends EnumValueManager {
    @Override
    protected boolean isMultiValue(IValueController valueController) {
        return false;
    }

    @Override
    protected List<String> getEnumValues(IValueController valueController) {
        final PostgreAttribute attribute = (PostgreAttribute) valueController.getValueType();
        if (attribute.getDataType() == null) {
            return null;
        }
        final Object[] values = attribute.getDataType().getEnumValues();
        if (values == null) {
            return null;
        }
        List<String> strValues = new ArrayList<>(values.length);
        for (Object value : values) {
            strValues.add(DBValueFormatting.getDefaultValueDisplayString(value, DBDDisplayFormat.UI));
        }
        return strValues;
    }

    @Override
    protected List<String> getSetValues(IValueController valueController, Object value) {
        return null;
    }

}