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
package org.jkiss.dbeaver.ui.e4;

import org.eclipse.e4.ui.internal.workbench.swt.AbstractPartRenderer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.renderers.swt.WBWRenderer;
import org.eclipse.e4.ui.workbench.renderers.swt.WorkbenchRendererFactory;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;

public class DBeaverRendererFactory extends WorkbenchRendererFactory {
    private DBeaverStackRenderer stackRenderer;
    private WBWRenderer windowRenderer;

    public DBeaverRendererFactory() {
    }

    @Override
    public AbstractPartRenderer getRenderer(MUIElement uiElement, Object parent) {
        if (uiElement instanceof MPartStack) {
            if (stackRenderer == null) {
                stackRenderer = new DBeaverStackRenderer();
                super.initRenderer(stackRenderer);
            }
            return stackRenderer;
        }
        if (uiElement instanceof MWindow) {
            if (windowRenderer == null) {
                windowRenderer = new WBWRenderer() {
                    @Override
                    public void postProcess(@NotNull MUIElement element) {
                        // initialize the compatibility window before its native shell is shown
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                        super.postProcess(element);
                    }
                };
                super.initRenderer(windowRenderer);
            }
            return windowRenderer;
        }
        return super.getRenderer(uiElement, parent);
    }
}