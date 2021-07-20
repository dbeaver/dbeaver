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
package org.jkiss.dbeaver.tools.transfer.transformers;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlExpression;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferAttributeTransformer;

import java.util.Map;

/**
 * Expression attribute transformer
 */
public class DataTransferTransformerExpression implements IDataTransferAttributeTransformer {

    private JexlExpression jexlExpression;

    @Override
    public Object transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding[] dataAttributes, @NotNull Object[] dataRow, @NotNull DBDAttributeBinding attribute, Object attrValue, @NotNull Map<String, Object> options) throws DBException {
        JexlExpression jexlExpression = getJexlExpression(options);

        JexlContext context = new VariablesContext(session, dataAttributes, dataRow);

        return jexlExpression.evaluate(context);
    }

    public JexlExpression getJexlExpression(Map<String, Object> options) throws DBCException {
        if (jexlExpression == null) {
            String expr = JSONUtils.getString(options, "expression");
            if (expr == null) {
                throw new DBCException("Expression property not specified");
            }
            jexlExpression = DBVUtils.parseExpression(expr);
        }

        return jexlExpression;
    }

    private static class VariablesContext implements JexlContext {

        private final DBCSession session;
        private final DBDAttributeBinding[] dataAttributes;
        private final Object[] dataRow;

        public VariablesContext(DBCSession session, DBDAttributeBinding[] dataAttributes, Object[] dataRow) {
            this.session = session;
            this.dataAttributes = dataAttributes;
            this.dataRow = dataRow;
        }

        @Override
        public Object get(String s) {
            for (int i = 0; i < dataAttributes.length; i++) {
                if (dataAttributes[i].getName().equals(s)) {
                    return DBUtils.getAttributeValue(dataAttributes[i], dataAttributes, dataRow);
                }
            }
            return null;
        }

        @Override
        public void set(String s, Object o) {

        }

        @Override
        public boolean has(String s) {
            return get(s) != null;
        }
    }

}
