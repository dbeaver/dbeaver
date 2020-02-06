/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.colors;

import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;

public class ResetAllColorAction extends ColorAction {
    public ResetAllColorAction(ResultSetViewer resultSetViewer) {
        super(resultSetViewer,"Reset all colors");
    }

    @Override
    public void run() {
        final DBVEntity vEntity = getColorsVirtualEntity();
        if (!UIUtils.confirmAction("Reset all row coloring", "Are you sure you want to reset all color settings for '" + vEntity.getName() + "'?")) {
            return;
        }
        vEntity.removeAllColorOverride();
        updateColors(vEntity);
    }
}
