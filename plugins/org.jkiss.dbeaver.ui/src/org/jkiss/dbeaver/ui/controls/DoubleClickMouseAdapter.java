/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;

public class DoubleClickMouseAdapter extends MouseAdapter {
    private boolean singleClick = false;

    public void onMouseSingleClick(@NotNull MouseEvent e) {
        // do nothing
    }

    public void onMouseDoubleClick(@NotNull MouseEvent e) {
        // do nothing
    }

    @Override
    public final void mouseDoubleClick(MouseEvent e) {
        singleClick = false;
        onMouseDoubleClick(e);
    }

    @Override
    public final void mouseDown(MouseEvent e) {
        singleClick = true;

        UIUtils.timerExec(UIUtils.getDisplay().getDoubleClickTime(), () -> {
            if (singleClick) {
                onMouseSingleClick(e);
            }
        });
    }
}
