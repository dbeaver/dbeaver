/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.CustomSelectionProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.lang.reflect.InvocationTargetException;

/**
 * SourceEditSection
 */
public abstract class SourceEditSection extends AbstractPropertySection implements IRefreshablePart {

    static final Log log = LogFactory.getLog(SourceEditSection.class);

    private final IDatabaseNodeEditor editor;
    private Composite parent;
    private SQLEditorBase sqlViewer;
    private final ISelectionProvider selectionProvider = new CustomSelectionProvider();
    private final IAction actionDelete = new ActionDelete();
    private boolean sourcesModified = false;

    protected SourceEditSection(IDatabaseNodeEditor editor)
    {
        this.editor = editor;
    }

    public IDatabaseNodeEditor getEditor()
    {
        return editor;
    }

    public void createControls(Composite parent, final TabbedPropertySheetPage tabbedPropertySheetPage)
    {
		super.createControls(parent, tabbedPropertySheetPage);
        this.parent = parent;
	}

    @Override
    public void dispose()
    {
        super.dispose();
    }

    private void createEditor()
    {
        sqlViewer = new SQLEditorBase() {
            public DBPDataSource getDataSource()
            {
                return editor.getDataSource();
            }
        };
        sqlViewer.setHasVerticalRuler(false);
        try {
            sqlViewer.init(editor.getEditorSite(), makeSourcesInput());
        } catch (PartInitException e) {
            UIUtils.showErrorDialog(parent.getShell(), "Create SQL viewer", null, e);
        }
        sqlViewer.createPartControl(parent);
        sqlViewer.reloadSyntaxRules();
        parent.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                if (sqlViewer != null) {
                    sqlViewer.dispose();
                    sqlViewer = null;
                }
            }
        });
        sqlViewer.getTextViewer().getTextWidget().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e)
            {
                if (sourcesModified) {
                    updateSources(sqlViewer.getDocument().get());
                    sourcesModified = false;
                }
            }
        });
        sqlViewer.getDocument().addDocumentListener(new IDocumentListener() {
            public void documentAboutToBeChanged(DocumentEvent event)
            {
            }

            public void documentChanged(DocumentEvent event)
            {
                if (!sourcesModified) {
                    updateSources(event.getDocument().get());
                    sourcesModified = true;
                }
            }
        });
    }

    protected IEditorInput makeSourcesInput()
    {
        final SourceLoader sourceLoader = new SourceLoader();
        try {
            if (!isSourceRead()) {
                DBeaverCore.getInstance().runInProgressService(sourceLoader);
            } else {
                sourceLoader.run(VoidProgressMonitor.INSTANCE);
            }
        } catch (InvocationTargetException e) {
            log.error("Can't load source", e.getTargetException());
        } catch (InterruptedException e) {
            // Skip
        }
        return new StringEditorInput("Source", sourceLoader.source, isReadOnly());
    }

    protected boolean isSourceRead()
    {
        return false;
    }

    protected boolean isReadOnly()
    {
        return false;
    }

    protected abstract String loadSources(DBRProgressMonitor monitor) throws DBException;

    protected void updateSources(String source)
    {
        throw new IllegalStateException("Update is not supported in " + getClass().getName());
    }

    public boolean shouldUseExtraSpace()
    {
		return true;
	}

    @Override
    public void aboutToBeShown()
    {
        if (sqlViewer == null) {
            createEditor();
        }
        sqlViewer.enableUndoManager(true);
        editor.getSite().setSelectionProvider(selectionProvider);
        selectionProvider.setSelection(new StructuredSelection());

        final IActionBars actionBars = editor.getEditorSite().getActionBars();
        actionBars.setGlobalActionHandler(IWorkbenchCommandConstants.EDIT_DELETE, actionDelete);
    }

    @Override
    public void aboutToBeHidden()
    {
        if (sqlViewer != null) {
            sqlViewer.enableUndoManager(false);
        }
        final IActionBars actionBars = editor.getEditorSite().getActionBars();
        actionBars.setGlobalActionHandler(IWorkbenchCommandConstants.EDIT_DELETE, null);
    }

    public void refreshPart(Object source)
    {
        // Reload sources
        sqlViewer.setInput(makeSourcesInput());
    }

    private class SourceLoader implements DBRRunnableWithProgress {
        String source;
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                source = loadSources(monitor);
            } catch (DBException e) {
                source = e.getMessage();
                throw new InvocationTargetException(e);
            }
            finally {
                if (source == null) {
                    source = "";
                }
            }
        }
    }

    private class ActionDelete extends Action {
        private ActionDelete()
        {
            setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);
        }

        @Override
        public void run()
        {
            if (sqlViewer != null && !sqlViewer.isDisposed()) {
                sqlViewer.doOperation(ITextOperationTarget.DELETE);
            }
        }
    }

}