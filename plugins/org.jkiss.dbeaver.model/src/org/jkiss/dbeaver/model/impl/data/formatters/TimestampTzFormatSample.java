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
package org.jkiss.dbeaver.model.impl.data.formatters;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class TimestampTzFormatSample implements DBDDataFormatterSample {

    @Override
    public Map<String, Object> getDefaultProperties(Locale locale) {
        return Collections.singletonMap(
            DateTimeDataFormatter.PROP_PATTERN,
            DBConstants.DEFAULT_TIMESTAMP_TZ_FORMAT);
    }

    @Override
    public Object getSampleValue() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        ts.setNanos(ts.getNanos() + new Random(System.currentTimeMillis()).nextInt(99999));
        return ts;
    }

}
