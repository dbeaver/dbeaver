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
      DBPPreferenceStore store = new BundlePreferenceStore(ERDUIActivator.getDefault().getBundle());
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_DIAGRAM_SHOW_VIEWS, true);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_DIAGRAM_SHOW_PARTITIONS, false);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_DIAGRAM_CHANGE_BORDER_COLORS, true);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_DIAGRAM_CHANGE_HEADER_COLORS, true);

      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_PRINT_PAGE_MODE, ERDUIConstants.PRINT_MODE_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_PRINT_MARGIN_TOP, ERDUIConstants.PRINT_MARGIN_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_PRINT_MARGIN_BOTTOM, ERDUIConstants.PRINT_MARGIN_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_PRINT_MARGIN_LEFT, ERDUIConstants.PRINT_MARGIN_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_PRINT_MARGIN_RIGHT, ERDUIConstants.PRINT_MARGIN_DEFAULT);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_GRID_ENABLED, false);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_GRID_SNAP_ENABLED, true);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_GRID_WIDTH, 20);
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_GRID_HEIGHT, 20);
  }

} 