/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.SimpleAction;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

/**
 * LOB Editor contributor
 */
public class ContentEditorContributor extends MultiPageEditorActionBarContributor
{
    static final Log log = LogFactory.getLog(ContentEditorContributor.class);

    private ContentEditor activeEditor;
    private IEditorPart activePage;

    private IAction saveAction = new FileExportAction();
    private IAction loadAction = new FileImportAction();
    private IAction infoAction = new InfoAction();
    private IAction applyAction = new ApplyAction();
    private IAction closeAction = new CloseAction();
    private Combo encodingCombo;

    private IPropertyListener dirtyListener = new IPropertyListener() {
        public void propertyChanged(Object source, int propId)
        {
            if (propId == ContentEditor.PROP_DIRTY) {
                if (applyAction != null && activeEditor != null) {
                    applyAction.setEnabled(activeEditor.isDirty());
                }
            }
        }
    };

    public ContentEditorContributor()
    {
    }

    ContentEditor getEditor()
    {
        return activeEditor;
    }

    @Override
    public void dispose()
    {
        if (activeEditor != null) {
            activeEditor.removePropertyListener(dirtyListener);
        }
        super.dispose();
    }

    @Override
    public void setActiveEditor(IEditorPart part)
    {
        super.setActiveEditor(part);
        if (activeEditor != null) {
            activeEditor.removePropertyListener(dirtyListener);
        }
        this.activeEditor = (ContentEditor) part;
        this.activeEditor.addPropertyListener(dirtyListener);

        if (this.activeEditor != null) {
            if (encodingCombo != null && !encodingCombo.isDisposed()) {
                try {
                    String curCharset = this.activeEditor.getEditorInput().getFile().getCharset();
                    int charsetCount = encodingCombo.getItemCount();
                    for (int i = 0; i < charsetCount; i++) {
                        if (encodingCombo.getItem(i).equals(curCharset)) {
                            encodingCombo.select(i);
                            break;
                        }
                    }
                } catch (CoreException e) {
                    log.error(e);
                }
            }
            if (applyAction != null) {
                applyAction.setEnabled(activeEditor.isDirty());
            }
            if (loadAction != null) {
                loadAction.setEnabled(!activeEditor.getEditorInput().isReadOnly());
            }

        }
    }

    public void setActivePage(IEditorPart activeEditor)
    {
        this.activePage = activeEditor;
    }

    @Override
    public void contributeToMenu(IMenuManager manager)
    {
        super.contributeToMenu(manager);

        IMenuManager menu = new MenuManager("L&OB Editor");
        manager.prependToGroup(IWorkbenchActionConstants.MB_ADDITIONS, menu);
        menu.add(loadAction);
        menu.add(saveAction);
        menu.add(new Separator());
        menu.add(infoAction);
        menu.add(new Separator());
        menu.add(applyAction);
        menu.add(closeAction);
    }

    @Override
    public void contributeToStatusLine(IStatusLineManager statusLineManager)
    {
        super.contributeToStatusLine(statusLineManager);
    }

    @Override
    public void contributeToToolBar(IToolBarManager manager)
    {
        super.contributeToToolBar(manager);
        // Execution
        manager.add(loadAction);
        manager.add(saveAction);
        manager.add(new Separator());
        manager.add(infoAction);
        manager.add(new Separator());
        manager.add(applyAction);
        manager.add(closeAction);
        manager.add(new Separator());
        manager.add(new ControlContribution("Encoding")
        {
            protected Control createControl(Composite parent)
            {
                String curCharset = null;
                if (getEditor() != null) {
                    try {
                        curCharset = getEditor().getEditorInput().getFile().getCharset();
                    } catch (CoreException e) {
                        log.error(e);
                    }
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
                            try {
                                contentEditor.getSite().getWorkbenchWindow().run(false, false, new IRunnableWithProgress() {
                                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                                        try {
                                            contentEditorInput.getFile().setCharset(charset, monitor);
                                        } catch (CoreException e1) {
                                            throw new InvocationTargetException(e1);
                                        }
                                    }
                                });
                            } catch (InvocationTargetException e1) {
                                log.error(e1.getTargetException());
                            } catch (InterruptedException e1) {
                                // do nothing
                            }
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

    private class FileExportAction extends SimpleAction
    {
        public FileExportAction()
        {
            super(IWorkbenchCommandConstants.FILE_EXPORT, "Export", "Save to File", DBIcon.EXPORT);
        }

        @Override
        public void run()
        {
            Shell shell = getEditor().getSite().getShell();
            final File saveFile = ContentUtils.selectFileForSave(shell);
            if (saveFile == null) {
                return;
            }
            try {
                getEditor().getSite().getWorkbenchWindow().run(true, true, new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        try {
                            getEditor().getEditorInput().saveToExternalFile(saveFile, monitor);
                        }
                        catch (CoreException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            }
            catch (InvocationTargetException e) {
                DBeaverUtils.showErrorDialog(
                    shell,
                    "Could not save content",
                    "Could not save content to file '" + saveFile.getAbsolutePath() + "'",
                    e.getTargetException());
            }
            catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    private class FileImportAction extends SimpleAction
    {
        public FileImportAction()
        {
            super(IWorkbenchCommandConstants.FILE_IMPORT, "Import", "Load from File", DBIcon.IMPORT);
        }

        @Override
        public void run()
        {
            Shell shell = getEditor().getSite().getShell();
            final File loadFile = ContentUtils.openFile(shell);
            if (loadFile == null) {
                return;
            }
            try {
                getEditor().getSite().getWorkbenchWindow().run(true, true, new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        try {
                            getEditor().getEditorInput().loadFromExternalFile(loadFile, monitor);
                        }
                        catch (CoreException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            }
            catch (InvocationTargetException e) {
                DBeaverUtils.showErrorDialog(
                    shell,
                    "Could not load content",
                    "Could not load content from file '" + loadFile.getAbsolutePath() + "'",
                    e.getTargetException());
            }
            catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    private class InfoAction extends SimpleAction
    {
        public InfoAction()
        {
            super("org.jkiss.dbeaver.lob.actions.info", "Info", "Show column information", DBIcon.INFO);
        }

        @Override
        public void run()
        {
            getEditor().toggleInfoBar();
        }
    }

    private class ApplyAction extends SimpleAction
    {
        public ApplyAction()
        {
            super("org.jkiss.dbeaver.lob.actions.apply", "Apply Changes", "Apply Changes", DBIcon.SAVE);
        }

        @Override
        public void run()
        {
            try {
                getEditor().getSite().getWorkbenchWindow().run(true, true, new IRunnableWithProgress() {
                    public void run(IProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        getEditor().doSave(monitor);
                    }
                });
            }
            catch (InvocationTargetException e) {
                DBeaverUtils.showErrorDialog(
                    getEditor().getSite().getShell(),
                    "Could not apply content changes",
                    "Could not apply content changes",
                    e.getTargetException());
            }
            catch (InterruptedException e) {
                // do nothing
            }

        }
    }

    private class CloseAction extends SimpleAction
    {
        public CloseAction()
        {
            super("org.jkiss.dbeaver.lob.actions.close", "Close", "Reject changes", DBIcon.REJECT);
        }

        @Override
        public void run()
        {
            ContentEditor contentEditor = getEditor();
            if (contentEditor != null) {
                contentEditor.closeValueEditor();
            }
        }
    }

}
