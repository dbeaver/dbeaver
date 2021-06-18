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
import org.jkiss.utils.CommonUtils;

public class PostgreTimeTypeHandler extends PostgreTypeHandler {

    public static final PostgreTimeTypeHandler INSTANCE = new PostgreTimeTypeHandler();

    private PostgreTimeTypeHandler() {
        // disallow constructing singleton class
    }

    @Override
    public int getTypeModifiers(@NotNull PostgreDataType type, @NotNull String typeName, @NotNull String[] typmod) throws DBException {
        switch (typmod.length) {
            case 0:
                return EMPTY_MODIFIERS;
            case 1:
                return getTimeModifiers(CommonUtils.toInt(typmod[0]));
            default:
                return super.getTypeModifiers(type, typeName, typmod);
        }
    }

    @NotNull
    @Override
    public String getTypeModifiersString(@NotNull PostgreDataType type, int typmod) {
        final StringBuilder sb = new StringBuilder();
        if (typmod >= 0) {
            final Integer precision = getTypePrecision(type, typmod);
            if (precision != null) {
                sb.append('(').append(precision).append(')');
            }
        }
        return sb.toString();
    }

    @Nullable
    @Override
    public Integer getTypePrecision(@NotNull PostgreDataType type, int typmod) {
        if (typmod < 0) {
            return null;
        }
        return typmod;
    }

    private static int getTimeModifiers(int precision) throws DBException {
        if (precision < 0 || precision > 6) {
            throw new DBException("Time precision " + precision + " must be between 0 and 6");
        }
        return precision;
    }
}
