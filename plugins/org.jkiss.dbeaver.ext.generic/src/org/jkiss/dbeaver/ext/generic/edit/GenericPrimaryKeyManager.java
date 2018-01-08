/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ext.generic.edit;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.sql.edit.struct.SQLConstraintManager;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.editors.object.struct.EditConstraintPage;

/**
 * Generic constraint manager
 */
public class GenericPrimaryKeyManager extends SQLConstraintManager<GenericPrimaryKey, GenericTable> {

    @Nullable
    @Override
    public DBSObjectCache<? extends DBSObject, GenericPrimaryKey> getObjectsCache(GenericPrimaryKey object)
    {
        return object.getParentObject().getContainer().getPrimaryKeysCache();
    }

    @Override
    protected GenericPrimaryKey createDatabaseObject(
        DBRProgressMonitor monitor, DBECommandContext context, final GenericTable parent,
        Object from)
    {
        return new UITask<GenericPrimaryKey>() {
            @Override
            protected GenericPrimaryKey runTask() {
                EditConstraintPage editPage = new EditConstraintPage(
                    "Create constraint",
                    parent,
                    new DBSEntityConstraintType[] {DBSEntityConstraintType.PRIMARY_KEY, DBSEntityConstraintType.UNIQUE_KEY} );
                if (!editPage.edit()) {
                    return null;
                }

                final GenericPrimaryKey primaryKey = new GenericPrimaryKey(
                    parent,
                    null,
                    null,
                    editPage.getConstraintType(),
                    false);
                primaryKey.setName(editPage.getConstraintName());
                int colIndex = 1;
                for (DBSEntityAttribute tableColumn : editPage.getSelectedAttributes()) {
                    primaryKey.addColumn(
                        new GenericTableConstraintColumn(
                            primaryKey,
                            (GenericTableColumn) tableColumn,
                            colIndex++));
                }
                return primaryKey;
            }
        }.execute();
    }

    @Override
    protected boolean isLegacyConstraintsSyntax(GenericTable owner) {
        return GenericUtils.isLegacySQLDialect(owner);
    }
}
