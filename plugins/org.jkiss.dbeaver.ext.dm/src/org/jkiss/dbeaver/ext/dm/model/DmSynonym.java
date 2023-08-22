package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DM synonym
 * 
 * @author caosw
 *
 */
public class DmSynonym extends DmSchemaObject implements DBSAlias {

	private String objectOwner;
	private String objectTypeName;
	private String objectName;
	private String dbLink;

	public DmSynonym(DmSchema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "SYNONYM_NAME"), true);
		this.objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
		this.objectOwner = JDBCUtils.safeGetString(dbResult, "TABLE_OWNER");
		this.objectName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
		this.dbLink = JDBCUtils.safeGetString(dbResult, "DB_LINK");
	}

	public DmObjectType getObjectType() {
		return DmObjectType.getByType(objectTypeName);
	}

	@NotNull
	@Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
	public String getName() {
		return super.getName();
	}

	@Property(viewable = true, order = 2)
	public String getObjectTypeName() {
		return objectTypeName;
	}

	@Property(viewable = true, order = 3)
	public Object getObjectOwner() {
		final DmSchema schema = getDataSource().schemaCache.getCachedObject(objectOwner);
		return schema == null ? objectOwner : schema;
	}
	
	@Property(viewable = true, linkPossible = true, order = 4)
	public Object getObject(DBRProgressMonitor monitor) throws DBException {
		if(objectTypeName == null) {
			return null;
		}
		return DmObjectType.resolveObject(monitor, getDataSource(), dbLink, objectTypeName, objectOwner, objectName);
	}
	
	@Property(viewable = true, order = 5)
	public Object getDbLink(DBRProgressMonitor monitor) throws DBException {
		return DmDBLink.resolveObject(monitor, getSchema(), dbLink);
	}

	@Override
	public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
		Object object = getObject(monitor);
		if (object instanceof DBSObject) {
			return (DBSObject) object;
		}
		return null;
	}

	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		if(DmConstants.USER_PUBLIC.equals(getSchema().getName())) {
			return DBUtils.getQuotedIdentifier(this);
		}
		return super.getFullyQualifiedName(context);
	}

}
