/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.gis.internal;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.model.gis.GisConstants;
import org.jkiss.dbeaver.model.impl.preferences.BundlePreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.gis.GeometryViewerConstants;
import org.jkiss.dbeaver.utils.PrefUtils;

public class GISViewerPreferencesInitializer extends AbstractPreferenceInitializer {

  @Override
  public void initializeDefaultPreferences() {

      // Init default preferences
      DBPPreferenceStore store = new BundlePreferenceStore(GISViewerActivator.getDefault().getBundle());

      // View settings
      PrefUtils.setDefaultPreferenceValue(store, GeometryViewerConstants.PREF_MAX_OBJECTS_RENDER, GeometryViewerConstants.DEFAULT_MAX_OBJECTS_RENDER);
      PrefUtils.setDefaultPreferenceValue(store, GeometryViewerConstants.PREF_DEFAULT_SRID, GisConstants.SRID_4326);
  }

}
