/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
