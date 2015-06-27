package org.jkiss.dbeaver.ext.erd;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.preferences.BundlePreferenceStore;

public class ERDPreferencesInitializer extends AbstractPreferenceInitializer {

  public ERDPreferencesInitializer() {
  }

  @Override
  public void initializeDefaultPreferences() {
      // Init default preferences
      DBPPreferenceStore store = new BundlePreferenceStore(ERDActivator.getDefault().getBundle());
      RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_PAGE_MODE, ERDConstants.PRINT_MODE_DEFAULT);
      RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_TOP, ERDConstants.PRINT_MARGIN_DEFAULT);
      RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_BOTTOM, ERDConstants.PRINT_MARGIN_DEFAULT);
      RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_LEFT, ERDConstants.PRINT_MARGIN_DEFAULT);
      RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_RIGHT, ERDConstants.PRINT_MARGIN_DEFAULT);
      RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_ENABLED, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_SNAP_ENABLED, true);
      RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_WIDTH, 20);
      RuntimeUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_HEIGHT, 20);
  }

} 