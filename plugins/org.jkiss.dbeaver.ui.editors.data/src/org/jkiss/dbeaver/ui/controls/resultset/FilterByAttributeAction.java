/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.Action;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.ui.UITextUtils;

class FilterByAttributeAction extends Action {
    private final ResultSetViewer resultSetViewer;
    private final DBCLogicalOperator operator;
    private final FilterByAttributeType type;
    private final DBDAttributeBinding attribute;
    FilterByAttributeAction(ResultSetViewer resultSetViewer, DBCLogicalOperator operator, FilterByAttributeType type, DBDAttributeBinding attribute)
    {
        super(attribute.getName() + " " + translateFilterPattern(resultSetViewer, operator, type, attribute), null);
        this.resultSetViewer = resultSetViewer;
        this.operator = operator;
        this.type = type;
        this.attribute = attribute;
    }

    @Override
    public void run()
    {
        Object value = type.getValue(resultSetViewer, attribute, operator, false);
        if (operator.getArgumentCount() != 0 && value == null) {
            return;
        }
        DBDDataFilter filter = new DBDDataFilter(resultSetViewer.getModel().getDataFilter());
        DBDAttributeConstraint constraint = filter.getConstraint(attribute);
        if (constraint != null) {
            constraint.setOperator(operator);
            constraint.setValue(value);
            resultSetViewer.setDataFilter(filter, true);
        }
    }

    @NotNull
    private static String translateFilterPattern(@NotNull ResultSetViewer viewer, @NotNull DBCLogicalOperator operator, @NotNull FilterByAttributeType type, @NotNull DBDAttributeBinding attribute) {
        Object value = type.getValue(viewer, attribute, operator, true);

        DBCExecutionContext executionContext = viewer.getExecutionContext();
        String strValue = executionContext == null ? String.valueOf(value) : attribute.getValueHandler().getValueDisplayString(attribute, value, DBDDisplayFormat.UI);
        strValue = strValue.replaceAll("\\s+", " ").replace("@", "^").trim();
        strValue = UITextUtils.getShortText(viewer.getControl(), strValue, 150);
        if (operator.getArgumentCount() == 0) {
            return operator.getExpression();
        } else {
            if (!ResultSetViewer.CUSTOM_FILTER_VALUE_STRING.equals(strValue)) {
                strValue = "'" + strValue + "'";
            }
            return operator.getExpression() + " " + strValue;
        }
    }
}
