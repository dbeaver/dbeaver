/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.*;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLIndexManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * MySQL index manager
 */
public class MySQLIndexManager extends SQLIndexManager<MySQLTableIndex, MySQLTable> implements DBEObjectRenamer<MySQLTableIndex> {

    @Nullable
    @Override
    public DBSObjectCache<MySQLCatalog, MySQLTableIndex> getObjectsCache(MySQLTableIndex object)
    {
        return object.getTable().getContainer().getIndexCache();
    }

    @Override
    protected MySQLTableIndex createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final Object container,
        Object from, Map<String, Object> options)
    {
        return new MySQLTableIndex(
            (MySQLTable) container,
            false,
            null,
            DBSIndexType.OTHER,
            null,
            false);
    }

    @Override
    protected String getDropIndexPattern(MySQLTableIndex index)
    {
        return "ALTER TABLE " + PATTERN_ITEM_TABLE + " DROP INDEX " + PATTERN_ITEM_INDEX_SHORT; //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected void appendIndexType(MySQLTableIndex index, StringBuilder decl) {
        DBSIndexType indexType = index.getIndexType();
        if (indexType != MySQLConstants.INDEX_TYPE_FULLTEXT) {
            decl.append(" USING ").append(indexType.getId());
        }
    }

    protected void appendIndexModifiers(MySQLTableIndex index, StringBuilder decl) {
        if (index.getIndexType() == MySQLConstants.INDEX_TYPE_FULLTEXT) {
            decl.append(" FULLTEXT");
        } else {
            super.appendIndexModifiers(index, decl);
        }
    }

    protected void appendIndexColumnModifiers(DBRProgressMonitor monitor, StringBuilder decl, DBSTableIndexColumn indexColumn) {
        final String subPart = ((MySQLTableIndexColumn) indexColumn).getSubPart();
        if (!CommonUtils.isEmpty(subPart)) {
            decl.append(" (").append(subPart).append(")");
        }
        if (!indexColumn.isAscending()) {
            decl.append(" DESC"); //$NON-NLS-1$
        }
    }

    @Override
    public void renameObject(DBECommandContext commandContext, MySQLTableIndex object, String newName) throws DBException {
        processObjectRename(commandContext, object, newName);
    }

    @Override
    protected void addObjectRenameActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, ObjectRenameCommand command, Map<String, Object> options)
    {
        final MySQLDataSource dataSource = command.getObject().getDataSource();
        actions.add(
            new SQLDatabasePersistAction(
                "Rename table",
                "ALTER TABLE " + command.getObject().getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                    "\nRENAME INDEX " + DBUtils.getQuotedIdentifier(dataSource, command.getOldName()) +
                    " TO " + DBUtils.getQuotedIdentifier(dataSource, command.getNewName())) //$NON-NLS-1$
        );
    }


}
