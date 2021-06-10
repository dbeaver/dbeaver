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

import java.util.Arrays;

public class PostgreNumericTypeHandler extends PostgreTypeHandler {

    public static final PostgreNumericTypeHandler INSTANCE = new PostgreNumericTypeHandler();

    private static final int NUMERIC_MASK_PRECISION  = 0xffff_0000;
    private static final int NUMERIC_MASK_SCALE      = 0x0000_ffff;

    private PostgreNumericTypeHandler() {
        // disallow constructing singleton class
    }

    @Override
    public int getTypeModifiers(@NotNull PostgreDataType type, @NotNull String typeName, @NotNull String[] typmod) throws DBException {
        switch (typmod.length) {
            case 0:
                return -1;
            case 1:
                return getNumberModifiers(CommonUtils.toInt(typmod[0]), 0);
            case 2:
                return getNumberModifiers(CommonUtils.toInt(typmod[0]), CommonUtils.toInt(typmod[1]));
            default:
                throw new DBException("Invalid modifiers for numeric type: " + Arrays.toString(typmod));
        }
    }

    @NotNull
    @Override
    public String getTypeModifiersString(@NotNull PostgreDataType type, int typmod) {
        final StringBuilder sb = new StringBuilder();
        if (typmod > 0) {
            final Integer precision = getTypePrecision(type, typmod);
            final Integer scale = getTypeScale(type, typmod);
            if (precision != null && precision > 0) {
                sb.append('(').append(precision);
                if (scale != null && scale > 0) {
                    sb.append(", ").append(scale);
                }
                sb.append(')');
            }
        }
        return sb.toString();
    }

    @Nullable
    @Override
    public Integer getTypePrecision(@NotNull PostgreDataType type, int typmod) {
        if (typmod < 0) {
            return 0;
        }
        return (typmod & NUMERIC_MASK_PRECISION) >> 16;
    }

    @Nullable
    @Override
    public Integer getTypeScale(@NotNull PostgreDataType type, int typmod) {
        if (typmod < 0) {
            return 0;
        }
        return (typmod & NUMERIC_MASK_SCALE) - 4;
    }

    private static int getNumberModifiers(int precision, int scale) throws DBException {
        if (precision < 1 || precision > 1000) {
            throw new DBException("Numeric precision " + +precision + " must be between 1 and 1000");
        }
        if (scale < 0 || scale > precision) {
            throw new DBException("Numeric scale " + +scale + " must be between 0 and " + precision);
        }
        return (precision << 16) | scale | 4;
    }
}
