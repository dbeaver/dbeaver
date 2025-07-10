/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
 *
 * Contributors:
 *    Stefan Uhrig - initial implementation
 *    Frederick Arand - moved code to separate file
 */
package org.jkiss.dbeaver.ext.hana.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;

import java.sql.SQLException;
import java.sql.Types;

public class HANARealVectorValueHandler extends HANAVectorValueHandler {

    public static final HANARealVectorValueHandler INSTANCE = new HANARealVectorValueHandler();

    @Override
    protected void bindVectorParameter(@NotNull JDBCPreparedStatement statement, int paramIndex,
            @NotNull JDBCCollection collection)
            throws DBCException, SQLException {
        if (collection.getComponentType().getTypeID() != Types.REAL) {
            throw new DBCException("Only REAL numbers are allowed in REAL_VECTOR");
        }
        float[] nvals = new float[collection.size()];
        for (int i = 0; i < nvals.length; ++i) {
            Float val = (Float) collection.get(i);
            if (val == null) {
                throw new DBCException("NULL elements are not allowed in REAL_VECTOR");
            }
            nvals[i] = val;
        }
        statement.setObject(paramIndex, nvals);
    }
}
