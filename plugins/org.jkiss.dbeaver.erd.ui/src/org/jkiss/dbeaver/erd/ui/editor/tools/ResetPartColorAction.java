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
package org.jkiss.dbeaver.erd.ui.editor.tools;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Color;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorPart;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.part.ICustomizablePart;

import java.util.HashMap;
import java.util.Map;

public class ResetPartColorAction extends SelectionAction {

    private IStructuredSelection selection;

    public ResetPartColorAction(ERDEditorPart part, IStructuredSelection selection) {
        super(part);
        this.selection = selection;

        this.setText(ERDUIMessages.erd_tool_color_action_text_reset_color);
        this.setToolTipText(ERDUIMessages.erd_tool_color_action_tip_text_reset_figure_color);
        this.setId("removeFigureColor");
    }

    protected boolean calculateEnabled() {
        for (Object item : selection.toArray()) {
            if (item instanceof ICustomizablePart && ((ICustomizablePart) item).getCustomBackgroundColor() != null) {
                return true;
            }
        }
        return false;
    }

    protected void init() {
        super.init();
    }

    public void run() {
        this.execute(this.createColorCommand(selection.toArray()));
    }

    private Command createColorCommand(final Object[] objects) {
        return new Command() {
            private final Map<ICustomizablePart, Color> oldColors = new HashMap<>();
            @Override
            public void execute() {
                for (Object item : objects) {
                    if (item instanceof ICustomizablePart) {
                        ICustomizablePart colorizedPart = (ICustomizablePart) item;
                        oldColors.put(colorizedPart, colorizedPart.getCustomBackgroundColor());
                        colorizedPart.setCustomBackgroundColor(null);
                    }
                }
            }

            @Override
            public void undo() {
                for (Object item : objects) {
                    if (item instanceof ICustomizablePart) {
                        ICustomizablePart colorizedPart = (ICustomizablePart) item;
                        colorizedPart.setCustomBackgroundColor(oldColors.get(colorizedPart));
                    }
                }
            }

            @Override
            public void redo() {
                for (Object item : objects) {
                    if (item instanceof ICustomizablePart) {
                        ICustomizablePart colorizedPart = (ICustomizablePart) item;
                        colorizedPart.setCustomBackgroundColor(null);
                    }
                }
            }
        };
    }


}
