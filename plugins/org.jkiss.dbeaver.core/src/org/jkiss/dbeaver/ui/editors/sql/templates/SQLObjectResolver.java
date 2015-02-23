/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract object resolver
 */
public abstract class SQLObjectResolver<T extends DBSObject> extends TemplateVariableResolver {

    static final Log log = Log.getLog(SQLObjectResolver.class);

    public SQLObjectResolver(String type, String description)
    {
        super(type, description);
    }

    @Override
    protected String[] resolveAll(final TemplateContext context)
    {
        final List<T> entities = new ArrayList<T>();
        if (context instanceof IDataSourceProvider) {
            try {
                DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        try {
                            resolveObjects(monitor, ((IDataSourceProvider) context).getDataSource(), context, entities);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                log.error(e.getTargetException());
            } catch (InterruptedException e) {
                // skip
            }
        }
        if (!CommonUtils.isEmpty(entities)) {
            String[] result = new String[entities.size()];
            for (int i = 0; i < entities.size(); i++) {
                T entity = entities.get(i);
                result[i] = entity.getName();
            }
            return result;
        }
        return super.resolveAll(context);
    }

    protected abstract void resolveObjects(DBRProgressMonitor monitor, DBPDataSource dataSource, TemplateContext context, List<T> entities) throws DBException;
}
