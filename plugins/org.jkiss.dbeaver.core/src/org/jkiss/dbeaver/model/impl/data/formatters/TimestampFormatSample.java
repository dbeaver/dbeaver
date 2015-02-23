/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
