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

package org.jkiss.dbeaver.ext.altibase;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseProcedureParameter;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseTrigger;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseTriggerType;
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
import org.jkiss.utils.StandardConstants;
import org.osgi.framework.Version;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Altibase utils
 */
public class AltibaseUtils {

    private static final Log log = Log.getLog(AltibaseUtils.class);

    /*
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

    public static String getTriggerSourceWithHeader(DBRProgressMonitor monitor, AltibaseTrigger trigger, String source) throws DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TRIGGER ").append(trigger.getName()).append(" ");
        AltibaseTriggerType type = trigger.getType();
        if (type.isDbEvent()) {
            sql.append(type.getDisplayName());
        } else if (trigger.getTable() != null) {
            sql.append("FOR ").append(DBUtils.getQuotedIdentifier(trigger.getTable()));
            sql.append(" ").append(type.getDisplayName());
        }
        sql.append("\n").append(source);

        return sql.toString();
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
    */
    // Since here
    
	public static final String NEW_LINE = System.getProperty(StandardConstants.ENV_LINE_SEPARATOR);
	
	public static boolean isEmpty(String aValue) {
		if (aValue != null)
			return (aValue.length() < 1);
		else
			return true;
	}
	
	/*
	 * DBMS_METADATA: Object type name
	 * In case of space in the name, DBMS_METADATA requires replace space to underscore.
	 */
	public static String getDmbsMetaDataObjTypeName(String objTypeName)
	{
		if (isEmpty(objTypeName)) {
			return "UNKNOWN_OBJECT_TYPE";
		}
		
		return objTypeName.replaceAll(" ", "_");
	}
	
	public static String getQuotedName(String aSchemaName, String aObjName)
	{
		StringBuilder sQuotedName = new StringBuilder();
		
		if (isEmpty(aSchemaName) == false) {
			sQuotedName.append("\"").append(aSchemaName).append("\".");
		}
		
		if (isEmpty(aObjName) == false) {
			sQuotedName.append("\"").append(aObjName).append("\"");
		}
		
		return sQuotedName.toString();
	}

}
