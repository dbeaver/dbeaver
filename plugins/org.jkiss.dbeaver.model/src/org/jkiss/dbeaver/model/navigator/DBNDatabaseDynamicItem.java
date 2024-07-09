/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBNDatabaseDynamicItem.
 *
 * Dynamic items are not in the main navigator model. They are created dynamically as child nodes for complex
 * database objects references (e.g. table columns struct data type attributes).
 */
public class DBNDatabaseDynamicItem extends DBNDatabaseNode {
    private DBSObject object;

    public DBNDatabaseDynamicItem(DBNDatabaseNode parent, DBSObject object) {
        super(parent);
        this.object = object;
    }

    @Override
    public boolean isDisposed() {
        return object == null || super.isDisposed();
    }

    @Override
    protected void dispose(boolean reflect) {
        this.object = null;
        super.dispose(reflect);
    }

    @Override
    public DBXTreeNode getMeta() {
        return ((DBNDatabaseNode)getParentNode()).getMeta();
    }

    @Override
    protected boolean reloadObject(DBRProgressMonitor monitor, DBSObject newObject) {
        return false;
    }

    @Nullable
    @Override
    public DBSObject getObject() {
        return object;
    }

    @Override
    public Object getValueObject() {
        return object;
    }

    @Override
    public boolean isPersisted() {
        return object != null && object.isPersisted();
    }

    @Override
    public final boolean isManagable() {
        return true;
    }

    @Override
    public String toString() {
        return object == null ? super.toString() : object.toString();
    }
}
