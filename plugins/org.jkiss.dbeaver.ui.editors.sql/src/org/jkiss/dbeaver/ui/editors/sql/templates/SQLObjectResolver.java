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
package org.jkiss.dbeaver.ui.editors.sql.templates;

import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract object resolver
 */
public abstract class SQLObjectResolver<T extends DBSObject> extends TemplateVariableResolver {

    private static final Log log = Log.getLog(SQLObjectResolver.class);

    public SQLObjectResolver(String type, String description)
    {
        super(type, description);
    }

    @Override
    protected String[] resolveAll(final TemplateContext context)
    {
        final List<T> entities = new ArrayList<>();
        if (context instanceof DBPContextProvider) {
            final DBCExecutionContext executionContext = ((DBPContextProvider) context).getExecutionContext();
            if (executionContext != null) {
                RuntimeUtils.runTask(new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException {
                        try {
                            resolveObjects(monitor, executionContext, context, entities);
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                }, "Resolve object references", 1000);
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

    protected abstract void resolveObjects(DBRProgressMonitor monitor, DBCExecutionContext executionContext, TemplateContext context, List<T> entities) throws DBException;
}
