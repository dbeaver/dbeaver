/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.exasol.manager;

import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.model.security.ExasolSecurityPolicy;
import org.jkiss.dbeaver.ext.exasol.model.ExasolDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;


public class ExasolSecurityPolicyManager
        extends SQLObjectEditor<ExasolSecurityPolicy, ExasolDataSource>  {
    
    @Override
    public long getMakerOptions(DBPDataSource dataSource)
    {
        return FEATURE_SAVE_IMMEDIATELY;
    }
    
    
    @Override
    public DBSObjectCache<ExasolDataSource, ExasolSecurityPolicy> getObjectsCache(
            ExasolSecurityPolicy object)
    {
        ExasolDataSource source = (ExasolDataSource) object.getDataSource();
        return source.getSecurityPolicyCache();
    }
    
    @Override
    protected ExasolSecurityPolicy createDatabaseObject(DBRProgressMonitor monitor,
            DBECommandContext context, ExasolDataSource parent, Object copyFrom)
            throws DBException
    {
    	throw new DBException("Not Supported");
    }
    
    
    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions,
                                          ObjectRenameCommand command, Map<String, Object> options)
    {
        ExasolSecurityPolicy obj = command.getObject();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename Connection",
                "RENAME CONNECTION " +  DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getOldName()) + " to " +
                    DBUtils.getQuotedIdentifier(obj.getDataSource(), command.getNewName()))
        );
    }
    
    @Override
    protected void addObjectDeleteActions(List<DBEPersistAction> actions,
                                          ObjectDeleteCommand command, Map<String, Object> options)
    {
        final ExasolSecurityPolicy con = command.getObject();
        actions.add(
                new SQLDatabasePersistAction("Drop Connection","DROP CONNECTION " + DBUtils.getQuotedIdentifier(con))
                );
    }
    
    @Override
    protected void addObjectModifyActions(DBRProgressMonitor monitor, List<DBEPersistAction> actionList,
                                          ObjectChangeCommand command, Map<String, Object> options)
    {
    	
    	ExasolSecurityPolicy policy = command.getObject();
    	
    	if (policy.getEnabled())
    	{
    		String script = String.format("ALTER SYSTEM SET PASSWORD_SECURITY_POLICY='%s'", policy.getSecurityString());
    		actionList.add(new SQLDatabasePersistAction(script));
    	} else {
    		String script = String.format("ALTER SYSTEM SET PASSWORD_SECURITY_POLICY='OFF'", policy.getSecurityString());
    		actionList.add(new SQLDatabasePersistAction(script));
    	}

    }
    
    @Override
    public boolean canDeleteObject(ExasolSecurityPolicy object) {
    	return false;
    }

    @Override
    public boolean canCreateObject(ExasolDataSource parent) {
    	return false;
    }


	@Override
	protected void addObjectCreateActions(DBRProgressMonitor monitor, List<DBEPersistAction> actions,
			SQLObjectEditor<ExasolSecurityPolicy, ExasolDataSource>.ObjectCreateCommand command,
			Map<String, Object> options) {
	}
    
    
    

}
