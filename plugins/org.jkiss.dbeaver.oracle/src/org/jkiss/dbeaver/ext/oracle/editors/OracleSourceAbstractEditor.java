/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.oracle.Activator;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceHost;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObject;
import org.jkiss.dbeaver.ext.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.text.BaseTextDocumentProvider;
import sun.plugin.util.UIUtil;
import sun.rmi.runtime.Log;

import java.lang.reflect.InvocationTargetException;

/**
 * Oracle source editor
 */
public abstract class OracleSourceAbstractEditor<T extends OracleSourceObject>
    extends SQLEditorBase
    implements IActiveWorkbenchPart, IRefreshablePart, OracleSourceHost
{

    private EditorPageControl pageControl;
    private IEditorInput lazyInput;
    private Table logTable;
    private Control editorControl;
    private SashForm editorSash;

    public OracleSourceAbstractEditor() {
        super();

        setDocumentProvider(new ObjectDocumentProvider());
    }

    @Override
    public IDatabaseNodeEditorInput getEditorInput() {
        return (IDatabaseNodeEditorInput)super.getEditorInput();
    }

    public T getObject()
    {
        return (T)getEditorInput().getDatabaseObject();
    }

    public OracleDataSource getDataSource() {
        return getObject().getDataSource();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        pageControl = new EditorPageControl(parent, SWT.NONE);

        editorSash = new SashForm(pageControl.createContentContainer(), SWT.VERTICAL | SWT.SMOOTH);
        super.createPartControl(editorSash);

        editorControl = editorSash.getChildren()[0];
        makeLogTable();

        pageControl.createProgressPanel();

        editorSash.setWeights(new int[] {70, 30});
        editorSash.setMaximizedControl(editorControl);
    }

    private void makeLogTable()
    {
        logTable = new Table(editorSash, SWT.SINGLE | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        logTable.setHeaderVisible(true);
        TableColumn messageColumn = UIUtils.createTableColumn(logTable, SWT.LEFT, "Message");
        TableColumn lineColumn = UIUtils.createTableColumn(logTable, SWT.LEFT, "Line");
        TableColumn posColumn = UIUtils.createTableColumn(logTable, SWT.LEFT, "Pos");
    }

    @Override
    public void init(IEditorSite site, IEditorInput input) throws PartInitException {
        lazyInput = input;
        setSite(site);
    }

    @Override
    public void doSave(IProgressMonitor progressMonitor) {
        if (lazyInput != null) {
            return;
        }
        super.doSave(progressMonitor);
    }

    public void activatePart() {
        if (lazyInput != null) {
            try {
                super.init(getEditorSite(), lazyInput);
                reloadSyntaxRules();

                pageControl.setInfo("State: " + getObject().getObjectState().getTitle());
                lazyInput = null;
            } catch (PartInitException e) {
                log.error(e);
            }
        }
    }

    public void deactivatePart() {
    }

    public void refreshPart(Object source, boolean force) {
        if (lazyInput == null && force) {
            try {
                super.init(getEditorSite(), getEditorInput());
                reloadSyntaxRules();
                setFocus();
            } catch (PartInitException e) {
                log.error(e);
            }
        }
    }

    private class ObjectDocumentProvider extends BaseTextDocumentProvider {
        @Override
        public boolean isReadOnly(Object element) {
            return false;
        }

        @Override
        public boolean isModifiable(Object element) {
            return true;
        }

        @Override
        protected IDocument createDocument(Object element) throws CoreException {
            final Document document = new Document();
/*
            try {
                String declaration = getObject().getSourceDeclaration(new DefaultProgressMonitor(getProgressMonitor()));
                if (declaration != null) {
                    document.set(declaration);
                }
            } catch (DBException e) {
                throw new CoreException(RuntimeUtils.makeExceptionStatus(e));
            }
*/
            try {
                DBeaverCore.getInstance().runInProgressService(new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            String sourceText = getSourceText(monitor);
                            if (sourceText != null) {
                                document.set(sourceText);
                            }
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                throw new CoreException(RuntimeUtils.makeExceptionStatus(e.getTargetException()));
            } catch (InterruptedException e) {
                // just skip it
            }

            return document;
        }

        @Override
        protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
            setSourceText(document.get());
        }

    }

    public Log getCompileLog()
    {
        return null;
    }

    public void setCompileInfo(String message, boolean error)
    {
        pageControl.setInfo(message);
    }

    public void positionSource(int line, int position)
    {
        try {
            final IRegion lineInfo = getTextViewer().getDocument().getLineInformation(line - 1);
            final int offset = lineInfo.getOffset() + position - 1;
            super.selectAndReveal(offset, 0);
            //textEditor.setFocus();
        } catch (BadLocationException e) {
            log.warn(e);
            // do nothing
        }
    }

    private void showCompileLog()
    {
        editorSash.setMaximizedControl(null);
        logTable.getColumn(0).setWidth(logTable.getBounds().width - 128);
        logTable.getColumn(1).setWidth(64);
        logTable.getColumn(2).setWidth(64);
    }

    protected abstract String getSourceText(DBRProgressMonitor monitor)
        throws DBException;

    protected abstract void setSourceText(String sourceText);

    private class EditorPageControl extends ProgressPageControl {

        private ToolBarManager toolBarManager;

        public EditorPageControl(Composite parent, int style)
        {
            super(parent, style);
        }

        @Override
        public void dispose()
        {
            toolBarManager.dispose();
            super.dispose();
        }

        @Override
        public Composite createProgressPanel(Composite container)
        {
            Composite infoGroup = super.createProgressPanel(container);

            toolBarManager = new ToolBarManager();

            toolBarManager.add(ActionUtils.makeCommandContribution(DBeaverCore.getActiveWorkbenchWindow(), ICommandIds.CMD_OPEN_FILE));
            toolBarManager.add(ActionUtils.makeCommandContribution(DBeaverCore.getActiveWorkbenchWindow(), ICommandIds.CMD_SAVE_FILE));
            toolBarManager.add(new Separator());
            toolBarManager.add(ActionUtils.makeCommandContribution(DBeaverCore.getActiveWorkbenchWindow(), OracleConstants.CMD_COMPILE));
            toolBarManager.add(new ViewLogAction());

            toolBarManager.createControl(infoGroup);

            return infoGroup;
        }
    }

    public class ViewLogAction extends Action
    {
        public ViewLogAction()
        {
            super("View compile log", Activator.getImageDescriptor("icons/commands/compile-log.png"));
        }

        public void run()
        {
            if (getTextViewer().getControl().isDisposed()) {
                return;
            }
            if (editorSash.getMaximizedControl() == null) {
                editorSash.setMaximizedControl(editorControl);
            } else {
                showCompileLog();
            }
        }

    }

}
