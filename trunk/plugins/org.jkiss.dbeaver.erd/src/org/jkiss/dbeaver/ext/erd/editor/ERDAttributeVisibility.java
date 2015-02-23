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
package org.jkiss.dbeaver.ext.erd.editor;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
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

    static final Log log = Log.getLog(ERDAttributeVisibility.class);

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

    public static void setDefaultVisibility(IPreferenceStore store, ERDAttributeVisibility visibility)
    {
        store.setValue(
            ERDConstants.PREF_ATTR_VISIBILITY,
            visibility.name());
    }

}
