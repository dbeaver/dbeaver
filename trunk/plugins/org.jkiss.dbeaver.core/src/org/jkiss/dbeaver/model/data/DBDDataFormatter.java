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

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.Nullable;

import java.text.ParseException;
import java.util.Locale;
import java.util.Map;

/**
 * Data formatter
 */
public interface DBDDataFormatter {

    String TYPE_NAME_NUMBER = "number"; //$NON-NLS-1$
    String TYPE_NAME_DATE = "date"; //$NON-NLS-1$
    String TYPE_NAME_TIME = "time"; //$NON-NLS-1$
    String TYPE_NAME_TIMESTAMP = "timestamp"; //$NON-NLS-1$

    void init(Locale locale, Map<Object, Object> properties);

    @Nullable
    String getPattern();

    @Nullable
    String formatValue(Object value);

    @Nullable
    Object parseValue(String value, @Nullable Class<?> typeHint) throws ParseException;
    
}
