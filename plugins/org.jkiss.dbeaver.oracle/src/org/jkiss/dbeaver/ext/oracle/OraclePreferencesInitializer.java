package org.jkiss.dbeaver.ext.oracle;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

public class OraclePreferencesInitializer extends AbstractPreferenceInitializer {

    public OraclePreferencesInitializer()
    {
    }

    @Override
    public void initializeDefaultPreferences()
    {
        // Init default preferences
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
    }

} 