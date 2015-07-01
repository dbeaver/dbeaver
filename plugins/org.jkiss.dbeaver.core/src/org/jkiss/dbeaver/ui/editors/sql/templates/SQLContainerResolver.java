/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import org.jkiss.dbeaver.Log;
import org.eclipse.jface.text.templates.TemplateContext;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;

/**
 * Container resolver
 */
public class SQLContainerResolver<T extends DBSObjectContainer> extends SQLObjectResolver<T> {

    static final Log log = Log.getLog(SQLContainerResolver.class);
    public static final String VAR_NAME_SCHEMA = "schema";
    public static final String VAR_NAME_CATALOG = "catalog";

    private Class<T> objectType;

    public SQLContainerResolver(String id, String title, Class<T> objectType)
    {
        super(id, title);
        this.objectType = objectType;
    }

    @Override
    protected void resolveObjects(DBRProgressMonitor monitor, DBCExecutionContext executionContext, TemplateContext context, List<T> entities) throws DBException
    {
        DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, executionContext.getDataSource());
        if (objectContainer != null) {
            makeProposalsFromChildren(monitor, objectContainer, entities);
        }
    }

    void makeProposalsFromChildren(DBRProgressMonitor monitor, DBSObjectContainer container, List<T> names) throws DBException
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
            DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, container);
            if (objectSelector != null) {
                container = DBUtils.getAdapter(DBSObjectContainer.class, objectSelector.getSelectedObject());
                if (container != null) {
                    makeProposalsFromChildren(monitor, container, names);
                }
            }
        }
    }
}
