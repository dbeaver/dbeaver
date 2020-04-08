package org.jkiss.dbeaver.ext.erd;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.utils.PrefUtils;

public class ERDPreferencesInitializer extends AbstractPreferenceInitializer {

  public ERDPreferencesInitializer() {
  }

  @Override
  public void initializeDefaultPreferences() {
      // Init default preferences
      DBPPreferenceStore store = new BundlePreferenceStore(ERDActivator.getDefault().getBundle());
      PrefUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_DIAGRAM_SHOW_VIEWS, true);

      PrefUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_PAGE_MODE, ERDConstants.PRINT_MODE_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_TOP, ERDConstants.PRINT_MARGIN_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_BOTTOM, ERDConstants.PRINT_MARGIN_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_LEFT, ERDConstants.PRINT_MARGIN_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_PRINT_MARGIN_RIGHT, ERDConstants.PRINT_MARGIN_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_ENABLED, false);
      PrefUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_SNAP_ENABLED, true);
      PrefUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_WIDTH, 20);
      PrefUtils.setDefaultPreferenceValue(store, ERDConstants.PREF_GRID_HEIGHT, 20);
  }

} 