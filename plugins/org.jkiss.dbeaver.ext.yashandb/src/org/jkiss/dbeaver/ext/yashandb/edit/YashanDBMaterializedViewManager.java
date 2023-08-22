package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBDDLFormat;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBMaterializedView;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBSchema;
import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableColumn;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.edit.prop.DBECommandComposite;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/6 19:05
 */
public class YashanDBMaterializedViewManager extends SQLObjectEditor<YashanDBMaterializedView, YashanDBSchema> {

    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_EDITOR_ON_CREATE;
    }

    @Override
    protected void validateObjectProperties(DBRProgressMonitor monitor, ObjectChangeCommand command, Map<String, Object> options)
            throws DBException
    {
        if (CommonUtils.isEmpty(command.getObject().getName())) {
            throw new DBException("View name cannot be empty"); //$NON-NLS-1$
        }
    }

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBMaterializedView> getObjectsCache(YashanDBMaterializedView object)
    {
        return (DBSObjectCache) object.getSchema().tableCache;
    }

    @Override
    protected YashanDBMaterializedView createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options)
    {
        YashanDBSchema schema = (YashanDBSchema) container;
        YashanDBMaterializedView newView = new YashanDBMaterializedView(schema, "NEW_MVIEW"); //$NON-NLS-1$
        setNewObjectName(monitor, schema, newView);
        newView.setObjectDefinitionText("SELECT 1 AS A FROM DUAL");
        newView.setCurrentDDLFormat(YashanDBDDLFormat.COMPACT);
        return newView;
    }

    @Override
    protected String getBaseObjectName() {
        return SQLTableManager.BASE_MATERIALIZED_VIEW_NAME;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectCreateCommand command, Map<String, Object> options) throws DBException {
        createOrReplaceViewQuery(monitor, actions, command, options);
    }

    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actionList, ObjectChangeCommand command, Map<String, Object> options) throws DBException {
        createOrReplaceViewQuery(monitor, actionList, command, options);
    }

    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        actions.add(
                new SQLDatabasePersistAction("Drop view", "DROP MATERIALIZED VIEW " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL)) //$NON-NLS-2$
        );
    }

    private void createOrReplaceViewQuery(DBRProgressMonitor monitor, List<DBEPersistAction> actions, DBECommandComposite<YashanDBMaterializedView, PropertyHandler> command, Map<String, Object> options) throws DBException {
        YashanDBMaterializedView view = command.getObject();

        StringBuilder decl = new StringBuilder(200);
        final String lineSeparator = GeneralUtils.getDefaultLineSeparator();
        boolean hasComment = command.hasProperty("comment");
        if (!hasComment || command.getProperties().size() > 1) {
            String mViewDefinition = view.getMViewText().trim();
            if (mViewDefinition.contains("CREATE MATERIALIZED VIEW")) {
                if (mViewDefinition.endsWith(";")) mViewDefinition = mViewDefinition.substring(0, mViewDefinition.length() - 1);
                decl.append(mViewDefinition);
            } else {
                decl.append("CREATE MATERIALIZED VIEW ").append(view.getFullyQualifiedName(DBPEvaluationContext.DDL)).append(lineSeparator) //$NON-NLS-1$
                        .append("AS ").append(mViewDefinition); //$NON-NLS-1$
            }
            if (view.isPersisted()) {
                actions.add(
                        new SQLDatabasePersistAction("Drop view", "DROP MATERIALIZED VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL))); //$NON-NLS-2$
            }
            List<SQLScriptElement> sqlScriptElements = SQLScriptParser.parseScript(view.getDataSource(), mViewDefinition);
            if (sqlScriptElements.size() > 1) {
                // In this case we already have and view definition, and view/columns comments
                for (SQLScriptElement scriptElement : sqlScriptElements) {
                    actions.add(new SQLDatabasePersistAction("Create view part", scriptElement.getText()));
                }
                return;
            }
            actions.add(
                    new SQLDatabasePersistAction("Create view", decl.toString()));
        }

//        if (hasComment || (CommonUtils.getOption(options, DBPScriptObject.OPTION_OBJECT_SAVE) && CommonUtils.isNotEmpty(view.getComment()))) {
//            actions.add(new SQLDatabasePersistAction(
//                    "Comment view",
//                    "COMMENT ON MATERIALIZED VIEW " + view.getFullyQualifiedName(DBPEvaluationContext.DDL) +
//                            " IS " + SQLUtils.quoteString(view.getDataSource(), CommonUtils.notEmpty(view.getComment()))));
//        }

        if (!(hasComment && command.getProperties().size() == 1) && CommonUtils.getOption(options, DBPScriptObject.OPTION_OBJECT_SAVE)) { // Add column comments only in save case
            // Column comments for the newly created table
            for (YashanDBTableColumn column : CommonUtils.safeCollection(view.getAttributes(monitor))) {
                if (!CommonUtils.isEmpty(column.getComment(monitor))) {
                    YashanDBTableColumnManager.addColumnCommentAction(actions, column, view);
                }
            }
        }
    }

}


