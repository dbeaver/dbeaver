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
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Transforms numeric attribute value into epoch time
 */
public class EpochTimeAttributeTransformer implements DBDAttributeTransformer {

    private static final Log log = Log.getLog(EpochTimeAttributeTransformer.class);
    private static final String PROP_UNIT = "unit";

    private static final SimpleDateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT, Locale.ENGLISH);

    enum EpochUnit {
        seconds,
        milliseconds,
        nanoseconds
    }

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, String> options) throws DBException {
        attribute.setPresentationAttribute(
            new TransformerPresentationAttribute(attribute, "EpochTime", -1, DBPDataKind.DATETIME));

        EpochUnit unit = EpochUnit.milliseconds;
        if (options.containsKey(PROP_UNIT)) {
            try {
                unit = EpochUnit.valueOf(options.get(PROP_UNIT));
            } catch (IllegalArgumentException e) {
                log.error("Bad unit option", e);
            }
        }
        attribute.setTransformHandler(new EpochValueHandler(attribute.getValueHandler(), unit));
    }

    private class EpochValueHandler extends ProxyValueHandler {
        private final EpochUnit unit;
        public EpochValueHandler(DBDValueHandler target, EpochUnit unit) {
            super(target);
            this.unit = unit;
        }

        @NotNull
        @Override
        public String getValueDisplayString(@NotNull DBSTypedObject column, @Nullable Object value, @NotNull DBDDisplayFormat format) {
            if (value instanceof Number) {
                long dateValue = ((Number) value).longValue();
                switch (unit) {
                    case seconds: dateValue *= 1000; break;
                    case nanoseconds: dateValue /= 1000; break;
                }
                return DEFAULT_TIME_FORMAT.format(new Date(dateValue));
            }
            return DBValueFormatting.getDefaultValueDisplayString(value, format);
        }

        @Nullable
        @Override
        public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy) throws DBCException {
            if (object instanceof String) {
                try {
                    return DEFAULT_TIME_FORMAT.parse((String) object).getTime();
                } catch (Exception e) {
                    log.debug("Error parsing time value", e);
                }
            }
            return super.getValueFromObject(session, type, object, copy);
        }
    }
}
