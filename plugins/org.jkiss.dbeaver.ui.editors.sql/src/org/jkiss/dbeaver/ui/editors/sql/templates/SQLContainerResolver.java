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
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.templates.TemplateContext;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Container resolver
 */
public class SQLContainerResolver<T extends DBSObjectContainer> extends SQLObjectResolver<T> {

    private static final Log log = Log.getLog(SQLContainerResolver.class);

    static final String VAR_NAME_SCHEMA = "schema";
    static final String VAR_NAME_CATALOG = "catalog";

    private Class<T> objectType;

    SQLContainerResolver(String id, String title, Class<T> objectType)
    {
        super(id, title);
        this.objectType = objectType;
    }

    @Override
    protected void resolveObjects(DBRProgressMonitor monitor, DBCExecutionContext executionContext, TemplateContext context, List<T> entities) throws DBException
    {
        DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, executionContext.getDataSource());
        if (objectContainer != null) {
            makeProposalsFromChildren(monitor, executionContext, objectContainer, entities);
        }
    }

    private void makeProposalsFromChildren(DBRProgressMonitor monitor, DBCExecutionContext executionContext, DBSObjectContainer container, List<T> names) throws DBException
    {
        Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (CommonUtils.isEmpty(children)) {
            return;
        }
        for (DBSObject child : children) {
            if (objectType.isInstance(child)) {
                names.add(objectType.cast(child));
            }
        }
        if (names.isEmpty()) {
            // Nothing found - maybe we should go deeper in active container
            DBSObjectContainer activeContainer = DBUtils.getSelectedObject(executionContext, DBSObjectContainer.class);
            if (activeContainer != null && activeContainer != container) {
                makeProposalsFromChildren(monitor, executionContext, activeContainer, names);
            }
        }
    }
}
