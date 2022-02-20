/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
 * Created on Jun 25, 2021
 */
package org.jkiss.dbeaver.erd.ui.action;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorPart;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.erd.ui.model.DiagramLoader;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.File;
import java.io.FileWriter;

/**
 * Action to toggle diagram persistence
 */
public class DiagramExportAction extends Action {

    private static final Log log = Log.getLog(DiagramExportAction.class);

    private final ERDEditorPart editor;
    private final Shell shell;

    public DiagramExportAction(ERDEditorPart editor, Shell shell) {
        super(ERDUIMessages.erd_action_diagram_export_text, DBeaverIcons.getImageDescriptor(UIIcon.EXPORT));
        setDescription(ERDUIMessages.erd_action_diagram_export_description);
        setToolTipText(getDescription());
        this.editor = editor;
        this.shell = shell;
    }

    @Override
    public void run() {
        try {
            final File path = DialogUtils.selectFileForSave(shell, ERDUIMessages.erd_action_diagram_export_dialog_save_as_title, new String[]{"*.erd"}, null);
            if (path != null) {
                try (FileWriter writer = new FileWriter(path, GeneralUtils.UTF8_CHARSET)) {
                    writer.write(DiagramLoader.serializeDiagram(new VoidProgressMonitor(), editor.getDiagramPart(), editor.getDiagram(), false, true));
                }
            } else {
                log.error("Invalid path to save the image");
            }
        } catch (Exception e) {
            log.error("Error saving diagram", e);
        }
    }

}