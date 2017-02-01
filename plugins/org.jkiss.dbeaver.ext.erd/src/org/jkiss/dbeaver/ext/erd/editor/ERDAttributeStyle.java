/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
