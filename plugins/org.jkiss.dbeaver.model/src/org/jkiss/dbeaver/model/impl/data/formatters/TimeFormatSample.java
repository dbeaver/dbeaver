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
package org.jkiss.dbeaver.model.impl.data.formatters;

import org.jkiss.dbeaver.model.data.DBDDataFormatterSample;

import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class TimeFormatSample implements DBDDataFormatterSample {

    public static final String DEFAULT_TIME_PATTERN = "HH:mm:ss";

    @Override
    public Map<Object, Object> getDefaultProperties(Locale locale)
    {
//        SimpleDateFormat tmp = (SimpleDateFormat)DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);
//        String pattern = tmp.toPattern();
        return Collections.singletonMap((Object)DateTimeDataFormatter.PROP_PATTERN, (Object)DEFAULT_TIME_PATTERN);
    }

    @Override
    public Object getSampleValue()
    {
        return new Date();
    }

}
