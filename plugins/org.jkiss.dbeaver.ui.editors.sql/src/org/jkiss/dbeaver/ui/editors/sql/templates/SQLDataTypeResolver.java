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
import org.eclipse.jface.text.templates.TemplateVariableResolver;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataTypeProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * Data type resolver
 */
public class SQLDataTypeResolver extends TemplateVariableResolver {

    private static final Log log = Log.getLog(SQLDataTypeResolver.class);

    public SQLDataTypeResolver()
    {
        super("type", "Data type");
    }

    @Override
    protected String[] resolveAll(final TemplateContext context)
    {
        final DBCExecutionContext executionContext = ((DBPContextProvider) context).getExecutionContext();
        if (executionContext == null) {
            return super.resolveAll(context);
        }

        DBPDataTypeProvider dataTypeProvider = DBUtils.getAdapter(DBPDataTypeProvider.class, executionContext.getDataSource());
        if (dataTypeProvider != null) {
            final Collection<? extends DBSDataType> localDataTypes = dataTypeProvider.getLocalDataTypes();
            if (!CommonUtils.isEmpty(localDataTypes)) {
                String[] result = new String[localDataTypes.size()];
                int index = 0;
                for (DBSDataType dataType : localDataTypes) {
                    result[index++] = dataType.getName();
                }
                return result;
            }
        }
        return super.resolveAll(context);
    }

}
