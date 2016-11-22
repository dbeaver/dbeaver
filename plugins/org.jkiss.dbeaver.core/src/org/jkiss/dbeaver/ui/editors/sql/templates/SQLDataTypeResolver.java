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
