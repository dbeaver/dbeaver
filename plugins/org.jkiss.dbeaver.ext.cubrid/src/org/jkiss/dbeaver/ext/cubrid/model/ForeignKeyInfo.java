/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.cubrid.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.cubrid.model.meta.CubridMetaObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;

/**
* Helper class for reading foreign key fields
 */

public class ForeignKeyInfo {

    String pkColumnName;
    String fkTableCatalog;
    String fkTableSchema;
    String fkTableName;
    String fkColumnName;
    int keySeq;
    int updateRuleNum;
    int deleteRuleNum;
    String fkName;
    String pkName;
    int deferabilityNum;

    public void fetchColumnsInfo (CubridMetaObject fkObject, @NotNull JDBCResultSet dbResult) {
        pkColumnName = CubridUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.PKCOLUMN_NAME);
        fkTableCatalog = CubridUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.FKTABLE_CAT);
        fkTableSchema = CubridUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.FKTABLE_SCHEM);
        fkTableName = CubridUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.FKTABLE_NAME);
        fkColumnName = CubridUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.FKCOLUMN_NAME);
        keySeq = CubridUtils.safeGetInt(fkObject, dbResult, JDBCConstants.KEY_SEQ);
        updateRuleNum = CubridUtils.safeGetInt(fkObject, dbResult, JDBCConstants.UPDATE_RULE);
        deleteRuleNum = CubridUtils.safeGetInt(fkObject, dbResult, JDBCConstants.DELETE_RULE);
        fkName = CubridUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.FK_NAME);
        pkName = CubridUtils.safeGetStringTrimmed(fkObject, dbResult, JDBCConstants.PK_NAME);
        deferabilityNum = CubridUtils.safeGetInt(fkObject, dbResult, JDBCConstants.DEFERRABILITY);
    }
}
