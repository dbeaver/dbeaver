/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.binary;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * The main plugin class to be used in the desktop.
 */
public class HexConfig {

    // Find/Replace dialog combo box lists
    private static List<Object[]> findReplaceFindList = null;
    private static List<Object[]> findReplaceReplaceList = null;
    // The shared instance.
    private static HexConfig instance;
    // Preferences font identifiers
    public static final String preferenceFontName = "font.name";
    public static final String preferenceFontSize = "font.size";
    public static final String preferenceFontStyle = "font.style";

    private IPreferenceStore preferenceStore = null;

    /**
     * Returns the shared instance.
     */
    public synchronized static HexConfig getInstance()
    {
        if (instance == null) {
            instance = new HexConfig();
        }
        return instance;
    }

    /**
     * Get the list of previous finds
     *
     * @return list of tuples {find String, Boolean:is hex(true) or text(false)}
     */
    public static List<Object[]> getFindReplaceFindList()
    {
        if (findReplaceFindList == null) {
            findReplaceFindList = new ArrayList<Object[]>();
        }

        return findReplaceFindList;
    }


    /**
     * Get the list of previous replaces
     *
     * @return list of tuples {find String, Boolean:is hex(true) or text(false)}
     */
    public static List<Object[]> getFindReplaceReplaceList()
    {
        if (findReplaceReplaceList == null) {
            findReplaceReplaceList = new ArrayList<Object[]>();
        }

        return findReplaceReplaceList;
    }


    /**
     * Get font data information common to all plugin editors. Data comes from preferences store.
     *
     * @return font data to be used by plugin editors. Returns null for default font data.
     */
    public static FontData getFontData()
    {
        IPreferenceStore store = getInstance().getPreferenceStore();
        String name = store.getString(preferenceFontName);
        int style = store.getInt(preferenceFontStyle);
        int size = store.getInt(preferenceFontSize);
        FontData fontData = null;
        if (name != null && !"".equals(name) && size > 0)
            fontData = new FontData(name, size, style);

        return fontData;
    }


    /**
     * @see AbstractUIPlugin#getPreferenceStore()
     */
    public IPreferenceStore getPreferenceStore()
    {
        if (preferenceStore == null) {
            preferenceStore = DBeaverCore.getInstance().getGlobalPreferenceStore();
            preferenceStore.setDefault(preferenceFontName, "Courier New");
            preferenceStore.setDefault(preferenceFontSize, 10);
        }

        return preferenceStore;
    }


    /**
     * The constructor.
     */
    public HexConfig()
    {
    }

    public void savePreferences()
    {
        RuntimeUtils.savePreferenceStore(getPreferenceStore());
    }

}