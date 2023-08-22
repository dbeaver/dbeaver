package org.jkiss.dbeaver.ext.yashandb.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.yashandb.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

import java.util.List;
import java.util.Map;

public class YashanDBConstraintManager extends SQLConstraintManager<YashanDBTableConstraint, YashanDBTableBase> {
    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, YashanDBTableConstraint> getObjectsCache(YashanDBTableConstraint object) {
        return object.getParentObject().getSchema().constraintCache;
    }

    @Override
    protected YashanDBTableConstraint createDatabaseObject(
            DBRProgressMonitor monitor, DBECommandContext context, final Object container,
            Object from, Map<String, Object> options) {
        YashanDBTableBase table = (YashanDBTableBase) container;

        return new YashanDBTableConstraint(
                table,
                "",
                DBSEntityConstraintType.UNIQUE_KEY,
                null,
                YashanDBObjectStatus.ENABLED);
    }

    @Override
    protected String getDropConstraintPattern(YashanDBTableConstraint constraint) {
        String clause = "CONSTRAINT";

        String tableType = constraint.getTable().isView() ? "VIEW" : "TABLE";

        return "ALTER " + tableType + " " + PATTERN_ITEM_TABLE + " DROP " + clause + " " + PATTERN_ITEM_CONSTRAINT;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions,
                                          ObjectCreateCommand command, Map<String, Object> options) {
        YashanDBTableConstraint constraint = command.getObject();
        boolean isView = constraint.getTable().isView();
        String tableType = isView ? "VIEW" : "TABLE";
        YashanDBTableBase table = constraint.getTable();
        actions.add(
                new SQLDatabasePersistAction(
                        ModelMessages.model_jdbc_create_new_constraint,
                        "ALTER " + tableType + " " + table.getFullyQualifiedName(DBPEvaluationContext.DDL) +
                                "\nADD " + getNestedDeclaration(monitor, table, command, options) +
                                (isView ? " NOVALIDATE" : "")
                ));
    }

    @Override
    protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<YashanDBTableConstraint> command) {
        if (command.getObject().getConstraintType() == DBSEntityConstraintType.CHECK) {
            decl.append(" (").append((command.getObject()).getSearchCondition()).append(")");
        } else {
            super.appendConstraintDefinition(decl, command);
        }
    }
}
