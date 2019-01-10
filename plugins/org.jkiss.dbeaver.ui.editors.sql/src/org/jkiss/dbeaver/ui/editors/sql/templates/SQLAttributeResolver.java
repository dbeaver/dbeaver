/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity resolver
 */
public class SQLAttributeResolver extends TemplateVariableResolver {

    private static final Log log = Log.getLog(SQLAttributeResolver.class);

    public SQLAttributeResolver()
    {
        super("column", "Table column");
    }

    @Override
    protected String[] resolveAll(final TemplateContext context)
    {
        final DBCExecutionContext executionContext = ((DBPContextProvider) context).getExecutionContext();
        if (executionContext == null) {
            return super.resolveAll(context);
        }

        TemplateVariable tableVariable = ((SQLContext) context).getTemplateVariable("table");
        final String tableName = tableVariable == null ? null : tableVariable.getDefaultValue();
        if (!CommonUtils.isEmpty(tableName)) {
            final List<DBSEntityAttribute> attributes = new ArrayList<>();
            DBRRunnableWithProgress runnable = new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException
                {
                    try {
                        List<DBSEntity> entities = new ArrayList<>();
                        SQLEntityResolver.resolveTables(monitor, executionContext, context, entities);
                        if (!CommonUtils.isEmpty(entities)) {
                            DBSEntity table = DBUtils.findObject(entities, tableName);
                            if (table != null) {
                                attributes.addAll(CommonUtils.safeCollection(table.getAttributes(monitor)));
                            }
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            };
            RuntimeUtils.runTask(runnable, "Resolve attributes", 1000);
            if (!CommonUtils.isEmpty(attributes)) {
                String[] result = new String[attributes.size()];
                for (int i = 0; i < attributes.size(); i++) {
                    DBSEntityAttribute entity = attributes.get(i);
                    result[i] = entity.getName();
                }
                return result;
            }
        }
        return super.resolveAll(context);
    }

    @Override
    public void resolve(TemplateVariable variable, TemplateContext context)
    {
        super.resolve(variable, context);
        if (variable instanceof SQLVariable) {
            ((SQLVariable)variable).setResolver(this);
        }
    }

}
