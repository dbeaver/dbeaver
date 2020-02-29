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
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLTableManager;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.cache.DBSObjectCache;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Generic table manager
 */
public class GenericTableManager extends SQLTableManager<GenericTableBase, GenericStructContainer> {

    private static final Class<?>[] CHILD_TYPES = {
        GenericTableColumn.class,
        GenericUniqueKey.class,
        GenericTableForeignKey.class,
        GenericTableIndex.class
    };

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericTableBase> getObjectsCache(GenericTableBase object)
    {
        return object.getContainer().getTableCache();
    }

    @NotNull
    @Override
    public Class<?>[] getChildTypes()
    {
        return CHILD_TYPES;
    }

    @Override
    protected GenericTableBase createDatabaseObject(DBRProgressMonitor monitor, DBECommandContext context, Object container, Object copyFrom, Map<String, Object> options)
    {
        GenericStructContainer structContainer = (GenericStructContainer) container;

        boolean isView = false;
        Object navContainer = options.get(DBEObjectMaker.OPTION_CONTAINER);
        if (navContainer instanceof DBNDatabaseFolder) {
            List<DBXTreeNode> folderChildren = ((DBNDatabaseFolder) navContainer).getMeta().getChildren((DBNNode) navContainer);
            if (folderChildren.size() == 1 && folderChildren.get(0) instanceof DBXTreeItem && ((DBXTreeItem) folderChildren.get(0)).getPropertyName().equals("views")) {
                isView = true;
            }
        }
        String tableName = getNewChildName(monitor, structContainer, isView ? BASE_VIEW_NAME : BASE_TABLE_NAME);
        return structContainer.getDataSource().getMetaModel().createTableImpl(structContainer, tableName,
            isView ? GenericConstants.TABLE_TYPE_VIEW : GenericConstants.TABLE_TYPE_TABLE,
            null);
    }

    @Override
    protected boolean excludeFromDDL(NestedObjectCommand command, Collection<NestedObjectCommand> orderedCommands) {
        // Filter out indexes for unique constraints (if they have the same name)
        DBPObject object = command.getObject();
        if (object instanceof DBSTableIndex) {
            for (NestedObjectCommand ccom : orderedCommands) {
                if (ccom.getObject() instanceof DBSEntityConstraint &&
                    ccom.getObject() != object &&
                    ((DBSEntityConstraint) ccom.getObject()).getConstraintType().isUnique() &&
                    CommonUtils.equalObjects(
                        ((DBSTableIndex) object).getName(), ((DBSEntityConstraint) ccom.getObject()).getName()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void addObjectExtraActions(DBRProgressMonitor monitor, DBCExecutionContext executionContext, List<DBEPersistAction> actions, NestedObjectCommand<GenericTableBase, PropertyHandler> command, Map<String, Object> options) {
        if (command.getProperty(DBConstants.PROP_ID_DESCRIPTION) != null) {
            actions.add(new SQLDatabasePersistAction(
                    "Comment table",
                    "COMMENT ON TABLE " + command.getObject().getFullyQualifiedName(DBPEvaluationContext.DDL) +
                            " IS " + SQLUtils.quoteString(command.getObject(), command.getObject().getDescription())));
        }
    }

}
