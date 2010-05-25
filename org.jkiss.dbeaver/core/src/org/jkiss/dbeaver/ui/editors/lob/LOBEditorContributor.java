/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.lob;

import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.MultiPageEditorActionBarContributor;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.actions.sql.AbstractSQLAction;
import org.jkiss.dbeaver.ui.actions.SimpleAction;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.SortedMap;
import java.nio.charset.Charset;
import java.lang.reflect.InvocationTargetException;

/**
 * LOB Editor contributor
 */
public class LOBEditorContributor extends MultiPageEditorActionBarContributor
{
    static Log log = LogFactory.getLog(LOBEditorContributor.class);

    private IEditorPart activeEditor;
    private IEditorPart activePage;

    private IAction saveAction = new SaveAction();
    private IAction loadAction = new LoadAction();
    private IAction infoAction = new InfoAction();
    private IAction applyAction = new ApplyAction();
    private IAction closeAction = new CloseAction();

    public LOBEditorContributor()
    {
    }

    LOBEditor getEditor()
    {
        if (activeEditor instanceof LOBEditor) {
            return ((LOBEditor)activeEditor);
        }
        return null;
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
        this.activeEditor = part;
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
                Combo encodingText = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
                encodingText.setVisibleItemCount(30);
                SortedMap<String,Charset> charsetMap = Charset.availableCharsets();
                int index = 0;
                int defIndex = -1;
                for (String csName : charsetMap.keySet()) {
                    Charset charset = charsetMap.get(csName);
                    encodingText.add(charset.displayName());
                    if (charset.equals(Charset.defaultCharset())) {
                        defIndex = index;
                    }
                    index++;
                }
                if (defIndex >= 0) {
                    encodingText.select(defIndex);
                }
                encodingText.setToolTipText("Content Encoding");
                encodingText.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        final LOBEditor lobEditor = getEditor();
                        if (lobEditor != null) {
                            final LOBEditorInput lobEditorInput = (LOBEditorInput)lobEditor.getEditorInput();
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
                return encodingText;
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
