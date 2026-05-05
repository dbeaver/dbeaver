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
package org.jkiss.dbeaver.ext.postgresql.model.sql.generator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.generator.SQLGeneratorProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

public class SQLGeneratorProcedureCheck extends SQLGeneratorProcedure {

    /**
     * Generate PostgreSQL procedure check SQL - via https://github.com/okbob/plpgsql_check
     */
    @Override
    protected void generateSQL(DBRProgressMonitor monitor, StringBuilder sql, DBSProcedure proc) throws DBException {
        sql.append("select * from plpgsql_check_function('" + proc.getFullyQualifiedName(DBPEvaluationContext.DML) + "(");
        boolean first = true;
        for (DBSProcedureParameter parameter : CommonUtils.safeCollection(proc.getParameters(monitor))) {
            if (parameter.getParameterKind() == DBSProcedureParameterKind.IN) {
                if (!first) {
                    sql.append(",");
                }
                String typeName = parameter.getParameterType().getFullTypeName();
                sql.append(typeName);
                first = false;
            }            
        }
        sql.append(")'").append(getLineSeparator());
        sql.append("/*, */").append(getLineSeparator());
        sql.append("/* Optional parameters are commented below - they may differ (or be absent) depending on plpgsql_check version */");
        sql.append(getLineSeparator()).append(" /* relid => 0, */ /* oid of relation assigned with trigger function. "
                + "It is necessary for check of any trigger function */");
        sql.append(getLineSeparator()).append(" /* fatal_errors => true, */ /* stop on first error */");
        sql.append(getLineSeparator()).append(" /* other_warnings => true, */ /* show warnings like different attributes number "
                + "in assignmenet on left and right side, variable overlaps function's parameter, "
                + "unused variables, unwanted casting, .. */");
        sql.append(getLineSeparator()).append(" /* extra_warnings => true, */ /* show warnings like missing RETURN, "
                + "shadowed variables, dead code, never read (unused) function's parameter, "
                + "unmodified variables, modified auto variables, .. */");
        sql.append(getLineSeparator()).append(" /* performance_warnings => false, */ /* performance related warnings like "
                + "declared type with type modificator, casting, "
                + "implicit casts in where clause (can be reason why index is not used), .. */");
        sql.append(getLineSeparator()).append(" /* security_warnings => false, */ /* security related checks "
                + "like SQL injection vulnerability detection */");
        sql.append(getLineSeparator()).append(" /* anyelementtype => 'int', */ /* a real type used instead anyelement type */");
        sql.append(getLineSeparator()).append(" /* anyenumtype => '-', */ /* a real type used instead anyenum type */");
        sql.append(getLineSeparator()).append(" /* anyrangetype => 'int4range', */ /* a real type used instead anyrange type */");
        sql.append(getLineSeparator()).append(" /* anycompatibletype => 'int', */ /* a real type used instead anycompatible type */");
        sql.append(getLineSeparator()).append(" /* anycompatiblerangetype => 'int4range', */ /* a real type used instead "
                + "anycompatible range type */");
        sql.append(getLineSeparator()).append(" /* without_warnings => false, */ /* disable all warnings */");
        sql.append(getLineSeparator()).append(" /* all_warnings => false, */ /* enable all warnings */");
        sql.append(getLineSeparator()).append(" /* newtable => NULL, */ /* the names of NEW or OLD transitive tables. "
                + "These parameters are required when transitive tables are used */");
        sql.append(getLineSeparator()).append(" /* oldtable => NULL */");
        sql.append(getLineSeparator()).append(")");
        sql.append(";").append(getLineSeparator()).append(getLineSeparator());
    }
    
}
