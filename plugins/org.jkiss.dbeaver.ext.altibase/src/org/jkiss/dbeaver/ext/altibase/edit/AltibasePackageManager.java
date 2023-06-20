package org.jkiss.dbeaver.ext.altibase.edit;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.edit.GenericProcedureManager;
import org.jkiss.dbeaver.ext.generic.model.GenericPackage;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericSynonym;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;

public class AltibasePackageManager  extends SQLObjectEditor<GenericPackage, GenericStructContainer> {
    
    @Override
    public boolean canCreateObject(Object container) {
        return false;
    }

    @Override
    public boolean canDeleteObject(GenericPackage object) {
        return true;
    }
    
    @Override
    protected void addObjectDeleteActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, 
            List<DBEPersistAction> actions, ObjectDeleteCommand command, Map<String, Object> options)
    {
        GenericPackage object = command.getObject();
        String procedureName;
        procedureName = object.getFullyQualifiedName(DBPEvaluationContext.DDL);

        actions.add(
            new SQLDatabasePersistAction(
                ModelMessages.model_jdbc_drop_table,
                "DROP PACKAGE" + " " + procedureName)
        );
    }

    @Override
    public long getMakerOptions(DBPDataSource dataSource) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public DBSObjectCache<? extends DBSObject, GenericPackage> getObjectsCache(GenericPackage object) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected GenericPackage createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context,
            Object container, Object copyFrom, Map<String, Object> options) throws DBException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void addObjectCreateActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext,
            List<DBEPersistAction> actions,
            SQLObjectEditor<GenericPackage, GenericStructContainer>.ObjectCreateCommand command,
            Map<String, Object> options) throws DBException {
        // TODO Auto-generated method stub
        
    }
}
