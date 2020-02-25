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
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformer;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.ProxyValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.time.ExtendedDateFormat;

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

    private static final SimpleDateFormat FORMAT_MILLIS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.ENGLISH);
    private static final SimpleDateFormat FORMAT_SECONDS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    private static final SimpleDateFormat FORMAT_NANOS = new ExtendedDateFormat("yyyy-MM-dd HH:mm:ss.ffffff",Locale.ENGLISH);

    enum EpochUnit {
        seconds,
        milliseconds,
        nanoseconds
    }

    @Override
    public void transformAttribute(@NotNull DBCSession session, @NotNull DBDAttributeBinding attribute, @NotNull List<Object[]> rows, @NotNull Map<String, Object> options) throws DBException {
        attribute.setPresentationAttribute(
            new TransformerPresentationAttribute(attribute, "EpochTime", -1, DBPDataKind.DATETIME));

        EpochUnit unit = EpochUnit.milliseconds;
        if (options.containsKey(PROP_UNIT)) {
            try {
                unit = EpochUnit.valueOf(CommonUtils.toString(options.get(PROP_UNIT)));
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
                    case seconds:
                        return FORMAT_SECONDS.format(new Date(dateValue * 1000));
                    case nanoseconds:
                        return FORMAT_NANOS.format(new Date(dateValue / 1000));
                    default:
                        return FORMAT_MILLIS.format(new Date(dateValue));
                }
            }
            return DBValueFormatting.getDefaultValueDisplayString(value, format);
        }

        @Nullable
        @Override
        public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) throws DBCException {
            if (object instanceof String) {
                try {
                    switch (unit) {
                        case seconds:
                            return FORMAT_SECONDS.parse((String) object).getTime() / 1000;
                        case milliseconds:
                            return FORMAT_MILLIS.parse((String) object).getTime();
                        case nanoseconds:
                            return FORMAT_NANOS.parse((String) object).getTime() * 1000;
                    }
                } catch (Exception e) {
                    log.debug("Error parsing time value", e);
                }
            }
            return super.getValueFromObject(session, type, object, copy, validateValue);
        }
    }
}
