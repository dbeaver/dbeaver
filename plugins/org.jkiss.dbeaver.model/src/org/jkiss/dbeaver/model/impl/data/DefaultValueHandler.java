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
package org.jkiss.dbeaver.model.impl.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

/**
 * Default value handler
 */
public class DefaultValueHandler extends BaseValueHandler {

    public static final DefaultValueHandler INSTANCE = new DefaultValueHandler();

    @NotNull
    @Override
    public Class<Object> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return Object.class;
    }

    @Override
    public Object fetchValueObject(
        @NotNull DBCSession session,
        @NotNull DBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index) throws DBCException
    {
        return resultSet.getAttributeValue(index);
    }

    @Override
    public void bindValueObject(
        @NotNull DBCSession session,
        @NotNull DBCStatement statement,
        @NotNull DBSTypedObject type,
        int index,
        Object value) throws DBCException
    {
        throw new DBCException("Object parameter [" + DBValueFormatting.getDefaultValueDisplayString(value, DBDDisplayFormat.UI) + "] binding not supported");
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
    {
        return object;
    }

}
