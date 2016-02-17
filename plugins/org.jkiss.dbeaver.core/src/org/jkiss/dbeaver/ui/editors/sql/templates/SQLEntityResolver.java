/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Entity resolver
 */
public class SQLEntityResolver extends SQLObjectResolver<DBSEntity> {

    public SQLEntityResolver()
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
            objectContainer = (DBSObjectContainer)DBUtils.getObjectByPath(monitor, objectContainer, catalogName, schemaName, null);
        } else {
            DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, executionContext.getDataSource());
            if (objectSelector != null) {
                objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, objectSelector.getSelectedObject());
            }
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
