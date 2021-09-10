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
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Transforms string attribute value into numeric value
 */
public class NumericAttributeTransformer implements DBDAttributeTransformer {
    private static final Log log = Log.getLog(NumericAttributeTransformer.class);

    private static final String PROP_TYPE = "type";
    private static final String PROP_LENIENT = "lenient";

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, Object> options) throws DBException {
        final String type = CommonUtils.toString(options.get(PROP_TYPE), "double");
        final boolean lenient = CommonUtils.getBoolean(options.get(PROP_LENIENT), false);

        attribute.setTransformHandler(new NumericValueHandler(attribute.getValueHandler(), type, lenient));
        attribute.setPresentationAttribute(new TransformerPresentationAttribute(attribute, "numeric", -1, DBPDataKind.NUMERIC));
    }

    private static class NumericValueHandler extends ProxyValueHandler {
        private final String type;
        private final boolean lenient;

        public NumericValueHandler(@NotNull DBDValueHandler target, @NotNull String type, boolean lenient) {
            super(target);
            this.type = type;
            this.lenient = lenient;
        }

        @NotNull
        @Override
        public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute) {
            return Object.class;
        }

        @Override
        public void bindValueObject(@NotNull DBCSession session, @NotNull DBCStatement statement, @NotNull DBSTypedObject type, int index, @Nullable Object value) throws DBCException {
            super.bindValueObject(session, statement, type, index, CommonUtils.toString(value));
        }

        @Nullable
        @Override
        public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) throws DBCException {
            if (object instanceof String) {
                object = parseValue((String) object, this.type, this.lenient);
            }
            if (object instanceof Number) {
                return object;
            }
            return super.getValueFromObject(session, type, object, copy, validateValue);
        }

        @NotNull
        @Override
        public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
            if (value instanceof String) {
                value = parseValue((String) value, this.type, this.lenient);
            }
            if (value instanceof Number) {
                return CommonUtils.toString(value);
            }
            if (value == null) {
                return "";
            }
            return super.getValueDisplayString(column, value, format);
        }

        @Nullable
        private static Object parseValue(@NotNull String value, @NotNull String type, boolean lenient) {
            try {
                switch (type) {
                    case "byte":
                        return Byte.parseByte(value);
                    case "short":
                        return Short.parseShort(value);
                    case "int":
                        return Integer.parseInt(value);
                    case "long":
                        return Long.parseLong(value);
                    case "float":
                        return Float.parseFloat(value);
                    case "double":
                        return Double.parseDouble(value);
                    default:
                        return null;
                }
            } catch (NumberFormatException e) {
                if (lenient) {
                    log.trace("Error converting string '" + value + "' to " + type, e);
                    return value;
                }
                throw e;
            }
        }
    }
}
