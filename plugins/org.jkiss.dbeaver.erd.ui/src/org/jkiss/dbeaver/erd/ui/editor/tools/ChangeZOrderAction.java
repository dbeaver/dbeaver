/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorPart;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.part.NodePart;

import java.util.List;

public class ChangeZOrderAction extends SelectionAction {

    private IStructuredSelection selection;
    private boolean front;

    public ChangeZOrderAction(ERDEditorPart part, IStructuredSelection selection, boolean front) {
        super(part);
        this.selection = selection;
        this.front = front;

        this.setText(front ? ERDUIMessages.erd_tool_set_text_text_bring_to_front : ERDUIMessages.erd_tool_set_text_text_send_to_back);
        this.setToolTipText(front ? ERDUIMessages.erd_tool_set_text_tip_text_bring_to_front : ERDUIMessages.erd_tool_set_text_tip_text_send_to_back);
        this.setId(front ? "bringToFront" : "sendToBack");
    }

    protected boolean calculateEnabled() {
        for (Object item : selection.toArray()) {
            if (item instanceof NodePart) {
                return true;
            }
        }
        return false;
    }

    protected void init() {
        super.init();
    }

    public void run() {
        this.execute(this.createReorderCommand(selection.toArray()));
    }

    private Command createReorderCommand(final Object[] objects) {
        return new Command() {
            @Override
            public void execute() {
                for (Object item : objects) {
                    if (item instanceof NodePart) {
                        IFigure child = ((NodePart) item).getFigure();
                        final IFigure parent = child.getParent();
                        final List children = parent.getChildren();
                        if (children != null) {
                            children.remove(child);
                            if (front) {
                                children.add(child);
                            } else {
                                children.add(0, child);
                            }
                            child.repaint();
                        }
                        //((NodePart) item).getDiagram().
                    }
                }
            }

            @Override
            public boolean canUndo() {
                return false;
            }
        };
    }
}
