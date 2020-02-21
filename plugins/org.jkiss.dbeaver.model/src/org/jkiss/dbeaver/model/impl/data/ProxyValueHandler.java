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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Proxy value handler
 */
public class ProxyValueHandler implements DBDValueHandler {

    protected final DBDValueHandler target;

    public ProxyValueHandler(DBDValueHandler target) {
        this.target = target;
    }

    @NotNull
    @Override
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
        return target.getValueObjectType(attribute);
    }

    @Nullable
    @Override
    public String getValueContentType(@NotNull DBSTypedObject attribute) {
        return target.getValueContentType(attribute);
    }

    @Nullable
    @Override
    public Object fetchValueObject(@NotNull DBCSession session, @NotNull DBCResultSet resultSet, @NotNull DBSTypedObject type, int index) throws DBCException {
        return target.fetchValueObject(session, resultSet, type, index);
    }

    @Override
    public void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index, @Nullable Object value) throws DBCException {
        target.bindValueObject(session, statement, type, index, value);
    }

    @Nullable
    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) throws DBCException {
        return target.getValueFromObject(session, type, object, copy, false);
    }

    @Override
    public Object createNewValueObject(@NotNull DBCSession session, @NotNull DBSTypedObject type) throws DBCException {
        return target.createNewValueObject(session, type);
    }

    @Override
    public void releaseValueObject(@Nullable Object value) {
        target.releaseValueObject(value);
    }

    @NotNull
    @Override
    public DBCLogicalOperator[] getSupportedOperators(@NotNull DBSTypedObject attribute) {
        return target.getSupportedOperators(attribute);
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
        return target.getValueDisplayString(column, value, format);
    }
}