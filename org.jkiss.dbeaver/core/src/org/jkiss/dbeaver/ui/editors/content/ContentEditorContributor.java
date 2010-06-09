/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.content;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.actions.SimpleAction;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.core.DBeaverCore;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.SortedMap;

/**
 * LOB Editor contributor
 */
public class ContentEditorContributor extends MultiPageEditorActionBarContributor
{
    static Log log = LogFactory.getLog(ContentEditorContributor.class);

    private ContentEditor activeEditor;
    private IEditorPart activePage;

    private IAction saveAction = new SaveAction();
    private IAction loadAction = new LoadAction();
    private IAction infoAction = new InfoAction();
    private IAction applyAction = new ApplyAction();
    private IAction closeAction = new CloseAction();
    private Combo encodingCombo;

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
        super.dispose();
    }

    @Override
    public void setActiveEditor(IEditorPart part)
    {
        super.setActiveEditor(part);
        this.activeEditor = (ContentEditor) part;

        if (this.activeEditor != null && encodingCombo != null && !encodingCombo.isDisposed()) {
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
                encodingCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
                encodingCombo.setVisibleItemCount(30);
                SortedMap<String,Charset> charsetMap = Charset.availableCharsets();
                int index = 0;
                String curCharset = null;
                try {
                    curCharset = getEditor().getEditorInput().getFile().getCharset();
                } catch (CoreException e) {
                    log.error(e);
                }
                int defIndex = -1;
                for (String csName : charsetMap.keySet()) {
                    Charset charset = charsetMap.get(csName);
                    encodingCombo.add(charset.displayName());
                    if (charset.displayName().equalsIgnoreCase(curCharset)) {
                        defIndex = index;
                    }
                    index++;
                }
                if (defIndex >= 0) {
                    encodingCombo.select(defIndex);
                }
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
                                DBeaverCore.getInstance().run(false, false, new IRunnableWithProgress() {
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

    private class SaveAction extends SimpleAction
    {
        public SaveAction()
        {
            super(IWorkbenchCommandConstants.FILE_EXPORT, "Save", "Save to File", DBIcon.SAVE);
        }

        @Override
        public void run()
        {
            Shell shell = getEditor().getSite().getShell();
            FileDialog fileDialog = new FileDialog(shell, SWT.SAVE);
            fileDialog.setText("Save Content As");
            String fileName = fileDialog.open();
            if (CommonUtils.isEmpty(fileName)) {
                return;
            }
            final File saveFile = new File(fileName);
            File saveDir = saveFile.getParentFile();
            if (!saveDir.exists()) {
                DBeaverUtils.showErrorDialog(shell, "Bad file name", "Directory '" + saveDir.getAbsolutePath() + "' does not exists");
                return;
            }
            if (saveFile.exists()) {
                MessageBox aMessageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.YES | SWT.NO);
                aMessageBox.setText("File already exists");
                aMessageBox.setMessage("The file "+ saveFile.getAbsolutePath() + " already exists.\nOverwrite file?");

                if (aMessageBox.open() != SWT.YES) {
                    return;
                }
            }
            try {
                DBeaverCore.getInstance().run(true, true, new IRunnableWithProgress() {
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

    private class LoadAction extends SimpleAction
    {
        public LoadAction()
        {
            super(IWorkbenchCommandConstants.FILE_IMPORT, "Load", "Load from File", DBIcon.LOAD);
        }

        @Override
        public void run()
        {
            Shell shell = getEditor().getSite().getShell();
            FileDialog fileDialog = new FileDialog(shell, SWT.OPEN);
            String fileName = fileDialog.open();
            if (CommonUtils.isEmpty(fileName)) {
                return;
            }
            final File loadFile = new File(fileName);
            if (!loadFile.exists()) {
                MessageBox aMessageBox = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
                aMessageBox.setText("File doesn't exists");
                aMessageBox.setMessage("The file "+ loadFile.getAbsolutePath() + " doesn't exists.");
                aMessageBox.open();
                return;
            }
            try {
                DBeaverCore.getInstance().run(true, true, new IRunnableWithProgress() {
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
            super("org.jkiss.dbeaver.lob.actions.apply", "Apply Changes", "Save changes in database", DBIcon.ACCEPT);
        }

        @Override
        public void run()
        {
            try {
                DBeaverCore.getInstance().run(true, true, new IRunnableWithProgress() {
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
                    "Could not save content",
                    "Could not save content to database",
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
            super("org.jkiss.dbeaver.lob.actions.close", "Close", "Reject changes and close editorPart", DBIcon.REJECT);
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
