/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.eclipse.jface.text.templates.TemplateVariable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Entity resolver
 */
class SQLEntityResolver extends SQLObjectResolver<DBSEntity> {

    SQLEntityResolver()
    {
        super("table", "Database table");
    }

    @Override
    protected void resolveObjects(DBRProgressMonitor monitor, DBCExecutionContext executionContext, TemplateContext context, List<DBSEntity> entities) throws DBException
    {
        resolveTables(monitor, executionContext, context, entities);
    }

    public void resolve(TemplateVariable variable, TemplateContext context)
    {
        super.resolve(variable, context);
        if (variable instanceof SQLVariable) {
            ((SQLVariable)variable).setResolver(this);
        }
    }

    static void resolveTables(DBRProgressMonitor monitor, DBCExecutionContext executionContext, TemplateContext context, List<DBSEntity> entities) throws DBException
    {
        TemplateVariable schemaVariable = ((SQLContext) context).getTemplateVariable(SQLContainerResolver.VAR_NAME_SCHEMA);
        TemplateVariable catalogVariable = ((SQLContext) context).getTemplateVariable(SQLContainerResolver.VAR_NAME_CATALOG);

        String catalogName = catalogVariable == null ? null : catalogVariable.getDefaultValue();
        String schemaName = schemaVariable == null ? null : schemaVariable.getDefaultValue();
        DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, executionContext.getDataSource());
        if (objectContainer == null) {
            return;
        }
        if (!CommonUtils.isEmpty(catalogName) || !CommonUtils.isEmpty(schemaName)) {
            // Find container for specified schema/catalog
            objectContainer = (DBSObjectContainer)DBUtils.getObjectByPath(monitor, executionContext, objectContainer, catalogName, schemaName, null);
        } else {
            objectContainer = DBUtils.getSelectedObject(executionContext, DBSObjectContainer.class);
        }
        if (objectContainer == null) {
            // Possibly neither catalogs nor schemas are supported
            objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, executionContext.getDataSource());
        }
        if (objectContainer != null) {
            makeProposalsFromChildren(monitor, objectContainer, entities);
        }
    }

    static void makeProposalsFromChildren(DBRProgressMonitor monitor, DBSObjectContainer container, List<DBSEntity> names) throws DBException
    {
        Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (CommonUtils.isEmpty(children)) {
            return;
        }
        for (DBSObject child : children) {
            if (child instanceof DBSEntity) {
                names.add((DBSEntity) child);
            }
        }
    }
}
