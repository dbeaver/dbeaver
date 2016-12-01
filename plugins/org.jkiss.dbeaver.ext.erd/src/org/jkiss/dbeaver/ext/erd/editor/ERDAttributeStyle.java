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
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

/**
 * Entity attribute presentation
 */
public enum ERDAttributeStyle
{
    ICONS(1, "Icons"),
    TYPES(2, "Data Types"),
    NULLABILITY(4, "Nullability")
    ;

    private final int value;
    private final String title;

    private static final Log log = Log.getLog(ERDAttributeVisibility.class);

    ERDAttributeStyle(int value, String title) {
        this.value = value;
        this.title = title;
    }

    public int getValue() {
        return value;
    }

    public String getTitle() {
        return title;
    }

    public static ERDAttributeStyle[] getDefaultStyles(IPreferenceStore store)
    {
        String attrString = store.getString(ERDConstants.PREF_ATTR_STYLES);
        if (!CommonUtils.isEmpty(attrString)) {
            String[] psList = attrString.split(",");
            ERDAttributeStyle[] pList = new ERDAttributeStyle[psList.length];
            for (int i = 0; i < psList.length; i++) {
                try {
                    pList[i] = ERDAttributeStyle.valueOf(psList[i]);
                } catch (IllegalArgumentException e) {
                    log.warn(e);
                }
            }
            return pList;
        }
        return new ERDAttributeStyle[] { ICONS };
    }

    public static void setDefaultStyles(DBPPreferenceStore store, ERDAttributeStyle[] styles)
    {
        String stylesString = "";
        for (ERDAttributeStyle style : styles) {
            if (!stylesString.isEmpty()) stylesString += ",";
            stylesString += style.name();
        }
        store.setValue(
            ERDConstants.PREF_ATTR_STYLES,
            stylesString);
    }

}
