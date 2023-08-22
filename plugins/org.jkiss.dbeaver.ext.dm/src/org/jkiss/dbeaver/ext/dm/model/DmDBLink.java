package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.util.Date;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * DB Link
 * 
 * @author caosw
 *
 */
public class DmDBLink extends DmSchemaObject {

	private static final Log log = Log.getLog(DmDBLink.class);

	private String userName;
	private String host;
	private Date created;

	protected DmDBLink(DBRProgressMonitor progressMonitor, DmSchema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "DB_LINK"), true);
		this.userName = JDBCUtils.safeGetString(dbResult, "USERNAME");
		this.host = JDBCUtils.safeGetString(dbResult, "HOST");
		this.created = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
	}

	@Property(viewable = true, editable = true, order = 2)
	public String getUserName() {	
		return userName;
	}

	@Property(viewable = true, editable = true, order = 3)
	public String getHost() {
		return host;
	}

	@Property(viewable = true, order = 4)
	public Date getCreated() {
		return created;
	}

	public static Object resolveObject(DBRProgressMonitor monitor, DmSchema schema, String dbLink) throws DBException {

		if (CommonUtils.isEmpty(dbLink)) {
			return null;
		}
		final DmDBLink object = schema.dbLinkCache.getObject(monitor, schema, dbLink);
		if (object == null) {
			log.warn("DB Link '" + dbLink + "' not found in schema '" + schema.getName() + "'");
			return dbLink;
		}
		return object;
	}
}
