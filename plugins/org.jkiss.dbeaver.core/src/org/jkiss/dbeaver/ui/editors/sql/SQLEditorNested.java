/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017-2018 Alexander Fedorov (alexander.fedorov@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLog;
import org.jkiss.dbeaver.model.exec.compile.DBCSourceHost;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorUtils;
import org.jkiss.dbeaver.ui.editors.text.DatabaseMarkerAnnotationModel;
import org.jkiss.dbeaver.runtime.resource.WorkspaceResources;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ObjectCompilerLogViewer;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.text.BaseTextDocumentProvider;
import org.jkiss.dbeaver.utils.RuntimeUtils;

/**
 * SQLEditorNested
 */
public abstract class SQLEditorNested<T extends DBSObject>
    extends SQLEditorBase
    implements IActiveWorkbenchPart, IRefreshablePart, DBCSourceHost
{

    private EditorPageControl pageControl;
    private ObjectCompilerLogViewer compileLog;
    private Control editorControl;
    private SashForm editorSash;
    private boolean activated;

    public SQLEditorNested() {
        super();

        setDocumentProvider(new ObjectDocumentProvider());
        //setHasVerticalRuler(false);
    }

    public IDatabaseEditorInput getDatabaseEditorInput() {
        return (IDatabaseEditorInput)super.getEditorInput();
    }

    @Override
    public T getSourceObject()
    {
        IEditorInput editorInput = getEditorInput();
        if (!(editorInput instanceof IDatabaseEditorInput)) {
            return null;
        }
        return (T) ((IDatabaseEditorInput)editorInput).getDatabaseObject();
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        IEditorInput editorInput = getEditorInput();
        if (!(editorInput instanceof IDatabaseEditorInput)) {
            return null;
        }
        return ((IDatabaseEditorInput)editorInput).getExecutionContext();
    }

    public DBPPropertySource getInputPropertySource() {
        return getDatabaseEditorInput().getPropertySource();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        pageControl = new EditorPageControl(parent, SWT.SHEET);

        boolean hasCompiler = getCompileCommandId() != null;

        if (hasCompiler) {
            editorSash = new SashForm(pageControl.createContentContainer(), SWT.VERTICAL | SWT.SMOOTH);
            super.createPartControl(editorSash);

            editorControl = editorSash.getChildren()[0];
            compileLog = new ObjectCompilerLogViewer(editorSash, false);
        } else {
            super.createPartControl(pageControl.createContentContainer());
        }

        // Create new or substitute progress control
        pageControl.createOrSubstituteProgressPanel(getSite());
        pageControl.setInfo("Source");

        if (hasCompiler) {
            editorSash.setWeights(new int[]{70, 30});
            editorSash.setMaximizedControl(editorControl);
        }

        // Use focus to activate page control
        final Control editorControl = getEditorControl();
        assert editorControl != null;
        editorControl.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (pageControl != null && !pageControl.isDisposed()) {
                    pageControl.activate(true);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (pageControl != null && !pageControl.isDisposed()) {
                    pageControl.activate(false);
                }
            }
        });
    }

    @Override
    public void doSave(final IProgressMonitor progressMonitor) {
        UIUtils.syncExec(() -> SQLEditorNested.super.doSave(progressMonitor));
    }

    @Override
    public void activatePart() {
        if (!activated) {
            reloadSyntaxRules();
            activated = true;
        }
    }

    @Override
    public void deactivatePart() {
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        // Check if we are in saving process
        // If so then no refresh needed (source text was updated during save)
        IEditorSite editorSite = getEditorSite();
        if (editorSite instanceof MultiPageEditorSite &&
            ((MultiPageEditorSite) editorSite).getMultiPageEditor() instanceof EntityEditor &&
            ((EntityEditor) ((MultiPageEditorSite) editorSite).getMultiPageEditor()).isSaveInProgress())
        {
            return;
        }

        final IDocumentProvider documentProvider = getDocumentProvider();
        if (documentProvider instanceof SQLEditorNested.ObjectDocumentProvider) {
            ((SQLEditorNested.ObjectDocumentProvider) documentProvider).sourceText = null;
        }
        if (force) {
            int caretOffset = getEditorControl().getCaretOffset();
            super.setInput(getEditorInput());
            // Try to keep cursor position
            if (caretOffset < getEditorControl().getCharCount()) {
                getEditorControl().setCaretOffset(caretOffset);
            }
        }
        reloadSyntaxRules();
    }

    protected String getCompileCommandId()
    {
        return null;
    }

    public boolean isDocumentLoaded() {
        final IDocumentProvider documentProvider = getDocumentProvider();
        if (documentProvider instanceof SQLEditorNested.ObjectDocumentProvider) {
            return ((SQLEditorNested.ObjectDocumentProvider) documentProvider).sourceLoaded;
        }
        return true;
    }

    private class ObjectDocumentProvider extends BaseTextDocumentProvider {

        private String sourceText;
        private boolean sourceLoaded;

        @Override
        public boolean isReadOnly(Object element) {
            return SQLEditorNested.this.isReadOnly();
        }

        @Override
        public boolean isModifiable(Object element) {
            return !SQLEditorNested.this.isReadOnly();
        }

        @Override
        protected IDocument createDocument(Object element) throws CoreException {
            final Document document = new Document();

            if (sourceText == null) {
                sourceText = SQLUtils.generateCommentLine(getDataSource(), "Loading '" + getEditorInput().getName() + "' source...");
                document.set(sourceText);

                AbstractJob job = new AbstractJob("Load SQL source") {
                    {
                        setUser(true);
                    }
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        try {
                            sourceText = getSourceText(monitor);
                            if (sourceText == null) {
                                sourceText = SQLUtils.generateCommentLine(getDataSource(), "Empty source");
                            }
                            return Status.OK_STATUS;
                        } catch (Exception e) {
                            sourceText = "/* ERROR WHILE READING SOURCE:\n\n" + e.getMessage() + "\n*/";
                            return Status.CANCEL_STATUS;
                        }
                    }
                };
                job.addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(IJobChangeEvent event) {
                        UIUtils.asyncExec(() -> {
                            SQLEditorNested.this.setInput(getEditorInput());
                            SQLEditorNested.this.reloadSyntaxRules();
                        });
                        super.done(event);
                    }
                });
                job.schedule();
            }
            // Set text
            document.set(sourceText);
            sourceLoaded = true;

            return document;
        }

        @Override
        protected IAnnotationModel createAnnotationModel(Object element) throws CoreException {
            DBSObject databaseObject = getSourceObject();
            DBNDatabaseNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(databaseObject);
            IResource resource = WorkspaceResources.resolveWorkspaceResource(databaseObject);
            if (resource != null) {
                return new DatabaseMarkerAnnotationModel(databaseObject, node, resource);
            }
            return super.createAnnotationModel(element);
        }
        
        @Override
        protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
            setSourceText(RuntimeUtils.makeMonitor(monitor), document.get());
        }
    }

    @Override
    public DBCCompileLog getCompileLog()
    {
        return compileLog;
    }

    @Override
    public void setCompileInfo(String message, boolean error)
    {
        pageControl.setInfo(message);
    }

    @Override
    public void positionSource(int line, int position)
    {
        try {
            final IRegion lineInfo = getTextViewer().getDocument().getLineInformation(line - 1);
            final int offset = lineInfo.getOffset() + position - 1;
            super.selectAndReveal(offset, 1);
            //textEditor.setFocus();
        } catch (BadLocationException e) {
            log.warn(e);
            // do nothing
        }
    }

    @Override
    public void showCompileLog()
    {
        editorSash.setMaximizedControl(null);
        compileLog.layoutLog();
    }

    protected abstract String getSourceText(DBRProgressMonitor monitor)
        throws DBException;

    protected abstract void setSourceText(DBRProgressMonitor monitor, String sourceText);

    protected void contributeEditorCommands(IContributionManager toolBarManager)
    {
        toolBarManager.add(ActionUtils.makeCommandContribution(getSite().getWorkbenchWindow(), CoreCommands.CMD_OPEN_FILE));
        toolBarManager.add(ActionUtils.makeCommandContribution(getSite().getWorkbenchWindow(), CoreCommands.CMD_SAVE_FILE));
        String compileCommandId = getCompileCommandId();
        if (compileCommandId != null) {
            toolBarManager.add(new Separator());
            //toolBarManager.add(ActionUtils.makeCommandContribution(getSite().getWorkbenchWindow(), compileCommandId));
            toolBarManager.add(new ViewLogAction());
        }
    }

    @Override
    public void editorContextMenuAboutToShow(IMenuManager menu) {
        super.editorContextMenuAboutToShow(menu);
        menu.add(new Separator());
        contributeEditorCommands(menu);
    }

    @Override
    public void doSaveAs() {
        saveToExternalFile();
    }

    private class EditorPageControl extends ProgressPageControl {

        EditorPageControl(Composite parent, int style)
        {
            super(parent, style);
        }

        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            contributeEditorCommands(contributionManager);
            IWorkbenchPartSite site = getSite();
            if (site != null) {
                DatabaseEditorUtils.contributeStandardEditorActions(site, contributionManager);
            }
        }
    }

    public class ViewLogAction extends Action
    {
        ViewLogAction()
        {
            super("View compile log", DBeaverIcons.getImageDescriptor(UIIcon.COMPILE_LOG)); //$NON-NLS-2$
        }

        @Override
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
