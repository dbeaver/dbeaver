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
package org.jkiss.dbeaver.ui.editors.binary.pref;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;


/**
 * This class represents a preference page that is contributed to the Preferences dialog.
 * <p/>
 * This page is used to modify preferences only. They are stored in the preference store that belongs
 * to the main plug-in class. That way, preferences can be accessed directly via the preference store.
 */
public class HexPreferencesPage extends AbstractPrefPage implements IWorkbenchPreferencePage {

    public static final String PROP_FONT_DATA = "prop.hex.font.data";

    private HexPreferencesManager preferences = null;

    /**
     * Get font data information common to all plugin editors. Data comes from preferences store.
     *
     * @return font data to be used by plugin editors. Returns null for default font data.
     */
    public static FontData getPrefFontData()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        String fontName = store.getString(DBeaverPreferences.HEX_FONT_NAME);
        int fontStyle = store.getInt(DBeaverPreferences.HEX_FONT_STYLE);
        int fontSize = store.getInt(DBeaverPreferences.HEX_FONT_SIZE);
        if (!CommonUtils.isEmpty(fontName) && fontSize > 0) {
            return new FontData(fontName, fontSize, fontStyle);
        }

        return null;
    }


    /**
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent)
    {
        FontData fontData = getPrefFontData();
        preferences = new HexPreferencesManager(fontData);

        return preferences.createPreferencesPart(parent);
    }


/* (non-Javadoc)
 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
 */

    @Override
    public void init(IWorkbench workbench)
    {
    }


    /**
     * @see HexPreferencesPage#performDefaults()
     */
    @Override
    protected void performDefaults()
    {
        super.performDefaults();
        preferences.setFontData(null);
    }

    /**
     * @see HexPreferencesPage#performOk()
     */
    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        FontData fontData = preferences.getFontData();
        store.setValue(DBeaverPreferences.HEX_FONT_NAME, fontData.getName());
        store.setValue(DBeaverPreferences.HEX_FONT_STYLE, fontData.getStyle());
        store.setValue(DBeaverPreferences.HEX_FONT_SIZE, fontData.getHeight());
        store.firePropertyChangeEvent(PROP_FONT_DATA, null, fontData);

        PrefUtils.savePreferenceStore(store);

        return true;
    }
}