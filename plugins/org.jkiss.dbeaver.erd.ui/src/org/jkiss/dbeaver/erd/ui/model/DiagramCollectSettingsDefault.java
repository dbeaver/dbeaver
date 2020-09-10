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
package org.jkiss.dbeaver.erd.ui.model;

import org.jkiss.dbeaver.erd.model.DiagramCollectSettings;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;

/**
 * Table collector settings
 */
public class DiagramCollectSettingsDefault implements DiagramCollectSettings {
    @Override
    public boolean isShowViews() {
        return ERDUIActivator.getDefault().getPreferenceStore().getBoolean(ERDUIConstants.PREF_DIAGRAM_SHOW_VIEWS);
    }

    @Override
    public void setShowViews(boolean showViews) {
        ERDUIActivator.getDefault().getPreferenceStore().setValue(ERDUIConstants.PREF_DIAGRAM_SHOW_VIEWS, showViews);
    }

    @Override
    public boolean isShowPartitions() {
        return ERDUIActivator.getDefault().getPreferenceStore().getBoolean(ERDUIConstants.PREF_DIAGRAM_SHOW_PARTITIONS);
    }

    @Override
    public void setShowPartitions(boolean showPartitions) {
        ERDUIActivator.getDefault().getPreferenceStore().setValue(ERDUIConstants.PREF_DIAGRAM_SHOW_PARTITIONS, showPartitions);
    }

}
