/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model.data.type;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreOid;
import org.jkiss.utils.CommonUtils;

public class PostgreStringTypeHandler extends PostgreTypeHandler {

    public static final PostgreStringTypeHandler INSTANCE = new PostgreStringTypeHandler();

    private PostgreStringTypeHandler() {
        // disallow constructing singleton class
    }

    @Override
    public int getTypeModifiers(@NotNull PostgreDataType type, @NotNull String typeName, @NotNull String[] typmod) throws DBException {
        switch (typmod.length) {
            case 0:
                return EMPTY_MODIFIERS;
            case 1:
                return getStringModifiers(type, CommonUtils.toInt(typmod[0]));
            default:
                return super.getTypeModifiers(type, typeName, typmod);
        }
    }

    @NotNull
    @Override
    public String getTypeModifiersString(@NotNull PostgreDataType type, int typmod) {
        final StringBuilder sb = new StringBuilder();
        if (typmod > 0) {
            final Integer length = getTypeLength(type, typmod);
            if (length != null) {
                sb.append('(').append(length).append(')');
            }
        }
        return sb.toString();
    }

    @Nullable
    @Override
    public Integer getTypeLength(@NotNull PostgreDataType type, int typmod) {
        if (typmod < 0) {
            return null;
        }
        switch ((int) type.getObjectId()) {
            case PostgreOid.BIT:
            case PostgreOid.VARBIT:
                return typmod;
            default:
                return typmod - 4;
        }
    }

    private static int getStringModifiers(@NotNull PostgreDataType type, int length) throws DBException {
        if (length < 0) {
            throw new DBException("Length for type '" + type.getName() + "' must be at least 1");
        }
        switch ((int) type.getObjectId()) {
            case PostgreOid.BIT:
            case PostgreOid.VARBIT:
                return length;
            default:
                return length + 4;
        }
    }
}
