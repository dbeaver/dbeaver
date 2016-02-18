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

package org.jkiss.dbeaver.model.qm;

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
