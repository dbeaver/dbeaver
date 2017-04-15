/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.managers.stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.editors.text.BaseTextEditor;
import org.jkiss.dbeaver.ui.editors.xml.XMLEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
* XMLPanelEditor
*/
public class XMLPanelEditor extends AbstractTextPanelEditor {

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
        // TODO: move to base class. Refactor resource release
        StyledText editorControl = editor.getEditorControl();
        assert editorControl != null;
        initEditorSettings(editorControl);
        editorControl.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                editor.releaseEditorInput();
            }
        });
        return editorControl;
    }

    @Override
    public void primeEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @NotNull DBDContent value) throws DBException
    {
        monitor.beginTask("Prime content value", 1);
        try {
            monitor.subTask("Prime XML value");
            IEditorInput sqlInput = new ContentEditorInput(valueController, null, null, monitor);
            editor.init(subSite, sqlInput);
            applyEditorStyle();
        } catch (Exception e) {
            throw new DBException("Can't load XML vaue", e);
        } finally {
            monitor.done();
        }
    }

    @Override
    public void extractEditorValue(@NotNull DBRProgressMonitor monitor, @NotNull StyledText control, @NotNull DBDContent value) throws DBException
    {
        if (valueController.isReadOnly() || !editor.isDirty()) {
            return;
        }

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
    protected BaseTextEditor getTextEditor() {
        return editor;
    }
}
