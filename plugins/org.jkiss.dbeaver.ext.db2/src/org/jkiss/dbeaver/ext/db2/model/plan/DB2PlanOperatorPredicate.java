/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedValueObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * DB2 EXPLAIN_PREDICATE table
 * 
 * @author Denis Forveille
 */
public class DB2PlanOperatorPredicate implements DBPNamedValueObject {

    private DB2PlanOperator db2Operator;

    private Integer predicateId;
    private String howApplied;
    private String whenEvaluated;
    private String predicateText;

    private String displayName;

    // ------------
    // Constructors
    // ------------

    public DB2PlanOperatorPredicate(JDBCResultSet dbResult, DB2PlanOperator db2Operator)
    {
        this.db2Operator = db2Operator;

        this.predicateId = JDBCUtils.safeGetInteger(dbResult, "PREDICATE_ID");
        this.howApplied = JDBCUtils.safeGetString(dbResult, "HOW_APPLIED");
        this.whenEvaluated = JDBCUtils.safeGetString(dbResult, "WHEN_EVALUATED");
        this.predicateText = JDBCUtils.safeGetString(dbResult, "PREDICATE_TEXT");

        StringBuilder sb = new StringBuilder(32);
        sb.append(predicateId);
        if (whenEvaluated != null) {
            sb.append(" - ");
            sb.append(whenEvaluated);
        }
        sb.append(" ");
        sb.append(howApplied);
        displayName = sb.toString();
    }

    // -----------------
    // Business contract
    // -----------------

    @NotNull
    @Override
    public String getName()
    {
        return displayName;
    }

    @NotNull
    @Override
    public Object getObjectValue()
    {
        return predicateText;
    }

    @Override
    public String toString()
    {
        return predicateText;
    }

    // ----------------
    // Standard Getters
    // ----------------

    public Integer getPredicateId()
    {
        return predicateId;
    }

    public String getHowApplied()
    {
        return howApplied;
    }

    public String getWhenEvaluated()
    {
        return whenEvaluated;
    }

    public String getPredicateText()
    {
        return predicateText;
    }

    public DB2PlanOperator getDb2Operator()
    {
        return db2Operator;
    }

}
