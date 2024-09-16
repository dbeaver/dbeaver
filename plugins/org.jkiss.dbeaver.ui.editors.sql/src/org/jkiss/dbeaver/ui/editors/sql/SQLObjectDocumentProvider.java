/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.text.BaseTextDocumentProvider;
import org.jkiss.dbeaver.ui.editors.text.DatabaseMarkerAnnotationModel;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;

public abstract class SQLObjectDocumentProvider extends BaseTextDocumentProvider {

    private String sourceText;
    private boolean sourceLoaded;

    private final SQLEditorBase editor;

    public SQLObjectDocumentProvider(SQLEditorBase editor) {
        this.editor = editor;
    }

    protected abstract String loadSourceText(DBRProgressMonitor monitor) throws DBException;

    protected abstract void saveSourceText(DBRProgressMonitor monitor, String text) throws DBException;

    protected DBSObject getProviderObject() {
        return null;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    @Override
    public boolean isReadOnly(Object element) {
        return false;
    }

    @Override
    public boolean isModifiable(Object element) {
        return !editor.isReadOnly();
    }

    @Override
    protected IDocument createDocument(Object element) {
        final Document document = new Document();
        if (element instanceof StringEditorInput) {
            sourceText = ((StringEditorInput) element).getBuffer().toString();
        }
        if (sourceText == null) {
            DBPDataSource dataSource = editor.getDataSource();
            if (dataSource != null) {
                sourceText = SQLUtils.generateCommentLine(editor.getDataSource(), "Loading '" + editor.getEditorInput().getName() + "' source...");
                document.set(sourceText);
            }

            AbstractJob job = new AbstractJob("Load SQL source") {
                {
                    setUser(true);
                }

                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    monitor.beginTask(getName(), 1);
                    try {
                        if (dataSource != null) {
                            DBExecUtils.tryExecuteRecover(monitor, dataSource, param -> {
                                try {
                                    sourceText = loadSourceText(monitor);
                                    if (sourceText == null) {
                                        sourceText = SQLUtils.generateCommentLine(dataSource, "Empty source");
                                    }
                                } catch (DBException e) {
                                    throw new InvocationTargetException(e);
                                }
                            });
                        } else {
                            sourceText = loadSourceText(monitor);
                        }
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        SQLEditorBase.log.error(e);
                        sourceText = "/* ERROR WHILE READING SOURCE:\n\n" + e.getMessage() + "\n*/";
                        return Status.CANCEL_STATUS;
                    } finally {
                        monitor.done();
                    }
                }
            };
            job.addJobChangeListener(new JobChangeAdapter() {
                @Override
                public void done(IJobChangeEvent event) {
                    UIUtils.asyncExec(() -> {
                        editor.setInput(editor.getEditorInput());
                        editor.reloadSyntaxRules();
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
        DBSObject databaseObject = getProviderObject();
        if (databaseObject != null) {
            DBNDatabaseNode node = DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(databaseObject);
            IResource resource = node == null || !(node.getOwnerProject() instanceof RCPProject rcpProject) ? null : rcpProject.getEclipseProject();
            if (resource != null) {
                return new DatabaseMarkerAnnotationModel(databaseObject, node, resource);
            }
        }
        return super.createAnnotationModel(element);
    }

    @Override
    protected void doSaveDocument(IProgressMonitor monitor, Object element, IDocument document, boolean overwrite) throws CoreException {
        DBRProgressMonitor pm = RuntimeUtils.makeMonitor(monitor);
        pm.beginTask("Save nested editor", 1);
        try {
            saveSourceText(pm, document.get());
        } catch (DBException e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        } finally {
            pm.done();
        }
    }

    public boolean isSourceLoaded() {
        return sourceLoaded;
    }
}
