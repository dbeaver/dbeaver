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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.content.ContentEditorInput;
import org.jkiss.dbeaver.ui.editors.xml.XMLEditor;

import java.lang.reflect.InvocationTargetException;

/**
* ControlPanelEditor
*/
public class XMLPanelEditor extends ContentPanelEditor {

    private static final Log log = Log.getLog(XMLPanelEditor.class);
    private final IEditorSite subSite;
    private XMLEditor editor;

    public XMLPanelEditor(IValueController controller) {
        super(controller);
        this.subSite = new SubEditorSite(controller.getValueSite());
    }

    @Override
    public void primeEditorValue(@Nullable final Object value) throws DBException
    {
        if (value == null) {
            log.warn("NULL content value. Must be DBDContent.");
            return;
        }
        DBeaverUI.runInUI(valueController.getValueSite().getWorkbenchWindow(), new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask("Prime content value", 1);
                try {
                    monitor.subTask("Prime XML value");

                    IEditorInput sqlInput = new ContentEditorInput(valueController, null, monitor);
                    editor.init(subSite, sqlInput);
                } catch (Exception e) {
                    log.error(e);
                    valueController.showMessage(e.getMessage(), true);
                } finally {
                    monitor.done();
                }
            }
        });
    }

    @Override
    public Object extractEditorValue() throws DBException
    {
        final DBDContent content = (DBDContent) valueController.getValue();
        if (content == null) {
            log.warn("NULL content value. Must be DBDContent.");
        } else {
            final ContentEditorInput editorInput = (ContentEditorInput) editor.getEditorInput();
            if (editorInput != null) {
                DBeaverUI.runInUI(DBeaverUI.getActiveWorkbenchWindow(), new DBRRunnableWithProgress() {
                    @Override
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        monitor.beginTask("Read XML value", 1);
                        try {
                            monitor.subTask("Read XML value");
                            editor.doSave(RuntimeUtils.getNestedMonitor(monitor));

                            editorInput.updateContentFromFile(RuntimeUtils.getNestedMonitor(monitor));
                        } catch (Exception e) {
                            throw new InvocationTargetException(e);
                        } finally {
                            monitor.done();
                        }
                    }
                });
            }
        }
        return content;
    }

    @Override
    protected Control createControl(Composite editPlaceholder)
    {
        editor = new XMLEditor();
        try {
            editor.init(subSite, StringEditorInput.EMPTY_INPUT);
        } catch (PartInitException e) {
            log.error(e);
            return null;
        }
        editor.createPartControl(editPlaceholder);
        StyledText control = editor.getTextViewer().getTextWidget();
        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                editor.dispose();
            }
        });
        return control;

//        Text text = new Text(editPlaceholder, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
//        text.setEditable(!valueController.isReadOnly());
//        text.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
//        return text;
    }

}
