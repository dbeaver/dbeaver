/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
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
      PrefUtils.setDefaultPreferenceValue(store, ERDUIConstants.PREF_ROUTING_TYPE, ERDUIConstants.ROUTING_SHORTEST_PATH);
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