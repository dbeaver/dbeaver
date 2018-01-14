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
package org.jkiss.dbeaver.ui.editors.content;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.*;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.texteditor.BasicTextEditorActionContributor;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * Content Editor contributor.
 * Uses text editor contributor to fill status bar and menu for possible integrated text editors.
 */
public class ContentEditorContributor extends MultiPageEditorActionBarContributor
{
    private final BasicTextEditorActionContributor textContributor;
    private ContentEditor activeEditor;
    //private IEditorPart activePage;

    private final IAction saveAction = new FileExportAction();
    private final IAction loadAction = new FileImportAction();
    private final IAction infoAction = new InfoAction();
    private Combo encodingCombo;

    public ContentEditorContributor()
    {
        textContributor = new BasicTextEditorActionContributor();
    }

    ContentEditor getEditor()
    {
        return activeEditor;
    }

    @Override
    public void init(IActionBars bars, IWorkbenchPage page)
    {
        super.init(bars, page);
        textContributor.init(bars, page);
    }

    @Override
    public void init(IActionBars bars)
    {
        super.init(bars);
        textContributor.init(bars);
    }

    @Override
    public void dispose()
    {
        textContributor.dispose();
        super.dispose();
    }

    @Override
    public void setActiveEditor(IEditorPart part)
    {
        super.setActiveEditor(part);
        this.activeEditor = (ContentEditor) part;

        if (this.activeEditor != null) {
            if (encodingCombo != null && !encodingCombo.isDisposed()) {
                String curCharset = activeEditor.getEditorInput().getEncoding();
                int charsetCount = encodingCombo.getItemCount();
                for (int i = 0; i < charsetCount; i++) {
                    if (encodingCombo.getItem(i).equals(curCharset)) {
                        encodingCombo.select(i);
                        break;
                    }
                }
            }
            loadAction.setEnabled(!activeEditor.getEditorInput().isReadOnly());
        }
    }

    @Override
    public void setActivePage(IEditorPart activeEditor)
    {
        //this.activePage = activeEditor;
        this.textContributor.setActiveEditor(activeEditor);
    }

    @Override
    public void contributeToMenu(IMenuManager manager)
    {
        super.contributeToMenu(manager);
        textContributor.contributeToMenu(manager);

        IMenuManager menu = new MenuManager("L&OB Editor");
        manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
        menu.add(saveAction);
        menu.add(loadAction);
        menu.add(new Separator());
        menu.add(infoAction);
        menu.add(new Separator());
    }

    @Override
    public void contributeToStatusLine(IStatusLineManager statusLineManager)
    {
        super.contributeToStatusLine(statusLineManager);
        textContributor.contributeToStatusLine(statusLineManager);
    }

    @Override
    public void contributeToToolBar(IToolBarManager manager)
    {
        super.contributeToToolBar(manager);
        textContributor.contributeToToolBar(manager);
        // Execution
        manager.add(saveAction);
        manager.add(loadAction);
        manager.add(new Separator());
        manager.add(infoAction);
        manager.add(new Separator());
        manager.add(new ControlContribution("Encoding")
        {
            @Override
            protected Control createControl(Composite parent)
            {
                String curCharset = null;
                if (getEditor() != null) {
                    curCharset = getEditor().getEditorInput().getEncoding();
                }
                encodingCombo = UIUtils.createEncodingCombo(parent, curCharset);
                encodingCombo.setToolTipText("Content Encoding");
                encodingCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        final ContentEditor contentEditor = getEditor();
                        if (contentEditor != null) {
                            final ContentEditorInput contentEditorInput = contentEditor.getEditorInput();
                            Combo combo = (Combo) e.widget;
                            final String charset = combo.getItem(combo.getSelectionIndex());
                            contentEditorInput.setEncoding(charset);
                        }

                    }
                });
                return encodingCombo;
            }

            @Override
            public void dispose() {
                encodingCombo = null;
                super.dispose();
            }
        });
    }

    /////////////////////////////////////////////////////////
    // Actions
    /////////////////////////////////////////////////////////

    public abstract class SimpleAction extends Action {

        SimpleAction(String id, String text, String toolTip, DBIcon icon)
        {
            super(text, DBeaverIcons.getImageDescriptor(icon));
            setId(id);
            //setActionDefinitionId(id);
            setToolTipText(toolTip);
        }

        @Override
        public abstract void run();

    }

    private class FileExportAction extends SimpleAction
    {
        FileExportAction()
        {
            super(IWorkbenchCommandConstants.FILE_EXPORT, "Export", "Save to File", UIIcon.SAVE_AS);
        }

        @Override
        public void run()
        {
            getEditor().doSaveAs();
        }
    }

    private class FileImportAction extends SimpleAction
    {
        FileImportAction()
        {
            super(IWorkbenchCommandConstants.FILE_IMPORT, "Import", "Load from File", UIIcon.LOAD);
        }

        @Override
        public void run()
        {
            final ContentEditor editor = getEditor();
            if (editor == null) {
                return;
            }
            Shell shell = editor.getSite().getShell();
            final File loadFile = DialogUtils.openFile(shell);
            if (loadFile == null) {
                return;
            }
            try {
                editor.getSite().getWorkbenchWindow().run(true, true, new IRunnableWithProgress() {
                    @Override
                    public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        try {
                            editor.getEditorInput().loadFromExternalFile(loadFile, monitor);
                        }
                        catch (CoreException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
                IValueController valueController = editor.getValueController();
                if (valueController != null) {
                    valueController.updateValue(editor.getValue(), true);
                }
                editor.setDirty(true);
                editor.fireContentChanged();
            }
            catch (InvocationTargetException e) {
                DBUserInterface.getInstance().showError(
                        "Can't load content",
                    "Can't load content from file '" + loadFile.getAbsolutePath() + "'",
                    e.getTargetException());
            }
            catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    private class InfoAction extends SimpleAction
    {
        InfoAction()
        {
            super("org.jkiss.dbeaver.lob.actions.info", "Info", "Show column information", DBIcon.TREE_INFO);
        }

        @Override
        public void run()
        {
            getEditor().toggleInfoBar();
        }
    }

}
