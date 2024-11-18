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
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataType;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseOid;

public class KingbaseTypeHandlerProvider {

    private KingbaseTypeHandlerProvider() {
    }

    @Nullable
    public static KingbaseTypeHandler getTypeHandler(@NotNull KingbaseDataType type) {
        
        switch ((int) type.getObjectId()) {
            case KingbaseOid.NUMERIC:
            case KingbaseOid.FLOAT4:
            case KingbaseOid.FLOAT8:
                return KingbaseNumericTypeHandler.INSTANCE;
            case KingbaseOid.INTERVAL:
                return KingbaseIntervalTypeHandler.INSTANCE;
            case KingbaseOid.CHAR:
            case KingbaseOid.BPCHAR:
            case KingbaseOid.VARCHAR:
            case KingbaseOid.BIT:
            case KingbaseOid.VARBIT:
                return KingbaseStringTypeHandler.INSTANCE;
            case KingbaseOid.TIME:
            case KingbaseOid.TIMETZ:
            case KingbaseOid.TIMESTAMP:
            case KingbaseOid.TIMESTAMPTZ:
                return KingbaseTimeTypeHandler.INSTANCE;
            default:
                return null;
        }
    }
}
