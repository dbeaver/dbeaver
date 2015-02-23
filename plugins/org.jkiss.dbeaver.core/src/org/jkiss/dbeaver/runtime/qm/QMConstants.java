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

package org.jkiss.dbeaver.runtime.qm;

/**
 * QM log constants
 */
public class QMConstants {

    public static final String PROP_PREFIX = "qm.";

    public static final String PROP_OBJECT_TYPES = PROP_PREFIX + "objectTypes";
    public static final String PROP_QUERY_TYPES = PROP_PREFIX + "queryTypes";
    public static final String PROP_ENTRIES_PER_PAGE = PROP_PREFIX + "maxEntries";
    public static final String PROP_HISTORY_DAYS = PROP_PREFIX + "historyDays";
    public static final String PROP_STORE_LOG_FILE = PROP_PREFIX + "storeLogs";
    public static final String PROP_LOG_DIRECTORY = PROP_PREFIX + "logDirectory";

}
