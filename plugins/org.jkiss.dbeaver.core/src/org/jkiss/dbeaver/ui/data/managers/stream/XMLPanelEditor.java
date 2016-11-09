/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.data.managers.stream;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.editors.ContentPanelEditor;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.editors.xml.XMLEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
* XMLPanelEditor
*/
public class XMLPanelEditor implements IStreamValueEditor<StyledText> {

    private IValueController valueController;
    private IEditorSite subSite;
    private XMLEditor editor;

    @Override
    public StyledText createControl(IValueController valueController)
    {
        this.valueController = valueController;
        this.subSite = new SubEditorSite(valueController.getValueSite());
        editor = new XMLEditor();
        try {
            editor.init(subSite, StringEditorInput.EMPTY_INPUT);
        } catch (PartInitException e) {
            valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
            return new StyledText(valueController.getEditPlaceholder(), SWT.NONE);
        }
        editor.createPartControl(valueController.getEditPlaceholder());
        ContentPanelEditor.setEditorSettings(editor.getEditorControl());
        return editor.getEditorControl();
    }

    @Override
    public void primeEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @NotNull DBDContent value) throws DBException
    {
        monitor.beginTask("Prime content value", 1);
        try {
            monitor.subTask("Prime XML value");
            IEditorInput sqlInput = new ContentEditorInput(valueController, null, null, monitor);
            editor.init(subSite, sqlInput);
        } catch (Exception e) {
            throw new DBException("Can't load XML vaue", e);
        } finally {
            monitor.done();
        }
    }

    @Override
    public void extractEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @NotNull DBDContent value) throws DBException
    {
        monitor.beginTask("Read XML value", 1);
        try {
            monitor.subTask("Read XML value");
            editor.doSave(RuntimeUtils.getNestedMonitor(monitor));
            final ContentEditorInput editorInput = (ContentEditorInput) editor.getEditorInput();
            editorInput.updateContentFromFile(RuntimeUtils.getNestedMonitor(monitor));
        } catch (Exception e) {
            throw new DBException("Error saving XML value", e);
        } finally {
            monitor.done();
        }
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull StyledText control) throws DBCException {

    }


}
