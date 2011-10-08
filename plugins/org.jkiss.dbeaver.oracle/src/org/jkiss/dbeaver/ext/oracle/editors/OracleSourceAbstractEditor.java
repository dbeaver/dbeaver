/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.ext.oracle.model.OracleSourceObject;
import org.jkiss.dbeaver.ext.ui.IActiveWorkbenchPart;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.text.BaseTextDocumentProvider;

import java.lang.reflect.InvocationTargetException;

/**
 * Oracle source editor
 */
public abstract class OracleSourceAbstractEditor<T extends OracleSourceObject> extends SQLEditorBase implements IActiveWorkbenchPart, IRefreshablePart {

    private IEditorInput lazyInput;

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

    public DBPDataSource getDataSource() {
        return getEditorInput().getDataSource();
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
                lazyInput = null;
            } catch (PartInitException e) {
                log.error(e);
            }
        }
    }

    public void deactivatePart() {
    }

    public void refreshPart(Object source) {
        if (lazyInput == null) {
            try {
                super.init(getEditorSite(), getEditorInput());
                reloadSyntaxRules();
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

    protected abstract String getSourceText(DBRProgressMonitor monitor)
        throws DBException;

    protected abstract void setSourceText(String sourceText);

}
