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
import org.eclipse.jface.text.TextViewerUndoManager;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.properties.tabbed.AbstractPropertySection;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.mysql.model.MySQLView;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.CustomSelectionProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.lang.reflect.InvocationTargetException;

/**
 * MySQLViewDefinitionSection
 */
public class MySQLViewDefinitionSection extends AbstractPropertySection {

    static final Log log = LogFactory.getLog(MySQLViewDefinitionSection.class);

    private final IDatabaseNodeEditor editor;
    private MySQLView view;
    private Composite parent;
    private SQLEditorBase sqlViewer;
    private StringEditorInput sqlEditorInput;
    private ISelectionProvider selectionProvider = new CustomSelectionProvider();
    private IAction actionDelete = new ActionDelete();
    private boolean visible;

    public MySQLViewDefinitionSection(IDatabaseNodeEditor editor)
    {
        this.editor = editor;
        this.view = (MySQLView) this.editor.getEditorInput().getDatabaseObject();
    }

    public void createControls(Composite parent, final TabbedPropertySheetPage tabbedPropertySheetPage)
    {
		super.createControls(parent, tabbedPropertySheetPage);
        this.parent = parent;
	}

    private void createEditor()
    {
        final ViewInitializer viewInitializer = new ViewInitializer();
        try {
            if (viewInitializer.isLazy()) {
                DBeaverCore.getInstance().runInProgressService(viewInitializer);
            } else {
                viewInitializer.run(VoidProgressMonitor.INSTANCE);
            }
        } catch (InvocationTargetException e) {
            log.error("Can't load view information", e.getTargetException());
        } catch (InterruptedException e) {
            // Skip
        }
        sqlViewer = new SQLEditorBase() {
            public DBPDataSource getDataSource()
            {
                return editor.getDataSource();
            }
        };
        sqlViewer.setRulerWidth(0);
        try {
            sqlEditorInput = new StringEditorInput("View", viewInitializer.definition, false);
            sqlViewer.init(editor.getEditorSite(), sqlEditorInput);
        } catch (PartInitException e) {
            UIUtils.showErrorDialog(parent.getShell(), "Create SQL viewer", null, e);
        }
        sqlViewer.createPartControl(parent);
        sqlViewer.reloadSyntaxRules();
        sqlViewer.doOperation(ISourceViewer.FORMAT);
        sqlViewer.doSave(VoidProgressMonitor.INSTANCE.getNestedMonitor());
        //sqlViewer.getTextViewer().setUndoManager(new NestedUndoManager());
        //sqlViewer.doRevertToSaved();
        parent.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                if (sqlViewer != null) {
                    sqlViewer.dispose();
                    sqlViewer = null;
                }
            }
        });
        sqlViewer.getDocument().addDocumentListener(new IDocumentListener() {
            public void documentAboutToBeChanged(DocumentEvent event)
            {
            }

            public void documentChanged(DocumentEvent event)
            {
                editor.getEditorInput().getPropertySource().setPropertyValue("definition", event.getDocument().get());
            }
        });
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
        visible = true;
    }

    @Override
    public void aboutToBeHidden()
    {
        if (sqlViewer != null) {
            sqlViewer.enableUndoManager(false);
        }
        final IActionBars actionBars = editor.getEditorSite().getActionBars();
        actionBars.setGlobalActionHandler(IWorkbenchCommandConstants.EDIT_DELETE, null);

        visible = false;
    }

    private class ViewInitializer implements DBRRunnableWithProgress {
        String definition;
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                definition = view.getAdditionalInfo(monitor).getDefinition();
            } catch (DBCException e) {
                definition = e.getMessage();
                throw new InvocationTargetException(e);
            }
            finally {
                if (definition == null) {
                    definition = "";
                }
            }
        }

        public boolean isLazy()
        {
            return !view.getAdditionalInfo().isLoaded();
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

    private class NestedUndoManager extends TextViewerUndoManager {

        public NestedUndoManager()
        {
            super(DBeaverCore.getInstance().getGlobalPreferenceStore().getInt(PrefConstants.TEXT_EDIT_UNDO_LEVEL));
        }

        @Override
        public boolean redoable()
        {
            return sqlViewer != null && visible && super.redoable();
        }

        @Override
        public boolean undoable()
        {
            return sqlViewer != null && visible && super.undoable();
        }
    }

}