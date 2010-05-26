/*
 * binary, a java binary editor
 * Copyright (C) 2006, 2009 Jordi Bergenthal, pestatije(-at_)users.sourceforge.net
 * The official binary site is sourceforge.net/projects/binary
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.jkiss.dbeaver.ui.editors.binary;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


/**
 * This class represents a preference page that is contributed to the Preferences dialog.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that belongs
 * to the main plug-in class. That way, preferences can be accessed directly via the preference store.
 */
public class PreferencesPage extends PreferencePage implements IWorkbenchPreferencePage {


static final String preferenceFontData = "font.data";
PreferencesManager preferences = null;


/**
 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
 */
protected Control createContents(Composite parent) {
	FontData fontData = HexConfig.getFontData();
	preferences = new PreferencesManager(fontData);
	
	return preferences.createPreferencesPart(parent);
}


/* (non-Javadoc)
 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
 */
public void init(IWorkbench workbench) {
}


/**
 * @see PreferencesPage#performDefaults()
 */
protected void performDefaults() {
	super.performDefaults();
	preferences.setFontData(null);
}


/**
 * @see PreferencesPage#performOk()
 */
public boolean performOk() {
	IPreferenceStore store = HexConfig.getInstance().getPreferenceStore();
	FontData fontData = preferences.getFontData();
	store.setValue(HexConfig.preferenceFontName, fontData.getName());
	store.setValue(HexConfig.preferenceFontStyle, fontData.getStyle());
	store.setValue(HexConfig.preferenceFontSize, fontData.getHeight());
	store.firePropertyChangeEvent(preferenceFontData, null, fontData);
	HexConfig.getInstance().savePreferences();
	
	return true;
}
}