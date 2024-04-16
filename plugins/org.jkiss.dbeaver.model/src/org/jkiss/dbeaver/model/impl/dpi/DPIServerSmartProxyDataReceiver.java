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
package org.jkiss.dbeaver.model.impl.dpi;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.dpi.DPIServerSmartObject;
import org.jkiss.dbeaver.model.dpi.DPISmartCallback;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.data.DBDValueError;

import java.util.List;

public class DPIServerSmartProxyDataReceiver implements DBDDataReceiver, DPIServerSmartObject {
    private transient DBDAttributeBinding[] bindings;
    private DBCSession session;
    private DPIResultSet dpiResultSet;
    private long offset;
    private long maxRows;

    @Override
    public void fetchStart(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, long offset, long maxRows)
        throws DBCException {
        this.session = session;
        this.offset = offset;
        this.maxRows = maxRows;
        createDPIResultSet(session, resultSet);
        DBCResultSetMetaData meta = resultSet.getMeta();
        List<? extends DBCAttributeMetaData> attributes = meta.getAttributes();

        bindings = new DBDAttributeBindingMeta[attributes.size()];
        for (int i = 0; i < attributes.size(); i++) {
            DBCAttributeMetaData attribute = attributes.get(i);
            dpiResultSet.addColumn(new DPIResultSetColumn(
                i, attribute.getLabel(), attribute
            ));
            bindings[i] = new DBDAttributeBindingMeta(
                null,
                session,
                attribute
            );

        }
    }

    private void createDPIResultSet(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) {
        dpiResultSet = new DPIResultSet(session, resultSet.getSourceStatement());
    }

    @Override
    public void fetchRow(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) throws DBCException {
        Object[] row = new Object[bindings.length];
        for (int i = 0; i < bindings.length; i++) {
            DBDAttributeBinding binding = bindings[i];
            try {
                Object cellValue = binding.getValueHandler().fetchValueObject(
                    resultSet.getSession(),
                    resultSet,
                    binding.getMetaAttribute(),
                    i);
                row[i] = cellValue;
            } catch (Throwable e) {
                row[i] = new DBDValueError(e);
            }
        }
        dpiResultSet.addRow(row);
    }

    @Override
    public void fetchEnd(@NotNull DBCSession session, @NotNull DBCResultSet resultSet) throws DBCException {

    }

    @Override
    public void close() {

    }

    public DBCSession getSession() {
        return session;
    }

    public DPIResultSet getDpiResultSet() {
        return dpiResultSet;
    }

    public long getOffset() {
        return offset;
    }

    public long getMaxRows() {
        return maxRows;
    }

    public void setSession(DBCSession session) {
        this.session = session;
    }

    public void setDpiResultSet(DPIResultSet dpiResultSet) {
        this.dpiResultSet = dpiResultSet;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public void setMaxRows(long maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public DPISmartCallback getCallback() {
        return new DPIDataReceiverCallback(
            session, dpiResultSet, offset, maxRows
        );
    }
}
