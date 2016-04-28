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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

/**
 * Entity attribute visibility
 */
public enum ERDAttributeVisibility
{

    ALL("All"),
    KEYS("Any keys"),
    PRIMARY("Primary key"),
    NONE("None");

    private final String title;

    private static final Log log = Log.getLog(ERDAttributeVisibility.class);

    ERDAttributeVisibility(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public static ERDAttributeVisibility getDefaultVisibility(IPreferenceStore store)
    {
        String attrVisibilityString = store.getString(ERDConstants.PREF_ATTR_VISIBILITY);
        if (!CommonUtils.isEmpty(attrVisibilityString)) {
            try {
                return ERDAttributeVisibility.valueOf(attrVisibilityString);
            } catch (IllegalArgumentException e) {
                log.warn(e);
            }
        }
        return PRIMARY;
    }

    public static void setDefaultVisibility(DBPPreferenceStore store, ERDAttributeVisibility visibility)
    {
        store.setValue(
            ERDConstants.PREF_ATTR_VISIBILITY,
            visibility.name());
    }

}
