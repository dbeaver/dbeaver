/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
