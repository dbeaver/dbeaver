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
