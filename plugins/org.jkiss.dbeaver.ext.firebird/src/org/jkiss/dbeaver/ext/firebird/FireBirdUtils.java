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

package org.jkiss.dbeaver.ext.firebird;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdTrigger;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdTriggerType;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.lang.reflect.InvocationTargetException;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FireBird utils
 */
public class FireBirdUtils {

    private static final Log log = Log.getLog(FireBirdUtils.class);

    public static String getProcedureSource(DBRProgressMonitor monitor, GenericProcedure procedure)
        throws DBException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, procedure, "Load procedure source code")) {
            DatabaseMetaData fbMetaData = session.getOriginal().getMetaData();
            String source = (String) fbMetaData.getClass().getMethod("getProcedureSourceCode", String.class).invoke(fbMetaData, procedure.getName());
            if (CommonUtils.isEmpty(source)) {
                return null;
            }

            return getProcedureSourceWithHeader(monitor, procedure, source);
        } catch (SQLException e) {
            throw new DBException("Can't read source code of procedure '" + procedure.getName() + "'", e);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static String getViewSource(DBRProgressMonitor monitor, GenericTableBase view)
        throws DBException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, view, "Load view source code")) {
            DatabaseMetaData fbMetaData = session.getOriginal().getMetaData();
            String source = (String) fbMetaData.getClass().getMethod("getViewSourceCode", String.class).invoke(fbMetaData, view.getName());
            if (CommonUtils.isEmpty(source)) {
                return null;
            }

            return getViewSourceWithHeader(monitor, view, source);
        } catch (SQLException e) {
            throw new DBException("Can't read source code of view '" + view.getName() + "'", e);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static String getTriggerSource(DBRProgressMonitor monitor, FireBirdTrigger trigger)
        throws DBException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, trigger, "Load trigger source code")) {
            DatabaseMetaData fbMetaData = session.getOriginal().getMetaData();
            String source = (String) fbMetaData.getClass().getMethod("getTriggerSourceCode", String.class).invoke(fbMetaData, trigger.getName());
            if (CommonUtils.isEmpty(source)) {
                return null;
            }

            return getTriggerSourceWithHeader(monitor, trigger, source);
        } catch (SQLException e) {
            throw new DBException("Can't read source code of trigger '" + trigger.getName() + "'", e);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static String getProcedureSourceWithHeader(DBRProgressMonitor monitor, GenericProcedure procedure, String source) throws DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE OR ALTER PROCEDURE ").append(procedure.getName()).append(" ");
        Collection<GenericProcedureParameter> parameters = procedure.getParameters(monitor);
        if (parameters != null && !parameters.isEmpty()) {
            List<GenericProcedureParameter> args = new ArrayList<>();
            List<GenericProcedureParameter> results = new ArrayList<>();
            for (GenericProcedureParameter param : parameters) {
                if (param.getParameterKind() == DBSProcedureParameterKind.OUT || param.getParameterKind() == DBSProcedureParameterKind.RETURN) {
                    results.add(param);
                } else {
                    args.add(param);
                }
            }
            if (!args.isEmpty()) {
                sql.append("(");
                for (int i = 0; i < args.size(); i++) {
                    GenericProcedureParameter param = args.get(i);
                    if (i > 0) sql.append(", ");
                    printParam(sql, param);
                }
                sql.append(")\n");
            }
            if (!results.isEmpty()) {
                sql.append("RETURNS (\n");
                for (int i = 0; i < results.size(); i++) {
                    sql.append('\t');
                    GenericProcedureParameter param = results.get(i);
                    printParam(sql, param);
                    if (i < results.size() - 1) sql.append(",");
                    sql.append('\n');
                }
                sql.append(")\n");
            }
        }

        sql.append("AS\n").append(source);

        return sql.toString();
    }

    private static void printParam(StringBuilder sql, GenericProcedureParameter param) {
        sql.append(DBUtils.getQuotedIdentifier(param)).append(" ").append(param.getTypeName());
        String typeModifiers = SQLUtils.getColumnTypeModifiers(param.getDataSource(), param, param.getTypeName(), param.getDataKind());
        if (typeModifiers != null) {
            sql.append(typeModifiers);
        }
    }

    public static String getViewSourceWithHeader(DBRProgressMonitor monitor, GenericTableBase view, String source) throws DBException {
        Version version = getFireBirdServerVersion(view.getDataSource());
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE ");
        if (version.getMajor() > 2 || (version.getMajor() == 2 && version.getMinor() >= 5)) {
            sql.append("OR ALTER ");
        }
        sql.append("VIEW ").append(view.getName()).append(" ");
        Collection<? extends GenericTableColumn> columns = view.getAttributes(monitor);
        if (columns != null) {
            sql.append("(");
            boolean first = true;
            for (GenericTableColumn column : columns) {
                if (!first) {
                    sql.append(", ");
                }
                first = false;
                sql.append(DBUtils.getQuotedIdentifier(column));
            }
            sql.append(")\n");
        }
        sql.append("AS\n").append(source);

        return sql.toString();
    }

    public static String getTriggerSourceWithHeader(DBRProgressMonitor monitor, FireBirdTrigger trigger, String source) throws DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TRIGGER ").append(trigger.getName()).append(" ");
        FireBirdTriggerType type = trigger.getType();
        if (type.isDbEvent()) {
            sql.append(type.getDisplayName());
        } else if (trigger.getTable() != null) {
            sql.append("FOR ").append(DBUtils.getQuotedIdentifier(trigger.getTable()));
            sql.append(" ").append(type.getDisplayName());
        }
        sql.append("\n").append(source);

        return sql.toString();
    }
    
    public static String getPlan(JDBCPreparedStatement statement) {
    	String plan = "";
		try {
			plan = (String) statement.getOriginal().getClass().getMethod("getExecutionPlan").invoke(statement.getOriginal());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return plan;
    }

    private static Pattern VERSION_PATTERN = Pattern.compile(".+\\-V([0-9]+\\.[0-9]+\\.[0-9]+).+");

    public static Version getFireBirdServerVersion(DBPDataSource dataSource) {
        String versionInfo = dataSource.getInfo().getDatabaseProductVersion();
        Matcher matcher = VERSION_PATTERN.matcher(versionInfo);
        if (matcher.matches()) {
            return new Version(matcher.group(1));
        }
        return new Version(0, 0, 0);
    }

}
