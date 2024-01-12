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
package org.jkiss.dbeaver.erd.ui.action;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.gef.EditDomain;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.erd.ui.ERDUIUtils;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorAdapter;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorPart;


public class ERDHandlerFocus extends AbstractHandler {
    
    private static final String DIAGRAM_FOCUS = "org.jkiss.dbeaver.erd.focus.diagram";
    private static final String PALETTE_FOCUS = "org.jkiss.dbeaver.erd.focus.palette";
    private static final String OUTLINE_FOCUS = "org.jkiss.dbeaver.erd.focus.outline";
    private static final String PARAMETER_FOCUS= "org.jkiss.dbeaver.erd.focus.parameter";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ERDEditorPart part = getPart(event);
        if (part == null) {
            return null;
        }
        switch (event.getCommand().getId()) {
            case DIAGRAM_FOCUS:
                part.getViewer().getControl().forceFocus();
                break;
            case PALETTE_FOCUS:
                Object adapter = part.getAdapter(EditDomain.class);
                if (adapter instanceof EditDomain && ((EditDomain) adapter).getPaletteViewer() != null) {
                    ((EditDomain) adapter).getPaletteViewer().getControl().setFocus();
                }
                break;
            case OUTLINE_FOCUS:
                ERDUIUtils.openOutline();
                break;
            case PARAMETER_FOCUS:
                ERDUIUtils.openProperties();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + event.getCommand().getId());
        }
        return null;
    }

    private ERDEditorPart getPart(ExecutionEvent event) {
        ERDEditorPart editor = null;
        Control control = (Control) HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        if (control != null) {
            editor = ERDEditorAdapter.getEditor(control);
        }
        if (editor == null) {
            Object activeEditor = HandlerUtil.getVariable(event, ISources.ACTIVE_EDITOR_NAME);
            if (activeEditor != null) {
                editor = new ERDEditorAdapter().getAdapter(activeEditor, ERDEditorPart.class);
            }
        }
        return editor;
    }
}
