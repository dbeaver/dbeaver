package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.*;
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
 * @Date 2023/7/8 18:00
 */
public class YashanDBUserProfileManager extends SQLObjectEditor<YashanDBUserProfile, YashanDBSchema> {

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("Profile name cannot be empty");
        }
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBUserProfile> getObjectsCache(YashanDBUserProfile object) {
        return object.getDataSource().profileCache;
    }

    @Override
    protected YashanDBUserProfile createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        YashanDBDataSource dataSource = (YashanDBDataSource) container;
        return new YashanDBUserProfile(dataSource, "NEW_PROFILE");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<YashanDBUserProfile, YashanDBSchema>.ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        YashanDBUserProfile profile = command.getObject();
        actions.add(
                new SQLDatabasePersistAction("Add Profile", profile.buildStatement(false))
        );
    }

    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) {{
        YashanDBUserProfile profile = command.getObject();
        actionList.add(
                new SQLDatabasePersistAction("Modify Profile", profile.buildStatement(true))
        );
    }}


        @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, SQLObjectEditor<YashanDBUserProfile, YashanDBSchema>.ObjectDeleteCommand command, Map<String, Object> options) throws DBException {
        actions.add(
                new SQLDatabasePersistAction("Drop profile", "DROP PROFILE " + command.getObject().getName() + " CASCADE") //$NON-NLS-2$
        );
    }
}
