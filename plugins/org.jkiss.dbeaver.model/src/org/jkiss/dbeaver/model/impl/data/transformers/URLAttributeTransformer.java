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
package org.jkiss.dbeaver.model.impl.data.transformers;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * Transforms string/numeric value into URL
 */
public class URLAttributeTransformer implements DBDAttributeTransformer {

    private static final Log log = Log.getLog(URLAttributeTransformer.class);

    private static final String PROP_PATTERN = "pattern";
    private static final String PROP_VIEW_INLINE = "view.inline";
    private static final String PROP_VIEW_PANEL = "view.panel";

    public static final String URL_TYPE_NAME = "URL.Preview";

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, String> options) throws DBException {
        attribute.setPresentationAttribute(
            new TransformerPresentationAttribute(attribute, URL_TYPE_NAME, -1, DBPDataKind.STRING));

        String pattern = null;
        if (options.containsKey(PROP_PATTERN)) {
            try {
                pattern = options.get(PROP_PATTERN);
            } catch (IllegalArgumentException e) {
                log.error("Bad unit option", e);
            }
        }
        attribute.setTransformHandler(new URLValueHandler(attribute.getValueHandler(), pattern));
    }

    private class URLValueHandler extends ProxyValueHandler {
        private final String pattern;
        private final MessageFormat messageFormat;

        public URLValueHandler(DBDValueHandler target, String pattern) {
            super(target);
            this.pattern = pattern.replace("${value}", "{0}");
            this.messageFormat = new MessageFormat(this.pattern);
        }

        @NotNull
        @Override
        public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
            if (pattern == null) {
                return DBValueFormatting.getDefaultValueDisplayString(value, format);
            } else {
                return messageFormat.format(new Object[] { value } );
            }
        }

        @Nullable
        @Override
        public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy) throws DBCException {
            if (pattern == null) {
                return super.getValueFromObject(session, type, object, copy);
            } else if (DBUtils.isNullValue(object)) {
                return null;
            } else {
                try {
                    Object[] parsedValues = messageFormat.parse(object.toString());
                    if (parsedValues.length > 0) {
                        return super.getValueFromObject(session, type, parsedValues[0], copy);
                    }
                    return object;
                } catch (ParseException e) {
                    return object;
                }
            }
        }
    }
}
