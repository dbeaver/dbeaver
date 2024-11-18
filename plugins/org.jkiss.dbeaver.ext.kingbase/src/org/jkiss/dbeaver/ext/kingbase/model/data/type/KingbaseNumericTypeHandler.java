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
package org.jkiss.dbeaver.ext.kingbase.model.data.type;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataType;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseOid;
import org.jkiss.utils.CommonUtils;


public class KingbaseNumericTypeHandler extends KingbaseTypeHandler {

    public static final KingbaseNumericTypeHandler INSTANCE = new KingbaseNumericTypeHandler();

    private static final int NUMERIC_MASK_PRECISION  = 0xffff_0000;
    private static final int NUMERIC_MASK_SCALE      = 0x0000_ffff;

    private KingbaseNumericTypeHandler() {
    }

    @Override
    public int getTypeModifiers(@NotNull KingbaseDataType type, @NotNull String typeName, @NotNull String[] typmod) throws DBException {
        if (typmod.length == 0) {
            return EMPTY_MODIFIERS;
        }
        if (typmod.length == 1 && type.getObjectId() == KingbaseOid.NUMERIC) {
            return getNumberModifiers(CommonUtils.toInt(typmod[0]), 0);
        }
        if (typmod.length == 2 && type.getObjectId() == KingbaseOid.NUMERIC) {
            return getNumberModifiers(CommonUtils.toInt(typmod[0]), CommonUtils.toInt(typmod[1]));
        }
        return super.getTypeModifiers(type, typeName, typmod);
    }

    @NotNull
    @Override
    public String getTypeModifiersString(@NotNull KingbaseDataType type, int typmod) {
        final StringBuilder sb = new StringBuilder();
        if (type.getObjectId() == KingbaseOid.NUMERIC && typmod > 0) {
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
    public Integer getTypePrecision(@NotNull KingbaseDataType type, int typmod) {
        if (type.getObjectId() == KingbaseOid.FLOAT4) {
            return 6;
        }
        if (type.getObjectId() == KingbaseOid.FLOAT8) {
            return 15;
        }
        if (type.getObjectId() == KingbaseOid.NUMERIC && typmod >= 0) {
            return (typmod & NUMERIC_MASK_PRECISION) >> 16;
        }
        return null;
    }

    @Nullable
    @Override
    public Integer getTypeScale(@NotNull KingbaseDataType type, int typmod) {
        if (type.getObjectId() == KingbaseOid.FLOAT4) {
            return 9;
        }
        if (type.getObjectId() == KingbaseOid.FLOAT8) {
            return 17;
        }
        if (typmod < 0) {
            return null;
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
        return (precision << 16) | (scale + 4);
    }
}
