/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

import java.sql.Timestamp;
import java.util.*;

public class TimestampFormatSample implements DBDDataFormatterSample {

    @Override
    public Map<Object, Object> getDefaultProperties(Locale locale)
    {
//        SimpleDateFormat tmp = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
//        String pattern = tmp.toPattern();
        return Collections.singletonMap((Object)DateTimeDataFormatter.PROP_PATTERN, (Object)(DateFormatSample.DEFAULT_DATE_PATTERN + " " + TimeFormatSample.DEFAULT_TIME_PATTERN));
    }

    @Override
    public Object getSampleValue()
    {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        ts.setNanos(ts.getNanos() + new Random(System.currentTimeMillis()).nextInt(99999));
        return ts;
    }

}
