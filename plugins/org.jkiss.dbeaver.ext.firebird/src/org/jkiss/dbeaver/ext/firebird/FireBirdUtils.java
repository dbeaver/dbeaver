/*
 * DBeaver - Universal Database Manager
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

package org.jkiss.dbeaver.ext.firebird;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdProcedureParameter;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdTrigger;
import org.jkiss.dbeaver.ext.firebird.model.FireBirdTriggerType;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.lang.reflect.InvocationTargetException;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Firebird utils
 */
public class FireBirdUtils {

    private static final Log log = Log.getLog(FireBirdUtils.class);

    public static String getProcedureSource(DBRProgressMonitor monitor, GenericProcedure procedure)
        throws DBException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, procedure, "Load procedure source code")) {
            String source = "";
            if (procedure.getProcedureType() == DBSProcedureType.PROCEDURE) {
                DatabaseMetaData fbMetaData = session.getOriginal().getMetaData();
                source = (String) fbMetaData.getClass().getMethod("getProcedureSourceCode", String.class).invoke(fbMetaData, procedure.getName());
                if (CommonUtils.isEmpty(source)) {
                    return null;
                }
            } else if (procedure.getDataSource().isServerVersionAtLeast(3, 0)) {
                String sql = "SELECT RDB$FUNCTION_SOURCE FROM RDB$FUNCTIONS WHERE RDB$FUNCTION_NAME =?";
                try (JDBCPreparedStatement dbStat = session.prepareStatement(sql))
                {
                    dbStat.setString(1, procedure.getName());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.nextRow()) {
                            source =  JDBCUtils.safeGetString(dbResult, 1);
                        }
                    }
                }
            } else {
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

    private static String getProcedureSourceWithHeader(DBRProgressMonitor monitor, GenericProcedure procedure, String source) throws DBException {
        StringBuilder sql = new StringBuilder();
        boolean isFunction = procedure.getProcedureType() == DBSProcedureType.FUNCTION;
        sql.append("CREATE OR ALTER ");
        if (isFunction) {
            sql.append(SQLConstants.KEYWORD_FUNCTION);
        } else {
            sql.append(SQLConstants.KEYWORD_PROCEDURE);
        }
        sql.append(" ").append(procedure.getName()).append(" ");
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
            Map<String, String> domainNames = new HashMap<>();
            try (JDBCSession session = DBUtils.openUtilSession(monitor, procedure, "Load domains used in procedure")) {
                try (JDBCPreparedStatement stmt = session.prepareStatement(
                        "SELECT RDB$PARAMETER_NAME, RDB$FIELD_SOURCE " +
                        "FROM RDB$PROCEDURE_PARAMETERS rpp " +
                        "WHERE RDB$PROCEDURE_NAME = ? " +
                        "AND LEFT(rpp.RDB$FIELD_SOURCE, 4) <> 'RDB$'")) {
                    stmt.setString(1, procedure.getName());
                    try (JDBCResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String paramName = rs.getString(1);
                            String domainName = rs.getString(2);
                            if (paramName != null && domainName != null) {
                                domainNames.put(paramName.trim(), domainName.trim());
                            }
                        }
                    }
                } catch (SQLException e) {
                    throw new DBException("Unable to load domains used in procedure", e);
                }
                domainNames = Collections.unmodifiableMap(domainNames);
            }
            if (!args.isEmpty()) {
                sql.append("(");
                for (int i = 0; i < args.size(); i++) {
                    GenericProcedureParameter param = args.get(i);
                    if (param.getParameterKind() == DBSProcedureParameterKind.RETURN) {
                        continue;
                    }
                    if (i > 0) sql.append(", ");
                    printParam(sql, param, domainNames);
                }
                sql.append(")\n");
            }
            if (!results.isEmpty()) {
                sql.append("RETURNS ");
                if (isFunction) {
                    GenericProcedureParameter param = results.get(0); // According Firebird documentation, functions return just one data type without parameter name
                    sql.append(param.getTypeName());
                    String typeModifiers = SQLUtils.getColumnTypeModifiers(param.getDataSource(), param, param.getTypeName(), param.getDataKind());
                    if (typeModifiers != null) {
                        sql.append(typeModifiers);
                    }
                    sql.append("\n");
                } else {
                    sql.append("(\n");
                    for (int i = 0; i < results.size(); i++) {
                        sql.append('\t');
                        GenericProcedureParameter param = results.get(i);
                        printParam(sql, param, domainNames);
                        if (i < results.size() - 1) sql.append(",");
                        sql.append('\n');
                    }
                    sql.append(")\n");
                }
            }
        }

        sql.append("AS\n").append(source);

        return sql.toString();
    }

    private static void printParam(StringBuilder sql, GenericProcedureParameter param, Map<String, String> domainNames) {
        String paramName = DBUtils.getQuotedIdentifier(param);
        sql.append(paramName).append(" ");
        String domainName = domainNames.get(paramName.trim());
        if (domainName != null) {
            sql.append(domainName);
            return;
        }
        sql.append(param.getTypeName());
        String typeModifiers = SQLUtils.getColumnTypeModifiers(param.getDataSource(), param, param.getTypeName(), param.getDataKind());
        if (typeModifiers != null) {
            sql.append(typeModifiers);
        }
        boolean notNull = param.isRequired();
        if (notNull) {
            sql.append(" NOT NULL");
        }
        if (param instanceof FireBirdProcedureParameter && param.getParameterKind() == DBSProcedureParameterKind.IN) {
            String defaultValue = ((FireBirdProcedureParameter) param).getDefaultValue();
            if (!CommonUtils.isEmpty(defaultValue)) {
                sql.append(" ").append(defaultValue);
            }
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

    public static Map<String, String> readColumnDomainTypes(DBRProgressMonitor monitor, GenericTableBase table) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, table, "Read column domain type")) {
            // Read metadata
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT RF.RDB$FIELD_NAME,RF.RDB$FIELD_SOURCE FROM RDB$RELATION_FIELDS RF WHERE RF.RDB$RELATION_NAME=?")) {
                dbStat.setString(1, table.getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    Map<String, String> dtMap = new HashMap<>();
                    while (dbResult.next()) {
                        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, 1);
                        String domainTypeName = JDBCUtils.safeGetStringTrimmed(dbResult, 2);
                        if (!CommonUtils.isEmpty(columnName) && !CommonUtils.isEmpty(domainTypeName)) {
                            dtMap.put(columnName, domainTypeName);
                        }
                    }
                    return dtMap;
                }
            }

        } catch (SQLException ex) {
            throw new DBException("Error reading column domain types for " + table.getName(), ex);
        }

    }
}
