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
package org.jkiss.dbeaver.model.sql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class RowDataReceiver implements DBDDataReceiver {
    private static final Log log = Log.getLog(RowDataReceiver.class);
    protected final DBDAttributeBinding[] curAttributes;
    protected Object[] rowValues;

    public RowDataReceiver(DBDAttributeBinding[] curAttributes) {
        this.curAttributes = curAttributes;
    }

    @Override
    public void fetchStart(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, long offset, long maxRows) {

    }

    @Override
    public void fetchRow(@NotNull DBCSession session, @NotNull DBCResultSet resultSet)
        throws DBCException {
        DBCResultSetMetaData rsMeta = resultSet.getMeta();
        // Compare attributes with existing model attributes
        List<? extends DBCAttributeMetaData> attributes = rsMeta.getAttributes();
        if (attributes.size() != curAttributes.length) {
            log.debug("Wrong meta attributes count (" + attributes.size() + " <> " + curAttributes.length + ") - can't refresh");
            return;
        }
        for (int i = 0; i < curAttributes.length; i++) {
            DBCAttributeMetaData metaAttribute = curAttributes[i].getMetaAttribute();
            if (metaAttribute == null ||
                !CommonUtils.equalObjects(metaAttribute.getName(), attributes.get(i).getName())) {
                log.debug("Attribute '" + metaAttribute + "' doesn't match '" + attributes.get(i).getName() + "'");
                return;
            }
        }

        fetchRowValues(session, resultSet);

    }

    protected void fetchRowValues(DBCSession session, DBCResultSet resultSet) throws DBCException {
        rowValues = new Object[curAttributes.length];
        for (int i = 0; i < curAttributes.length; i++) {
            final DBDAttributeBinding attr = curAttributes[i];
            DBDValueHandler valueHandler = attr.getValueHandler();
            Object attrValue = valueHandler.fetchValueObject(session, resultSet, attr, i);
            rowValues[i] = attrValue;
        }
    }

    @Override
    public void fetchEnd(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) {

    }

    @Override
    public void close() {
    }

    public Object[] getRowValues() {
        return rowValues;
    }
}
