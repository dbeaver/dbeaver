/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * OracleTableConstraint
 */
public class OracleTableConstraint extends OracleTableConstraintBase {

    static final Log log = Log.getLog(OracleTableConstraint.class);

    private String searchCondition;

    public OracleTableConstraint(OracleTableBase oracleTable, String name, DBSEntityConstraintType constraintType, String searchCondition, OracleObjectStatus status)
    {
        super(oracleTable, name, constraintType, status, false);
        this.searchCondition = searchCondition;
    }

    public OracleTableConstraint(OracleTableBase table, ResultSet dbResult)
    {
        super(
            table,
            JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME"),
            getConstraintType(JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TYPE")),
            CommonUtils.notNull(
                CommonUtils.valueOf(OracleObjectStatus.class, JDBCUtils.safeGetStringTrimmed(dbResult, "STATUS")),
                OracleObjectStatus.ENABLED),
            true);
        this.searchCondition = JDBCUtils.safeGetString(dbResult, "SEARCH_CONDITION");
    }

    @Property(viewable = true, editable = true, order = 4)
    public String getSearchCondition()
    {
        return searchCondition;
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            getTable(),
            this);
    }

    public static DBSEntityConstraintType getConstraintType(String code)
    {
        if ("C".equals(code)) {
            return DBSEntityConstraintType.CHECK;
        } else if ("P".equals(code)) {
            return DBSEntityConstraintType.PRIMARY_KEY;
        } else if ("U".equals(code)) {
            return DBSEntityConstraintType.UNIQUE_KEY;
        } else if ("R".equals(code)) {
            return DBSEntityConstraintType.FOREIGN_KEY;
        } else if ("V".equals(code)) {
            return OracleView.CONSTRAINT_WITH_CHECK_OPTION;
        } else if ("O".equals(code)) {
            return OracleView.CONSTRAINT_WITH_READ_ONLY;
        } else {
            log.debug("Unsupported constraint type: " + code);
            return DBSEntityConstraintType.CHECK;
        }
    }
}
