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
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.List;
import java.util.Map;

/**
 * Transforms numeric attribute value into epoch time
 */
public class BooleanAttributeTransformer implements DBDAttributeTransformer {

    private static final Log log = Log.getLog(BooleanAttributeTransformer.class);

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, Object> options) throws DBException {
        attribute.setPresentationAttribute(
            new TransformerPresentationAttribute(attribute, "boolean", -1, DBPDataKind.BOOLEAN));

        attribute.setTransformHandler(new BooleanValueHandler(attribute.getValueHandler()));
    }

    private static class BooleanValueHandler extends ProxyValueHandler {
        BooleanValueHandler(DBDValueHandler target) {
            super(target);
        }

        @NotNull
        @Override
        public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
            return Boolean.class;
        }

        @Override
        public void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index, @Nullable Object value) throws DBCException {
            if (Boolean.TRUE.equals(value)) {
                value = 1;
            } else if (Boolean.FALSE.equals(value)) {
                value = 0;
            }
            super.bindValueObject(session, statement, type, index, value);
        }

        @NotNull
        @Override
        public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
            if (value instanceof Number) {
                return Boolean.valueOf(((Number) value).byteValue() != 0).toString();
            }
            return DBValueFormatting.getDefaultValueDisplayString(value, format);
        }

        @Nullable
        @Override
        public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) throws DBCException {
            if (object instanceof Number) {
                return ((Number) object).byteValue() != 0;
            }
            return super.getValueFromObject(session, type, object, copy, validateValue);
        }
    }
}
