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
/*
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorEmbedded;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Action to toggle diagram persistence
 */
public class DiagramTogglePersistAction extends Action {
    private final ERDEditorEmbedded editor;

    public DiagramTogglePersistAction(ERDEditorEmbedded editor) {
        super("Persist diagram", AS_CHECK_BOX);
        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.SAVE_TO_DATABASE));
        this.editor = editor;
    }

    @Override
    public boolean isChecked() {
        return editor.isStateSaved();
    }

    @Override
    public void run() {
        if (isChecked()) {
            boolean refreshDiagram = UIUtils.confirmAction("Refresh diagram", "Diagram persisted state was reset.\nDo you want to reload diagram view?");
            editor.resetSavedState(refreshDiagram);
        } else {
            editor.doSave(new NullProgressMonitor());
        }
    }

}