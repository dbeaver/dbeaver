/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
public enum ERDViewStyle
{
    ICONS(1, "Icons"),
    TYPES(2, "Data Types"),
    NULLABILITY(4, "Nullability"),
    COMMENTS(8, "Comments"),
    ENTITY_FQN(16, "Fully qualified names")
    ;

    private final int value;
    private final String title;

    private static final Log log = Log.getLog(ERDAttributeVisibility.class);

    ERDViewStyle(int value, String title) {
        this.value = value;
        this.title = title;
    }

    public int getValue() {
        return value;
    }

    public String getTitle() {
        return title;
    }

    public static ERDViewStyle[] getDefaultStyles(IPreferenceStore store)
    {
        String attrString = store.getString(ERDConstants.PREF_ATTR_STYLES);
        if (!CommonUtils.isEmpty(attrString)) {
            String[] psList = attrString.split(",");
            ERDViewStyle[] pList = new ERDViewStyle[psList.length];
            for (int i = 0; i < psList.length; i++) {
                try {
                    pList[i] = ERDViewStyle.valueOf(psList[i]);
                } catch (IllegalArgumentException e) {
                    log.warn(e);
                }
            }
            return pList;
        }
        return new ERDViewStyle[] { ICONS };
    }

    public static void setDefaultStyles(DBPPreferenceStore store, ERDViewStyle[] styles)
    {
        String stylesString = "";
        for (ERDViewStyle style : styles) {
            if (!stylesString.isEmpty()) stylesString += ",";
            stylesString += style.name();
        }
        store.setValue(
            ERDConstants.PREF_ATTR_STYLES,
            stylesString);
    }

}
