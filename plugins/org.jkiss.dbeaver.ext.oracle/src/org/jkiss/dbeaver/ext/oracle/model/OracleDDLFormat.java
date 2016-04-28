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

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

/**
 * DDL format
 */
public enum OracleDDLFormat {

    FULL("Full DDL", true, true, true),
    NO_STORAGE("No storage information", false, true, true),
    COMPACT("Compact form", false, false, false);

    private final String title;
    private final boolean showStorage;
    private final boolean showSegments;
    private final boolean showTablespace;

    private static final Log log = org.jkiss.dbeaver.Log.getLog(OracleDDLFormat.class);

    private OracleDDLFormat(String title, boolean showStorage, boolean showSegments, boolean showTablespace)
    {
        this.showTablespace = showTablespace;
        this.showSegments = showSegments;
        this.showStorage = showStorage;
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public boolean isShowStorage()
    {
        return showStorage;
    }

    public boolean isShowSegments()
    {
        return showSegments;
    }

    public boolean isShowTablespace()
    {
        return showTablespace;
    }

    public static OracleDDLFormat getCurrentFormat(OracleDataSource dataSource) {
        String ddlFormatString = dataSource.getContainer().getPreferenceStore().getString(OracleConstants.PREF_KEY_DDL_FORMAT);
        if (!CommonUtils.isEmpty(ddlFormatString)) {
            try {
                return OracleDDLFormat.valueOf(ddlFormatString);
            } catch (IllegalArgumentException e) {
                log.error(e);
            }
        }
        return OracleDDLFormat.FULL;
    }

}
