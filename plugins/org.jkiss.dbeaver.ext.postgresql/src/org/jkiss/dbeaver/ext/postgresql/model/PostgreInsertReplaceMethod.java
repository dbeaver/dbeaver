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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDInsertReplaceMethod;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class PostgreInsertReplaceMethod implements DBDInsertReplaceMethod {
    private static final Log log = Log.getLog(PostgreInsertReplaceMethod.class);

    @NotNull
    @Override
    public String getOpeningClause(DBSTable table, DBRProgressMonitor monitor) {
        return "INSERT INTO";
    }

    @Override
    public String getTrailingClause(DBSTable table, DBRProgressMonitor monitor, DBSAttributeBase[] attributes) {
        StringBuilder query = new StringBuilder();
        try {
            String onConflictExpression = "ON CONFLICT (%s) DO UPDATE SET %s";
            Collection<? extends DBSTableConstraint> constraints = table.getConstraints(monitor);
            if (!CommonUtils.isEmpty(constraints)) {
                StringBuilder pkNames = new StringBuilder();
                Optional<? extends DBSTableConstraint> tableConstraint = constraints.stream().filter(key -> key.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY).findFirst();
                if (tableConstraint.isPresent()) {
                    DBSTableConstraint dbsTableConstraint = tableConstraint.get();
                    List<? extends DBSEntityAttributeRef> attributeReferences = dbsTableConstraint.getAttributeReferences(monitor);
                    if (!CommonUtils.isEmpty(attributeReferences)) {
                        boolean hasKey = false;
                        for (DBSEntityAttributeRef column : attributeReferences) {
                            if (hasKey) pkNames.append(",");
                            DBSEntityAttribute attribute = column.getAttribute();
                            if (attribute == null) continue;
                            pkNames.append(attribute.getName());
                            hasKey = true;
                        }
                        StringBuilder updateExpression = new StringBuilder();
                        updateExpression.append("(");
                        addAttributesNamesList(table, attributes, false, updateExpression);
                        updateExpression.append(") = (");
                        addAttributesNamesList(table, attributes, true, updateExpression);
                        updateExpression.append(")");
                        query.append(" ").append(String.format(onConflictExpression, pkNames, updateExpression));
                    }
                }
            }
        } catch (DBException e) {
            log.debug("Can't read table constraints list", e);
        }
        return query.toString();
    }

    private void addAttributesNamesList(DBSTable table, DBSAttributeBase[] attributes, boolean isExcluded, StringBuilder updateExpression) {
        boolean hasKey = false;
        for (DBSAttributeBase attribute : attributes) {
            if (DBUtils.isPseudoAttribute(attribute)) {
                continue;
            }
            if (hasKey) updateExpression.append(","); //$NON-NLS-1$
            hasKey = true;
            if (isExcluded) {
                updateExpression.append("EXCLUDED.");
            }
            updateExpression.append(getAttributeName(table, attribute));
        }
    }

    private String getAttributeName(DBSTable table, @NotNull DBSAttributeBase attribute) {
        // Do not quote pseudo attribute name
        return DBUtils.isPseudoAttribute(attribute) ? attribute.getName() : DBUtils.getObjectFullName(table.getDataSource(), attribute, DBPEvaluationContext.DML);
    }
}
