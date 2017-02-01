/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NumberFormatSample implements DBDDataFormatterSample {

    public static final String PROP_USE_GROUPING ="useGrouping";
    public static final String PROP_MAX_INT_DIGITS ="maxIntegerDigits";
    public static final String PROP_MIN_INT_DIGITS ="minIntegerDigits";
    public static final String PROP_MAX_FRACT_DIGITS ="maxFractionDigits";
    public static final String PROP_MIN_FRACT_DIGITS  ="minFractionDigits";
    public static final String PROP_ROUNDING_MODE ="roundingMode";

    @Override
    public Map<Object, Object> getDefaultProperties(Locale locale)
    {
        NumberFormat tmp = NumberFormat.getNumberInstance(locale);
        Map<Object, Object> props = new HashMap<>();
        props.put(PROP_USE_GROUPING, tmp.isGroupingUsed());
        props.put(PROP_MAX_INT_DIGITS, tmp.getMaximumIntegerDigits());
        props.put(PROP_MIN_INT_DIGITS, tmp.getMinimumIntegerDigits());
        props.put(PROP_MAX_FRACT_DIGITS, Math.max(tmp.getMaximumFractionDigits(), 10));
        props.put(PROP_MIN_FRACT_DIGITS, tmp.getMinimumFractionDigits());
        props.put(PROP_ROUNDING_MODE, tmp.getRoundingMode().name());
        return props;
    }

    @Override
    public Object getSampleValue()
    {
        return 1234567890.012345;
    }

}
