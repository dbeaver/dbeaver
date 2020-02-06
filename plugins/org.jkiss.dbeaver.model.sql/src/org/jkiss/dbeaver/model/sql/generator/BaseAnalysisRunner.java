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
package org.jkiss.dbeaver.model.sql.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class BaseAnalysisRunner<OBJECT> extends SQLGenerator<OBJECT> {

    protected abstract Collection<? extends DBSAttributeBase> getAllAttributes(DBRProgressMonitor monitor, OBJECT object) throws DBException;

    protected abstract Collection<? extends DBSAttributeBase> getKeyAttributes(DBRProgressMonitor monitor, OBJECT object) throws DBException;

    protected Collection<? extends DBSAttributeBase> getValueAttributes(DBRProgressMonitor monitor, OBJECT object, Collection<? extends DBSAttributeBase> keyAttributes) throws DBException {
        if (CommonUtils.isEmpty(keyAttributes)) {
            return getAllAttributes(monitor, object);
        }
        List<DBSAttributeBase> valueAttributes = new ArrayList<>(getAllAttributes(monitor, object));
        valueAttributes.removeIf(keyAttributes::contains);
        return valueAttributes;
    }

    protected void appendDefaultValue(StringBuilder sql, DBSAttributeBase attr) {
        String defValue = null;
        if (attr instanceof DBSEntityAttribute) {
            defValue = ((DBSEntityAttribute) attr).getDefaultValue();
        }
        if (!CommonUtils.isEmpty(defValue)) {
            sql.append(defValue);
        } else {
            switch (attr.getDataKind()) {
                case BOOLEAN:
                    sql.append("false");
                    break;
                case NUMERIC:
                    sql.append("0");
                    break;
                case STRING:
                case DATETIME:
                case CONTENT:
                    sql.append("''");
                    break;
                default:
                    sql.append("?");
                    break;
            }
        }
    }

}

