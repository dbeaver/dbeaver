package org.jkiss.dbeaver.ext.dm.model.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.DmDDLFormat;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.DmDataType;
import org.jkiss.dbeaver.ext.dm.model.DmExecutionContext;
import org.jkiss.dbeaver.ext.dm.model.DmObjectType;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.model.DmTableBase;
import org.jkiss.dbeaver.ext.dm.model.DmTablePhysical;
import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.ext.dm.model.source.DmStatefulObject;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

public class DmUtils {

	private static final Log log = Log.getLog(DmUtils.class);

	public static String getDDL(DBRProgressMonitor monitor, String objectType, DmTableBase object,
			DmDDLFormat ddlFormat, Map<String, Object> options) throws DBException {
		String objectFullName = DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
		DmSchema schema = object.getContainer();
		final DmDataSource dataSource = object.getDataSource();
		monitor.beginTask("Load sources for " + objectType + " '" + objectFullName + "'...", 1);
		try (final JDBCSession session = DBUtils.openMetaSession(monitor, object,
				"Load source code for " + objectType + " '" + objectFullName + "'")) {
			if (dataSource.isAtLeastV9()) {
				try {
					JDBCUtils.executeProcedure(session, "begin\n"
							+ "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'SQLTERMINATOR',true);\n"
							+ "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'STORAGE',"
							+ ddlFormat.isShowStorage() + ");\n"
							+ "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'TABLESPACE',"
							+ ddlFormat.isShowTablespace() + ");\n"
							+ "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'SEGMENT_ATTRIBUTES',"
							+ ddlFormat.isShowSegments() + ");\n"
							+ "DBMS_METADATA.SET_TRANSFORM_PARAM(DBMS_METADATA.SESSION_TRANSFORM,'EMIT_SCHEMA',"
							+ CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true)
							+ ");\n" + "end;");
				} catch (SQLException e) {
					log.error("Can't apply DDL transform parameters", e);
				}
			}
			String ddl;
			try (JDBCPreparedStatement dbStat = session.prepareStatement(
					"SELECT DBMS_METADATA.GET_DDL(?,?" + (schema == null ? "" : ",?") + ") TXT FROM DUAL")) {
				dbStat.setString(1, objectType);
				dbStat.setString(2, object.getName());
				if (schema != null) {
					dbStat.setString(3, schema.getName());
				}
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					if (dbResult.next()) {
						Object ddlValue = dbResult.getObject(1);
						if (ddlValue instanceof Clob) {
							StringWriter buf = new StringWriter();
							try (Reader clobReader = ((Clob) ddlValue).getCharacterStream()) {
								IOUtils.copyText(clobReader, buf);
							} catch (IOException e) {
								e.printStackTrace(new PrintWriter(buf, true));
							}
							ddl = buf.toString();
						} else {
							ddl = CommonUtils.toString(ddlValue);
						}
					} else {
						log.warn("No DDL for " + objectType + " '" + objectFullName + "'");
						return "-- EMPTY DDL";
					}
				}
			}
						
			if (ddlFormat != DmDDLFormat.COMPACT) { //此处获取表视图以及列的注释
				try (JDBCPreparedStatement dbStat = session
						.prepareStatement("SELECT COMMENT$ AS \"TABCOMMENT\" FROM SYS.SYSTABLECOMMENTS WHERE TVNAME = ? AND SCHNAME = ?")) {
					dbStat.setString(1, object.getName());
					if (schema != null) {
						dbStat.setString(2, schema.getName());
					}
					try (JDBCResultSet dbResult = dbStat.executeQuery()) {
						if (dbResult.next()) {
							String comment="COMMENT ON TABLE \""+schema.getName()+"\".\""+object.getName()+"\" IS '"+dbResult.getString(1)+"';";
							ddl += "\n" + comment;
						}
					}
				} catch (Exception e) {
					log.debug("Error reading dependent DDL", e);
				}
				
				try (JDBCPreparedStatement dbStat = session
						.prepareStatement("SELECT \"COLNAME\",\"COMMENT$\" AS COLCOMMENTS FROM SYS.SYSCOLUMNCOMMENTS WHERE TVNAME = ? AND SCHNAME = ?")){
					dbStat.setString(1, object.getName());
					if (schema != null) {
						dbStat.setString(2, schema.getName());
					}
					try (JDBCResultSet dbResult = dbStat.executeQuery()) {
						while (dbResult.next()) {
							String comment="COMMENT ON COLUMN \""+schema.getName()+"\".\""+object.getName()+"\".\""+dbResult.getString(1)+"\" IS '"+dbResult.getString(2)+"'";
							ddl += "\n" + comment;
						}
					}
				} catch (Exception e) {
					log.debug("Error reading dependent DDL", e);
				}
			}
			return ddl;
		} catch (SQLException e) {
			if (object instanceof DmTablePhysical) {
				log.error("Error generating Oracle DDL. Generate default.", e);
				return DBStructUtils.generateTableDDL(monitor, object, options, true);
			} else {
				throw new DBException(e, dataSource);
			}
		} finally {
			monitor.done();
		}
	}

	/**
	 * 获取所有管理员视图前缀
	 * 
	 * @param monitor
	 * @param dataSource
	 * @param viewName
	 * @return
	 */
	public static String getAdminAllViewPrefix(DBRProgressMonitor monitor, DmDataSource dataSource, String viewName) {
		boolean userDBAView = CommonUtils.toBoolean(dataSource.getContainer().getConnectionConfiguration()
				.getProviderProperty(DmConstants.PROP_ALWAYS_USE_DBA_VIEWS));
		if (userDBAView) {
			String dbaView = "DBA_" + viewName;
			if (dataSource.isViewAvailable(monitor, DmConstants.SCHEMA_SYS, dbaView)) {
				return DmUtils.getSysSchemaPrefix(dataSource) + dbaView;
			}
		}
		return DmUtils.getSysSchemaPrefix(dataSource) + "ALL_" + viewName;
	}

	/**
	 * 使用SYS Schema前缀
	 * 
	 * @param dataSource
	 * @return
	 */
	public static String getSysSchemaPrefix(DmDataSource dataSource) {
		boolean useSysView = CommonUtils.toBoolean(dataSource.getContainer().getConnectionConfiguration()
				.getProviderProperty(DmConstants.PROP_METADATA_USE_SYS_SCHEMA));
		if (useSysView) {
			return DmConstants.SCHEMA_SYS + ".";
		} else {
			return "";
		}
	}

	public static String getSysCatalogHint(DmDataSource dataSource) {
		return dataSource.isUseRuleHint() ? "/*+RULE*/" : "";
	}

	public static String getSource(DBRProgressMonitor monitor, DmSourceObject sourceObject, boolean body,
			boolean insertCreateReplace) throws DBCException {
		if (sourceObject.getSourceType().isCustom()) {
			log.warn("Can't read source for custom source objects");
			return "-- ???? CUSTOM SOURCE";
		}
		String sourceType = sourceObject.getSourceType().name();
		if (sourceType.equals("TRIGGER")) {
			sourceType = "TRIG";
		} else if (sourceType.equals("PROCEDURE")) {
			sourceType = "PROC";
		}
		final DmSchema sourceOwner = sourceObject.getSchema();
		if (sourceOwner == null) {
			log.warn("No source owner for object '" + sourceObject.getName() + "'");
			return null;
		}
		monitor.beginTask("Load sources for '" + sourceObject.getName() + "'...", 1);
		String sysViewName = DmConstants.VIEW_DBA_SOURCE;
		if (!sourceObject.getDataSource().isViewAvailable(monitor, DmConstants.SCHEMA_SYS, sysViewName)) {
			sysViewName = DmConstants.VIEW_ALL_SOURCE;
		}
		try (final JDBCSession session = DBUtils.openMetaSession(monitor, sourceOwner,
				"Load source code for " + sourceType + " '" + sourceObject.getName() + "'")) {
			try (JDBCPreparedStatement dbStat = session
					.prepareStatement("SELECT TEXT FROM " + getSysSchemaPrefix(sourceObject.getDataSource())
							+ sysViewName + " " + "WHERE OWNER=? AND NAME=? " + "ORDER BY LINE")) { // TYPE=? AND，此处去除type直接查询
				//dbStat.setString(1, body ? sourceType + " BODY" : sourceType);
				dbStat.setString(1, sourceOwner.getName());
				dbStat.setString(2, sourceObject.getName());
				dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					StringBuilder source = null;
					while (dbResult.next()) {
						if (monitor.isCanceled()) {
							break;
						}
						final String line = dbResult.getString(1);
						if (source == null) {
							source = new StringBuilder(200);
						}
						source.append(line);
					}
					if (source == null) {
						return null;
					}
					if (insertCreateReplace) {
						return insertCreateReplace(sourceObject, body, source.toString());
					} else {
						return source.toString();
					}
				}
			} catch (SQLException e) {
				throw new DBCException(e, session.getExecutionContext());
			}
		} finally {
			monitor.done();
		}
	}

	public static String insertCreateReplace(DmSourceObject object, boolean body, String source) {
		String sourceType = object.getSourceType().name();
		if (body) {
			sourceType += " BODY";
		}
		Pattern srcPattern = Pattern.compile("^(" + sourceType + ")\\s+(\"{0,1}\\w+\"{0,1})", Pattern.CASE_INSENSITIVE);
		Matcher matcher = srcPattern.matcher(source);
		if (matcher.find()) {
			return "CREATE OR REPLACE " + matcher.group(1) + " " + DBUtils.getQuotedIdentifier(object.getSchema()) + "."
					+ matcher.group(2) + source.substring(matcher.end());
		}
		return source;
	}

	public static boolean getObjectStatus(DBRProgressMonitor monitor, DmStatefulObject object, DmObjectType objectType)
			throws DBCException {
		try (JDBCSession session = DBUtils.openMetaSession(monitor, object,
				"Refresh state of " + objectType.getTypeName() + " '" + object.getName() + "'")) {
			try (JDBCPreparedStatement dbStat = session.prepareStatement(
					"SELECT STATUS FROM " + DmUtils.getAdminAllViewPrefix(monitor, object.getDataSource(), "OBJECTS")
							+ " WHERE OBJECT_TYPE=? AND OWNER=? AND OBJECT_NAME=?")) {
				dbStat.setString(1, objectType.getTypeName());
				dbStat.setString(2, object.getSchema().getName());
				dbStat.setString(3, DBObjectNameCaseTransformer.transformObjectName(object, object.getName()));
				try (JDBCResultSet dbResult = dbStat.executeQuery()) {
					if (dbResult.next()) {
						return "VALID".equals(dbResult.getString("STATUS"));
					} else {
						log.warn(objectType.getTypeName() + " '" + object.getName()
								+ "' not found in system dictionary");
						return false;
					}
				}
			} catch (SQLException e) {
				throw new DBCException(e, session.getExecutionContext());
			}
		}
	}

	public static String getSysUserViewName(DBRProgressMonitor monitor, DmDataSource dataSource, String viewName) {
		String dbaView = "DBA_" + viewName;
		if (dataSource.isViewAvailable(monitor, DmConstants.SCHEMA_SYS, dbaView)) {
			return DmUtils.getSysSchemaPrefix(dataSource) + dbaView;
		} else {
			return DmUtils.getSysSchemaPrefix(dataSource) + "USER_" + viewName;
		}
	}

	public static <PARENT extends DBSObject> Object resolveLazyReference(DBRProgressMonitor monitor, PARENT parent,
			DBSObjectCache<PARENT, ?> cache, DBSObjectLazy<?> referrer, Object propertyId) throws DBException {
		final Object reference = referrer.getLazyReference(propertyId);
		if (reference instanceof String) {
			Object object;
			if (monitor != null) {
				object = cache.getObject(monitor, parent, (String) reference);
			} else {
				object = cache.getCachedObject((String) reference);
			}
			if (object != null) {
				return object;
			} else {
				log.warn("Object '" + reference + "' not found");
				return reference;
			}
		} else {
			return reference;
		}
	}

	public static String normalizeSourceName(DmDataType object, boolean body) {
		try {
			String source = body ? ((DBPScriptObjectExt) object).getExtendedDefinitionText(null)
					: object.getObjectDefinitionText(null, DBPScriptObject.EMPTY_OPTIONS);
			if (source == null) {
				return null;
			}
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
					object.getSourceType() + (body ? "\\s+BODY" : "") + "\\s(\\s*)([\\w$\\.]+)[\\s\\(]+",
					java.util.regex.Pattern.CASE_INSENSITIVE);
			final Matcher matcher = pattern.matcher(source);
			if (matcher.find()) {
				String objectName = matcher.group(2);
				if (objectName.indexOf('.') == -1) {
					if (!objectName.equalsIgnoreCase(object.getName())) {
						object.setName(DBObjectNameCaseTransformer.transformObjectName(object, objectName));
						object.getDataSource().getContainer()
								.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object));
					}
					return source;
				}
			}
			return source.trim();
		} catch (DBException e) {
			log.error(e);
			return null;
		}
	}

	public static String normalizeSourceName(DmSourceObject object, boolean body) {
		try {
			String source = body ? ((DBPScriptObjectExt) object).getExtendedDefinitionText(null)
					: object.getObjectDefinitionText(null, DBPScriptObject.EMPTY_OPTIONS);
			if (source == null) {
				return null;
			}
			Pattern pattern = Pattern.compile(
					object.getSourceType() + (body ? "\\s+BODY" : "") + "\\s(\\s*)([\\w$\\.]+)[\\s\\(]+",
					Pattern.CASE_INSENSITIVE);
			final Matcher matcher = pattern.matcher(source);
			if (matcher.find()) {
				String objectName = matcher.group(2);
				if (objectName.indexOf('.') == -1) {
					if (!objectName.equalsIgnoreCase(object.getName())) {
						object.setName(DBObjectNameCaseTransformer.transformObjectName(object, objectName));
						object.getDataSource().getContainer()
								.fireEvent(new DBPEvent(DBPEvent.Action.OBJECT_UPDATE, object));
					}
					return source;
				}
			}
			return source.trim();
		} catch (Exception e) {
			log.error(e);
			return null;
		}
	}

	public static void addSchemaChangeActions(DBCExecutionContext executionContext, List<DBEPersistAction> actions,
			DmSourceObject object) {
		DmSchema schema = object.getSchema();
		if (schema == null) {
			return;
		}
		actions.add(0, new SQLDatabasePersistAction("Set target schema",
				"ALTER SESSION SET CURRENT_SCHEMA=" + schema.getName(), DBEPersistAction.ActionType.INITIALIZER));
		/**
		 * 向下转型即将父类类型引用变量转换为子类类型引用变量（由引用类型变量具体指向的实际变量决定，实际指向类型必须是目标类型对象或者其子类实现类对象，此处即必须是
		 * DmExecutionContext 类型对象或其子类对象）,
		 */
		try {
			DmSchema defaultSchema = ((DmExecutionContext) executionContext).getDefaultSchema();
			if (schema != defaultSchema && defaultSchema != null) {
				actions.add(new SQLDatabasePersistAction("Set current schema",
						"ALTER SESSION SET CURRENT_SCHEMA=" + defaultSchema.getName(),
						DBEPersistAction.ActionType.FINALIZER));
			}
		} catch (Exception e) {
			// TODO: handle exception
		 log.error(e.getMessage());
		}
	}

	/**
	 * From 1.2.4 不在运行该代码
             切换活动Schema
	 */
	public static void setCurrentSchema(JDBCSession session, String schema) throws SQLException {
		/*JDBCUtils.executeSQL(session,
				"ALTER SESSION SET CURRENT_SCHEMA=" + DBUtils.getQuotedIdentifier(session.getDataSource(), schema));*/		
	}

	public static String getCurrentSchema(JDBCSession session) throws SQLException {
		return JDBCUtils.queryString(session, "SELECT SYS_CONTEXT( 'USERENV', 'CURRENT_SCHEMA' ) FROM DUAL");
	}

	public static String formatWord(String word) {
		if (word == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(word.length());
		sb.append(Character.toUpperCase(word.charAt(0)));
		for (int i = 1; i < word.length(); i++) {
			char c = word.charAt(i);
			if ((c == 'i' || c == 'I') && sb.charAt(i - 1) == 'I') {
				sb.append('I');
			} else {
				sb.append(Character.toLowerCase(c));
			}
		}
		return sb.toString();
	}
}
