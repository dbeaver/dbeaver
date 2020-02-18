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
package org.jkiss.dbeaver.ext.hana.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericSQLDialect;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.util.Arrays;
import java.util.Locale;

public class HANASQLDialect extends GenericSQLDialect {

    private static final Log log = Log.getLog(HANASQLDialect.class);

    public static final String[][] HANA_BEGIN_END_BLOCK = new String[][]{
        {SQLConstants.BLOCK_BEGIN, SQLConstants.BLOCK_END},
        {"IF", SQLConstants.BLOCK_END},
    };

    public HANASQLDialect() {
        super("HANA");
    }

    @Override
    public String[][] getBlockBoundStrings() {
        return HANA_BEGIN_END_BLOCK;
    }

    public void initDriverSettings(JDBCDataSource dataSource, JDBCDatabaseMetaData metaData) {
        super.initDriverSettings(dataSource, metaData);
        // TODO: check if obsolete
        addSQLKeywords(
                Arrays.asList(
                        "REPLACE_REGEXPR"));
    }

    @Override
    public boolean supportsAliasInSelect() {
        return true;
    }

    /*
     * expression evaluation
     */
    @Override
    public String getDualTableName() {
        return "DUMMY";
    }

    @Override
    public String getColumnTypeModifiers(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject column,
            @NotNull String typeName, @NotNull DBPDataKind dataKind) {
        String ucTypeName = CommonUtils.notEmpty(typeName).toUpperCase(Locale.ENGLISH);
        if (("ST_POINT".equals(ucTypeName) || "ST_GEOMETRY".equals(ucTypeName))
                && (column instanceof HANATableColumn)) {
            HANATableColumn hanaColumn = (HANATableColumn) column;
            try {
                int srid = hanaColumn.getAttributeGeometrySRID(new VoidProgressMonitor());
                return "(" + Integer.toString(srid) + ")";
            } catch (DBCException e) {
                log.info("Could not determine SRID of column", e);
            }
        }
        return super.getColumnTypeModifiers(dataSource, column, ucTypeName, dataKind);
    }

}
