/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2017 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.editors;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.utils.CommonUtils;

/**
 * DDL format constants
 * 
 * @author Denis Forveille
 * 
 */
public enum DB2DDLFormat {

    FORMAT("Format DDL", true),

    NO_FORMAT("Raw Text", false);

    private final String     title;
    private final boolean    needsFormatting;

    private static final Log log = org.jkiss.dbeaver.Log.getLog(DB2DDLFormat.class);

    // ------------
    // Constructors
    // ------------
    private DB2DDLFormat(String title, boolean needFormatting)
    {
        this.title = title;
        this.needsFormatting = needFormatting;
    }

    // ------------
    // Helpers
    // ------------
    public static DB2DDLFormat getCurrentFormat(DB2DataSource dataSource)
    {
        String ddlFormatString = dataSource.getContainer().getPreferenceStore().getString(DB2Constants.PREF_KEY_DDL_FORMAT);
        if (!CommonUtils.isEmpty(ddlFormatString)) {
            try {
                return DB2DDLFormat.valueOf(ddlFormatString);
            } catch (IllegalArgumentException e) {
                log.error(e);
            }
        }
        return DB2DDLFormat.FORMAT;
    }

    // ------------
    // Standard Getters
    // ------------

    public String getTitle()
    {
        return title;
    }

    public boolean needsFormatting()
    {
        return needsFormatting;
    }

}
