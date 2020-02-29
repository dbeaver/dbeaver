/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.edit;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

/**
 * PostgrePartitionManager
 */
public class PostgrePartitionManager extends PostgreTableManager {

    private static final Log log = Log.getLog(PostgrePartitionManager.class);
    
    private static final Class<?>[] CHILD_TYPES_PART = {
            PostgreTableConstraint.class,
            PostgreTableForeignKey.class,
            PostgreIndex.class
        };
    
    protected PostgreTablePartition createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options) {
        PostgreTable owner = (PostgreTable)container;
        final PostgreTablePartition table = new PostgreTablePartition(owner);
        return table;
    }

    private String getParentTable(PostgreTablePartition partition)  {
        
        List<PostgreTableBase> superTables;
        try {
            superTables = partition.getSuperTables(new VoidProgressMonitor());
        } catch (DBException e) {
            log.error("Unable to get parent",e);
            return "";
        }
        
        if (superTables == null && partition.getPartitionOf() != null) {
            return partition.getPartitionOf().getSchema().getName() + "." + partition.getPartitionOf().getName();            
        } else if (superTables == null || superTables.size() > 1) {
            log.error("Unable to get parent");
            return "";
        } 
        
        //       final String tableName = CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true) ?
        //table.getFullyQualifiedName(DBPEvaluationContext.DDL) : DBUtils.getQuotedIdentifier(table);

        
        return superTables.get(0).getSchema().getName() + "." + superTables.get(0).getName();
    }   
    
    @Override
    protected String beginCreateTableStatement(PostgreTableBase table, String tableName) {
        return "CREATE " + getCreateTableType(table) + " " + tableName + " PARTITION OF " +
                getParentTable((PostgreTablePartition) table) + " ";//$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    protected boolean hasAttrDeclarations() {
        return false;
    }

    @Override
    protected boolean excludeFromDDL(NestedObjectCommand command, Collection<NestedObjectCommand> orderedCommands) {
        return !(command.getObject() instanceof PostgreTableConstraint);
    }

    @Override
    public boolean canEditObject(PostgreTableBase object) {
        return object instanceof PostgreTablePartition;
    }

    @Override
    public boolean canCreateObject(Object container) {
        return container instanceof PostgreTable;
    }

    @Override
    public boolean canDeleteObject(PostgreTableBase object) {
        return true;
    }
    
    @NotNull
    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES_PART;
    }


}
