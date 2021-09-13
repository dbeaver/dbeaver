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
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreOid;
import org.jkiss.dbeaver.ext.postgresql.model.impls.redshift.PostgreServerRedshift;

public class PostgreTypeHandlerProvider {

    private PostgreTypeHandlerProvider() {
        // disallow constructing singleton class
    }

    @Nullable
    public static PostgreTypeHandler getTypeHandler(@NotNull PostgreDataType type) {
        if (PostgreUtils.isGISDataType(type.getTypeName().toLowerCase())) {
            if (type.getDataSource().getServerType() instanceof PostgreServerRedshift) {
                return PostgreEmptyTypeHandler.INSTANCE;
            } else {
                return PostgreGeometryTypeHandler.INSTANCE;
            }
        }
        switch ((int) type.getObjectId()) {
            case PostgreOid.NUMERIC:
            case PostgreOid.FLOAT4:
            case PostgreOid.FLOAT8:
                return PostgreNumericTypeHandler.INSTANCE;
            case PostgreOid.INTERVAL:
                return PostgreIntervalTypeHandler.INSTANCE;
            case PostgreOid.CHAR:
            case PostgreOid.BPCHAR:
            case PostgreOid.VARCHAR:
            case PostgreOid.BIT:
            case PostgreOid.VARBIT:
                return PostgreStringTypeHandler.INSTANCE;
            case PostgreOid.TIME:
            case PostgreOid.TIMETZ:
            case PostgreOid.TIMESTAMP:
            case PostgreOid.TIMESTAMPTZ:
                return PostgreTimeTypeHandler.INSTANCE;
            default:
                return null;
        }
    }
}
