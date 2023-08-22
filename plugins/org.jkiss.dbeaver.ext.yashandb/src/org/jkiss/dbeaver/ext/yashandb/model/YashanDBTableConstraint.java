/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * OracleTableConstraint
 */
public class YashanDBTableConstraint extends YashanDBTableConstraintBase {

    private static final Log log = Log.getLog(YashanDBTableConstraint.class);

    private String searchCondition;

    public YashanDBTableConstraint(YashanDBTableBase yashanDBTable, String name, DBSEntityConstraintType constraintType, String searchCondition, YashanDBObjectStatus status) {
        super(yashanDBTable, name, constraintType, status, false);
        this.searchCondition = searchCondition;
    }

    public YashanDBTableConstraint(YashanDBTableBase table, ResultSet dbResult) {
        super(
                table,
                JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"),
                getConstraintType(JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TYPE")),
                CommonUtils.notNull(
                        CommonUtils.valueOf(YashanDBObjectStatus.class, JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS")),
                        YashanDBObjectStatus.ENABLED),
                true);
        this.searchCondition = JDBCUtils.safeGetString(dbResult, "SEARCH_CONDITION");
    }

    @Property(viewable = true, editable = true, order = 4)
    public String getSearchCondition() {
        return searchCondition;
    }

    public void setSearchCondition(String searchCondition) {
        this.searchCondition = searchCondition;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
                getTable().getContainer(),
                getTable(),
                this);
    }

    public static DBSEntityConstraintType getConstraintType(String code) {
        switch (code) {
            case "C":
                return DBSEntityConstraintType.CHECK;
            case "P":
                return DBSEntityConstraintType.PRIMARY_KEY;
            case "U":
                return DBSEntityConstraintType.UNIQUE_KEY;
            case "R":
                return DBSEntityConstraintType.FOREIGN_KEY;
            case "CHECK":
                return DBSEntityConstraintType.CHECK;
            case "PRIMARY KEY":
                return DBSEntityConstraintType.PRIMARY_KEY;
            case "UNIQUE KEY":
                return DBSEntityConstraintType.UNIQUE_KEY;
            case "FOREIGN KEY":
                return DBSEntityConstraintType.FOREIGN_KEY;
            case "V":
                return YashanDBConstants.CONSTRAINT_WITH_CHECK_OPTION;
            case "O":
                return YashanDBConstants.CONSTRAINT_WITH_READ_ONLY;
            case "H":
                return YashanDBConstants.CONSTRAINT_HASH_EXPRESSION;
            case "F":
                return YashanDBConstants.CONSTRAINT_REF_COLUMN;
            case "S":
                return YashanDBConstants.CONSTRAINT_SUPPLEMENTAL_LOGGING;
            default:
                log.debug("Unsupported Oracle constraint type: " + code);
                return DBSEntityConstraintType.CHECK;
        }
    }
}
