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
package org.jkiss.dbeaver.ui.editors.binary.pref;

import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
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
    public static final String PROP_DEF_WIDTH = "default.hex.width";

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

	public static String getDefaultWidth() {
		DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
    		return store.getString(DBeaverPreferences.HEX_DEF_WIDTH);
    	}
    /**
     * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent)
    {
        FontData fontData = getPrefFontData();
        String defWidth = getDefaultWidth();
        preferences = new HexPreferencesManager(fontData, defWidth);

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

        store.setValue(DBeaverPreferences.HEX_DEF_WIDTH, preferences.getDefWidth());
        store.firePropertyChangeEvent(PROP_DEF_WIDTH, 0, preferences.getDefWidth());
        
        PrefUtils.savePreferenceStore(store);

        return true;
    }
}