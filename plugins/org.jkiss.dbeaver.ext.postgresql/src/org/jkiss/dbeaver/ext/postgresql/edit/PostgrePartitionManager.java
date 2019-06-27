/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableConstraint;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTablePartition;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Postgre table manager
 */
public class PostgrePartitionManager extends PostgreTableManager {

    private static final Log log = Log.getLog(PostgrePartitionManager.class);
    
    private String getParentTable(PostgreTablePartition partition)  {
        
        List<PostgreTableBase> superTables;
        try {
            superTables = partition.getSuperTables(new VoidProgressMonitor());
        } catch (DBException e) {
            log.error("Unable to get parent",e);
            return "";
        }
        
        if (superTables == null || superTables.size() != 1) {
            log.error("Unable to get parent");
            return "";
        }
        
        //       final String tableName = CommonUtils.getOption(options, DBPScriptObject.OPTION_FULLY_QUALIFIED_NAMES, true) ?
        //table.getFullyQualifiedName(DBPEvaluationContext.DDL) : DBUtils.getQuotedIdentifier(table);

        
        return superTables.get(0).getSchema().getName() + "." + superTables.get(0).getName();
    }   
    
    @Override
    protected boolean hasAttrDeclarations() {
        return false;
    }
    
    @Override
    protected String createVerb(PostgreTableBase table, String tableName) {

        StringBuilder sb = new StringBuilder("CREATE "); //$NON-NLS-1$ //$NON-NLS-2$

        sb.append(getCreateTableType(table)).append(" ").append(tableName).append(" PARTITION OF ")
                .append(getParentTable((PostgreTablePartition) table)).append(" "); //$NON-NLS-1$ //$NON-NLS-2$
        return sb.toString();
    }
    
    @Override
    protected boolean excludeFromDDL(NestedObjectCommand command, Collection<NestedObjectCommand> orderedCommands) {
        return !(command.getObject() instanceof PostgreTableConstraint);
    }

    @Override
    public boolean canEditObject(PostgreTableBase object) {
        return false;
    }

    @Override
    public boolean canCreateObject(PostgreSchema parent) {
        return false;
    }

    @Override
    public boolean canDeleteObject(PostgreTableBase object) {
        return false;
    }

}
