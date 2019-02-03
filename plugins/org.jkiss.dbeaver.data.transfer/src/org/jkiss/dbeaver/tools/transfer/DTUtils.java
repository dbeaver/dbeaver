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
package org.jkiss.dbeaver.tools.transfer;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;

import java.util.Map;

/**
 * Abstract node
 */
public class DTUtils {

    public static void addSummary(StringBuilder summary, String option, Object value) {
        summary.append("\t").append(option).append(": ").append(value).append("\n");
    }

    public static void addSummary(StringBuilder summary, String option, boolean value) {
        summary.append("\t").append(option).append(": ").append(value ? "Yes" : "No").append("\n");
    }

    public static void addSummary(StringBuilder summary, DataTransferProcessorDescriptor processor, Map<?, ?> props) {
        summary.append(processor.getName()).append(" settings:\n");
        for (DBPPropertyDescriptor prop : processor.getProperties()) {
            Object propValue = props.get(prop.getId());
            if (propValue == null) {
                propValue = prop.getDefaultValue();
            }
            if (propValue != null) {
                addSummary(summary, prop.getDisplayName(), propValue);
            }
        }
    }

    public static String getTableName(DBPDataSource dataSource, DBPNamedObject source, boolean shortName) {
        if (source instanceof DBSEntity) {
            return shortName ?
                DBUtils.getQuotedIdentifier((DBSObject) source) :
                DBUtils.getObjectFullName(source, DBPEvaluationContext.UI);
        } else {
            String tableName = null;
            if (source instanceof SQLQueryContainer) {
                tableName = getTableNameFromQuery(dataSource, (SQLQueryContainer) source, shortName);
            } else if (source instanceof IAdaptable) {
                SQLQueryContainer queryContainer = ((IAdaptable) source).getAdapter(SQLQueryContainer.class);
                if (queryContainer != null) {
                    tableName = getTableNameFromQuery(dataSource, queryContainer, shortName);
                }
            }
            if (tableName == null && source instanceof IAdaptable) {
                DBSDataContainer dataContainer = ((IAdaptable) source).getAdapter(DBSDataContainer.class);
                if (dataContainer instanceof DBSEntity) {
                    tableName = shortName ?
                        DBUtils.getQuotedIdentifier(dataContainer) :
                        DBUtils.getObjectFullName(dataContainer, DBPEvaluationContext.UI);
                }
            }
            if (tableName == null) {
                return shortName ?
                    DBUtils.getQuotedIdentifier(dataSource, source.getName()) :
                    DBUtils.getObjectFullName(source, DBPEvaluationContext.DML);
            }
            return tableName;
        }
    }

    public static String getTableNameFromQuery(DBPDataSource dataSource, SQLQueryContainer queryContainer, boolean shortName) {
        SQLScriptElement query = queryContainer.getQuery();
        if (query instanceof SQLQuery) {
            DBCEntityMetaData singleSource = ((SQLQuery) query).getSingleSource();
            if (singleSource != null) {
                if (shortName) {
                    return DBUtils.getQuotedIdentifier(dataSource.getDataSource(), singleSource.getEntityName());
                } else {
                    return DBUtils.getFullyQualifiedName(dataSource.getDataSource(), singleSource.getCatalogName(), singleSource.getSchemaName(), singleSource.getEntityName());
                }
            }
        }
        return null;
    }
}
