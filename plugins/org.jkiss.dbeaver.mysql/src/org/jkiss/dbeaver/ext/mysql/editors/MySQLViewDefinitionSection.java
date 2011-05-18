/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.editors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
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
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;

import java.lang.reflect.InvocationTargetException;

/**
 * MySQLViewDefinitionSection
 */
public class MySQLViewDefinitionSection extends AbstractPropertySection {

    static final Log log = LogFactory.getLog(MySQLViewDefinitionSection.class);

    private final IDatabaseNodeEditor editor;
    private MySQLView view;
    private Composite parent;
    private IWorkbenchPart activePart;
    private SQLEditorBase sqlViewer;
    private StringEditorInput sqlEditorInput;

    public MySQLViewDefinitionSection(IDatabaseNodeEditor editor)
    {
        this.editor = editor;
        this.view = (MySQLView) this.editor.getEditorInput().getDatabaseObject();
    }

    public void createControls(Composite parent, final TabbedPropertySheetPage tabbedPropertySheetPage)
    {
		super.createControls(parent, tabbedPropertySheetPage);
        this.parent = parent;
        this.activePart = tabbedPropertySheetPage.getSite().getPage().getActivePart();
	}

    private void createEditor()
    {
        final ViewInitializer viewInitializer = new ViewInitializer();
        try {
            DBeaverCore.getInstance().runInProgressService(viewInitializer);
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
            sqlViewer.init(new SubEditorSite(activePart.getSite()), sqlEditorInput);
        } catch (PartInitException e) {
            UIUtils.showErrorDialog(parent.getShell(), "Create SQL viewer", null, e);
        }
        sqlViewer.createPartControl(parent);
        sqlViewer.reloadSyntaxRules();
        sqlViewer.formatSQL();
        parent.addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e)
            {
                if (sqlViewer != null) {
                    sqlViewer.dispose();
                    sqlViewer = null;
                }
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
    }

    @Override
    public void aboutToBeHidden()
    {

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
    }
}