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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.CommandContributionItem;
import org.jkiss.dbeaver.ui.UIUtils;

public class VerticalFolderManager extends ContributionManager {

    private final VerticalFolder folder;
    private final int itemStyle;

    public VerticalFolderManager(VerticalFolder folder, int itemStyle) {
        this.folder = folder;
        this.itemStyle = itemStyle;
    }

    @Override
    public void update(boolean force) {
        for (IContributionItem item : getItems ()) {
            VerticalButton button = new VerticalButton(folder, itemStyle);
            if (item instanceof ActionContributionItem) {
                button.setAction(((ActionContributionItem) item).getAction(), false);
            } else if (item instanceof CommandContributionItem) {
                button.setCommand(UIUtils.getActiveWorkbenchWindow(), ((CommandContributionItem) item).getCommand().getId(), false);
            } else if (item instanceof Separator) {
                Label sepLabel = new Label(folder, SWT.SEPARATOR | SWT.HORIZONTAL);
            } else if (item instanceof GroupMarker) {
                // ignore
            } else {
                // ignore
                button.setText("N/A");
            }
        }
    }

}