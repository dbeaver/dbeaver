/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.config.easy;

import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.dialogs.MultiPageWizardDialog;

public final class EasyConfigWizardDialog extends MultiPageWizardDialog {
    public EasyConfigWizardDialog(@NotNull IWorkbenchWindow window) {
        super(window, new EasyConfigWizard());
    }

    @NotNull
    @Override
    protected Point getInitialSize() {
        return new Point(700, 500);
    }

    @Override
    public void updateSize() {
        // don't update size - pages are adapted to the dialog size
    }
}
