/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.derby.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DerbyMetaModel
 */
public class DerbyMetaModel extends GenericMetaModel
{
    private Pattern ERROR_POSITION_PATTERN = Pattern.compile(" at line ([0-9]+), column ([0-9]+)\\.");

    public DerbyMetaModel() {
        super();
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject.getDataSource(), "Read view definition")) {
            return JDBCUtils.queryString(session, "SELECT v.VIEWDEFINITION from SYS.SYSVIEWS v,SYS.SYSTABLES t,SYS.SYSSCHEMAS s\n" +
                "WHERE v.TABLEID=t.TABLEID AND t.SCHEMAID=s.SCHEMAID AND s.SCHEMANAME=? AND t.TABLENAME=?", sourceObject.getContainer().getName(), sourceObject.getName());
        } catch (SQLException e) {
            throw new DBException(e, sourceObject.getDataSource());
        }
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        String procMethodName = sourceObject.getDescription();
        int divPos = procMethodName.lastIndexOf('.');
        if (divPos == -1) {
            throw new DBException("Bad Java method reference: " + procMethodName);
        }
        String className = procMethodName.substring(0, divPos);
        String methodName = procMethodName.substring(divPos + 1);
        return decompileJavaMethod(className, methodName);
    }

    private String decompileJavaMethod(String className, String methodName) throws DBException {
//        JavaDecompiler decompiler = new JavaDecompiler();
//        return decompiler.decompileJavaMethod(className, methodName);
        return "-- Source of " + className + "." + methodName;
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericSequence> loadSequences(@NotNull DBRProgressMonitor monitor, @NotNull GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container.getDataSource(), "Read procedure definition")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT seq.SEQUENCENAME,seq.CURRENTVALUE,seq.MINIMUMVALUE,seq.MAXIMUMVALUE,seq.INCREMENT\n" +
                    "FROM sys.SYSSEQUENCES seq,sys.SYSSCHEMAS s\n" +
                    "WHERE seq.SCHEMAID=s.SCHEMAID AND s.SCHEMANAME=?")) {
                dbStat.setString(1, container.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    List<GenericSequence> result = new ArrayList<GenericSequence>();
                    while (dbResult.next()) {
                        GenericSequence sequence = new GenericSequence(
                            container,
                            JDBCUtils.safeGetString(dbResult, 1),
                            "",
                            JDBCUtils.safeGetLong(dbResult, 2),
                            JDBCUtils.safeGetLong(dbResult, 3),
                            JDBCUtils.safeGetLong(dbResult, 4),
                            JDBCUtils.safeGetLong(dbResult, 5));
                        result.add(sequence);
                    }
                    return result;
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Override
    public DBPErrorAssistant.ErrorPosition getErrorPosition(@NotNull Throwable error) {
        String message = error.getMessage();
        if (!CommonUtils.isEmpty(message)) {
            Matcher matcher = ERROR_POSITION_PATTERN.matcher(message);
            if (matcher.find()) {
                DBPErrorAssistant.ErrorPosition pos = new DBPErrorAssistant.ErrorPosition();
                pos.line = Integer.parseInt(matcher.group(1)) - 1;
                pos.position = Integer.parseInt(matcher.group(2)) - 1;
                return pos;
            }
        }
        return null;
    }

    @Override
    public String getAutoIncrementClause(GenericTableColumn column) {
        return "GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1)";
    }

}
