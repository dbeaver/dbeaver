/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.actions.SimpleAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.SortedMap;
import java.nio.charset.Charset;
import java.lang.reflect.InvocationTargetException;

/**
 * LOB Editor contributor
 */
public class LOBEditorContributor extends MultiPageEditorActionBarContributor
{
    static Log log = LogFactory.getLog(LOBEditorContributor.class);

    private LOBEditor activeEditor;
    private IEditorPart activePage;

    private IAction saveAction = new SaveAction();
    private IAction loadAction = new LoadAction();
    private IAction infoAction = new InfoAction();
    private IAction applyAction = new ApplyAction();
    private IAction closeAction = new CloseAction();
    private Combo encodingCombo;

    public LOBEditorContributor()
    {
    }

    LOBEditor getEditor()
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
        this.activeEditor = (LOBEditor) part;

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
        menu.add(saveAction);
        menu.add(loadAction);
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
        manager.add(saveAction);
        manager.add(loadAction);
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
                        final LOBEditor lobEditor = getEditor();
                        if (lobEditor != null) {
                            final LOBEditorInput lobEditorInput = lobEditor.getEditorInput();
                            Combo combo = (Combo) e.widget;
                            final String charset = combo.getItem(combo.getSelectionIndex());
                            try {
                                lobEditor.getSite().getWorkbenchWindow().run(false, false, new IRunnableWithProgress() {
                                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                                        try {
                                            lobEditorInput.getFile().setCharset(charset, monitor);
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
            super(IWorkbenchActionDefinitionIds.SAVE, "Save", "Save to File", DBIcon.SAVE);
        }

        @Override
        public void run()
        {
        }
    }

    private class LoadAction extends SimpleAction
    {
        public LoadAction()
        {
            super("org.jkiss.dbeaver.lob.actions.load", "Load", "Load from File", DBIcon.LOAD);
        }

        @Override
        public void run()
        {
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
        }
    }

    private class CloseAction extends SimpleAction
    {
        public CloseAction()
        {
            super("org.jkiss.dbeaver.lob.actions.close", "Close", "Reject changes and close editor", DBIcon.REJECT);
        }

        @Override
        public void run()
        {
            LOBEditor lobEditor = getEditor();
            if (lobEditor != null) {
                lobEditor.closeValueEditor();
            }
        }
    }

}
