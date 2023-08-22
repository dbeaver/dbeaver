package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDBLink;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSequence;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/20 10:14
 */
public class YashanDBDBLinkManager extends SQLObjectEditor<YashanDBDBLink, YashanDBSchema> {
    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Dblink name cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getUserName())) {
            throw new DBException("Username  cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getPassword())) {
            throw new DBException("Password cannot be empty");
        }
        if (CommonUtils.isEmpty(command.getObject().getHost())) {
            throw new DBException("Host cannot be empty");
        }
    }

    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBDBLink> getObjectsCache(YashanDBDBLink object) {
        return object.getDataSource().dbLinkCache;
    }

    @Override
    protected YashanDBDBLink createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        YashanDBDataSource dataSource = (YashanDBDataSource) container;
        return new YashanDBDBLink(dataSource,"NEW_DBLINK");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<YashanDBDBLink, YashanDBSchema>.ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        String sql = command.getObject().buildStatement(false);
        actions.add(new SQLDatabasePersistAction("Create DBLink", sql));
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {
        String sql = command.getObject().buildStatement(true);
        actionList.add(new SQLDatabasePersistAction("Alter DBLink", sql));

    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<YashanDBDBLink, YashanDBSchema>.ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        String sql = "DROP DATABASE LINK " + command.getObject().getName();
        DBEPersistAction action = new SQLDatabasePersistAction("Drop DBLink", sql);
        actions.add(action);
    }
}
