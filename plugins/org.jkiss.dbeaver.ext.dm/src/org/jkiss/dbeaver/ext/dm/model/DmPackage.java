package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObjectExt;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSPackage;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;

public class DmPackage extends DmSchemaObject implements DmSourceObject, DBPScriptObjectExt, DBSObjectContainer,
		DBSPackage, DBPRefreshableObject, DBSProcedureContainer {

	private static final Log log = Log.getLog(DmPackage.class);

	private boolean valid;
	private String sourceDeclaration;
	private String sourceDefinition;

	public DmPackage(DmSchema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "OBJECT_NAME"), true);
		this.valid = "VALID".equals(JDBCUtils.safeGetString(dbResult, "STATUS"));
	}

	public DmPackage(DmSchema schema, String name) {
		super(schema, name, false);
	}

	@Property(viewable = true, order = 3)
	public boolean isValid() {
		return valid;
	}

	@Override
	public DmSourceType getSourceType() {
		return DmSourceType.PACKAGE;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBCException {
		if (sourceDeclaration == null && monitor != null) {
			sourceDeclaration = DmUtils.getSource(monitor, this, false, true);
		}
		return sourceDeclaration;
	}

	public void setObjectDefinitionText(String sourceDeclaration) {
		this.sourceDeclaration = sourceDeclaration;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getExtendedDefinitionText(DBRProgressMonitor monitor) throws DBException {
		if (sourceDefinition == null && monitor != null) {
			sourceDefinition = DmUtils.getSource(monitor, this, true, true);
		}
		return sourceDefinition;
	}

	public void setExtendedDefinitionText(String source) {
		this.sourceDefinition = source;
	}
	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	@Override
	public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException {
		this.valid = DmUtils.getObjectStatus(monitor, this, DmObjectType.PACKAGE) && 
				DmUtils.getObjectStatus(monitor, this, DmObjectType.PACKAGE_BODY);
	}

	@Override
	public Collection<? extends DBSProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DBSProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Class<? extends DBSObject> getPrimaryChildType(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}
}
