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
package org.jkiss.dbeaver.ext.erd.model;

import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.utils.CommonUtils;

/**
 * Entity attribute visibility
 */
public enum ERDAttributeVisibility
{

    ALL(ERDMessages.erd_attribute_visibility_selection_item_all),
    KEYS(ERDMessages.erd_attribute_visibility_selection_item_any_keys),
    PRIMARY(ERDMessages.erd_attribute_visibility_selection_item_primary_key),
    NONE(ERDMessages.erd_attribute_visibility_selection_item_none);

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
        return ALL;
    }

    public static void setDefaultVisibility(DBPPreferenceStore store, ERDAttributeVisibility visibility)
    {
        store.setValue(
            ERDConstants.PREF_ATTR_VISIBILITY,
            visibility.name());
    }

}
