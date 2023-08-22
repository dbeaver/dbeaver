package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmDataFile;
import org.jkiss.dbeaver.ext.dm.model.DmDataSource;
import org.jkiss.dbeaver.ext.dm.model.DmTablespace;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

/**
 * 表空间manager
 * @author saorionesan
 *
 */
public class DmTableSpaceManager extends SQLObjectEditor<DmTablespace, DmDataSource>{

	@Override
	public long getMakerOptions(DBPDataSource dataSource) {
		
		return FEATURE_EDITOR_ON_CREATE;
	}

	@Override
	public DBSObjectCache<? extends DBSObject, DmTablespace> getObjectsCache(DmTablespace object) {
		
		return object.getDataSource().tablespaceCache;
	}

	@Override
	protected DmTablespace createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container,
			Object copyFrom, Map<String, Object> options) throws DBException {

		return new DmTablespace((DmDataSource)container, "DmNewTableSpace",-1);
	}

	@Override
	protected void validateObjectProperties(DBRProgressMonitor monitor,
			SQLObjectEditor<DmTablespace, DmDataSource>.ObjectChangeCommand command, Map<String, Object> options)
			throws DBException {
		validateName(command.getObject());
		super.validateObjectProperties(monitor, command, options);
	}

	
    private void validateName(DmTablespace tablespace) throws DBException {
        if (tablespace.getName() == null || tablespace.getName().trim().isEmpty()) {
            throw new DBException("表空间名称不能为空");
        }
    }
	
	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmTablespace, DmDataSource>.ObjectCreateCommand command,
			Map<String, Object> options) throws DBException {
        DmTablespace tablespace = (DmTablespace)command.getObject();
        StringBuffer sql = new StringBuffer("CREATE TABLESPACE " + tablespace.getName().toUpperCase());
        DmDataFile dataFile = tablespace.getDataFile();
        boolean autoExtend = dataFile.isAutoExtensible();
        sql.append(" DATAFILE '" + dataFile.getFile() + "' ");
        if(CommonUtils.isNotEmpty(dataFile.getMirrorPath())) {
         sql.append("MIRROR '"+dataFile.getMirrorPath()+"' ");
        }
        sql.append(" SIZE "+dataFile.getTotalSize()).append(" AUTOEXTEND " + (autoExtend ? "ON" : "OFF"));
        if (autoExtend) {
            sql.append(" NEXT "+dataFile.getNextExtSize());
            sql.append(" MAXSIZE "+dataFile.getMaxExtSize());
        }
        sql.append(" CACHE = \""+tablespace.getCache()+"\"");
        actions.add(new SQLDatabasePersistAction("Create TABLESPACE", sql.toString()));
	}

	
	
	@Override //修改表空间   
	protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actionList, SQLObjectEditor<DmTablespace, DmDataSource>.ObjectChangeCommand command,
			Map<String, Object> options) throws DBException {
        DmTablespace tablespace = (DmTablespace)command.getObject();
        StringBuffer sql = new StringBuffer();
        sql.append("ALTER TABLESPACE \"" + tablespace.getName()+"\"");
        Map<Object, Object> properties = command.getProperties();
        if (properties.get("cache") != null) {
            sql.append(" CACHE =\""+tablespace.getCache()+"\"");
        }

        actionList.add(new SQLDatabasePersistAction("ALTER TABLESPACE ", sql.toString()));		
	}

	@Override
	protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
			List<DBEPersistAction> actions, SQLObjectEditor<DmTablespace, DmDataSource>.ObjectDeleteCommand command,
			Map<String, Object> options) throws DBException {
		actions.add(new SQLDatabasePersistAction("Drop Tablespace", "DROP TABLESPACE " + DBUtils.getQuotedIdentifier(command.getObject())));
	}

}
