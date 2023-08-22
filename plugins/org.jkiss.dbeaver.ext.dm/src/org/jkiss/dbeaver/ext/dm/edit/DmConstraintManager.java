package org.jkiss.dbeaver.ext.dm.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.DmObjectStatus;
import org.jkiss.dbeaver.ext.dm.model.DmTableBase;
import org.jkiss.dbeaver.ext.dm.model.DmTableConstraint;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

/**
 * DM Constraint Manager
 * @author caosw
 *
 */
public class DmConstraintManager extends SQLConstraintManager<DmTableConstraint, DmTableBase> {

	@Nullable
	@Override
	public DBSObjectCache<? extends DBSObject, DmTableConstraint> getObjectsCache(DmTableConstraint object) {
		return object.getParentObject().getSchema().constraintCache;
	}

	@Override
	protected DmTableConstraint createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
			Object container, Object copyFrom, Map<String, Object> options) throws DBException {
		DmTableBase table = (DmTableBase) container;
		return new DmTableConstraint(table, "", DBSEntityConstraintType.UNIQUE_KEY, null, DmObjectStatus.ENABLED);
	}


	//增加check 追加定义，其余仍然用默认的即可
    @Override
	protected void appendConstraintDefinition(StringBuilder decl, DBECommandAbstract<DmTableConstraint> command) {
        if (command.getObject().getConstraintType() == DBSEntityConstraintType.CHECK) {
            decl.append(" (").append((command.getObject()).getSearchCondition()).append(")");
        } else {
            super.appendConstraintDefinition(decl, command);
        }
	}

	
	@Override
	protected String getDropConstraintPattern(DmTableConstraint constraint) {
		String clause = "CONSTRAINT";
		return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP " + clause + " " + PATTERN_ITEM_CONSTRAINT;
	}	
}
