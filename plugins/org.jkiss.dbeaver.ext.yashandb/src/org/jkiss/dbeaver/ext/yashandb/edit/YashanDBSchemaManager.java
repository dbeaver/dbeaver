package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDataSource;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBUser;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBUtils;
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

import java.util.List;
import java.util.Map;

/**
 * @Author: donghy
 * @Date: 2022/08
 * @Description:
 */
public class YashanDBSchemaManager extends SQLObjectEditor<YashanDBSchema, YashanDBDataSource> {
    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        return FEATURE_SAVE_IMMEDIATELY;
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBSchema> getObjectsCache(YashanDBSchema object) {
        return object.getDataSource().schemaCache;
    }

    @Override
    protected YashanDBSchema createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, final Object container, Object copyFrom, Map<String, Object> options) {
        return new YashanDBSchema((YashanDBDataSource) container, -1, "NEW_SCHEMA");
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) {
        YashanDBUser user = command.getObject().getUser();
        String sql = "CREATE USER " + DBUtils.getQuotedIdentifier(user);
        if (!CommonUtils.isEmpty(user.getPassword())) {
            sql += " IDENTIFIED BY \"" + user.getPassword() + "\"";
        }

        actions.add(new SQLDatabasePersistAction("Create schema", sql));
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options) {
        /** it will not add double quotes when obtaining the schema name When schema object created with double quotes,
         * so the deletion will fail when schema name contains lowercase characters.*/
        actions.add(
                new SQLDatabasePersistAction("Drop schema",
                        "DROP USER " + DBUtils.getQuotedIdentifier(command.getObject()) + " CASCADE") //$NON-NLS-2$
        );
    }
}
