/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNodeHandler;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * EntityHyperlink
 */
public class EntityHyperlink implements IHyperlink
{
    private IWorkbenchSite site;
    private IRegion region;
    private DBSObjectReference reference;

    public EntityHyperlink(IWorkbenchSite site, DBSObjectReference reference, IRegion region)
    {
        this.site = site;
        this.reference = reference;
        this.region = region;
    }

    @Override
    public IRegion getHyperlinkRegion()
    {
        return region;
    }

    @Override
    public String getTypeLabel()
    {
        return null;
    }

    @Override
    public String getHyperlinkText()
    {
        return DBUtils.getObjectFullName(reference, DBPEvaluationContext.UI);
    }

    @Override
    public void open()
    {
        new ObjectFinder().schedule();
    }

    @NotNull
    protected String getNavigationErrorMessage(@Nullable DBException error) {
        String displayName = getHyperlinkText();
        return error == null
            ? NLS.bind(UINavigatorMessages.editors_entity_hyperlink_unavailable_message, displayName)
            : NLS.bind(UINavigatorMessages.editors_entity_hyperlink_error_message,
                displayName, GeneralUtils.makeStandardErrorMessage(error));
    }

    protected boolean isNavigationAvailable(@Nullable DBPDataSource dataSource) {
        Shell shell = site.getShell();
        IWorkbenchWindow window = site.getWorkbenchWindow();
        if (shell == null || shell.isDisposed() || window == null || window.getActivePage() == null) {
            return false;
        }
        // References to non-database objects need not have a data source.
        if (dataSource == null) {
            return true;
        }
        DBPDataSourceContainer container = dataSource.getContainer();
        return container.isConnected() && container.getDataSource() == dataSource;
    }

    static boolean isNavigationCanceled(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBPDataSource dataSource,
        @Nullable Throwable error
    ) {
        if (monitor.isCanceled()) {
            return true;
        }
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof OperationCanceledException) {
                return true;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return error != null && DBExecUtils.isExecutionCanceled(dataSource, error);
    }

    private class ObjectFinder extends AbstractJob {

        private final DBPDataSource dataSource;

        protected ObjectFinder() {
            super("Find object node by reference");
            dataSource = reference.getContainer().getDataSource();
        }

        @NotNull
        @Override
        public IStatus run(@NotNull DBRProgressMonitor monitor)
        {
            monitor.beginTask("Resolve object " + reference.getName(), 1);
            try {
                if (isNavigationCanceled(monitor, dataSource, null) || !isNavigationAvailable(dataSource)) {
                    return Status.CANCEL_STATUS;
                }
                DBSObject object = reference.resolveObject(monitor);
                if (object instanceof DBSAlias) {
                    object = ((DBSAlias) object).getTargetObject(monitor);
                }
                if (isNavigationCanceled(monitor, dataSource, null) || !isNavigationAvailable(dataSource)) {
                    return Status.CANCEL_STATUS;
                }
                DBNDatabaseNode node = DBNUtils.getNodeByObject(monitor, object, true);
                return scheduleResult(monitor, node, null);
            } catch (OperationCanceledException e) {
                return Status.CANCEL_STATUS;
            } catch (DBException e) {
                return scheduleResult(monitor, null, e);
            } finally {
                monitor.done();
            }
        }

        @NotNull
        private IStatus scheduleResult(
            @NotNull DBRProgressMonitor monitor,
            @Nullable DBNDatabaseNode node,
            @Nullable DBException error
        ) {
            if (isNavigationCanceled(monitor, dataSource, error) || !isNavigationAvailable(dataSource)) {
                return Status.CANCEL_STATUS;
            }
            UIUtils.asyncExec(() -> {
                if (isNavigationCanceled(monitor, dataSource, error) || !isNavigationAvailable(dataSource)) {
                    return;
                }
                if (error != null) {
                    DBWorkbench.getPlatformUI().showError(
                        UINavigatorMessages.editors_entity_hyperlink_error_title,
                        getNavigationErrorMessage(error),
                        error);
                } else if (node == null) {
                    DBWorkbench.getPlatformUI().showError(
                        UINavigatorMessages.editors_entity_hyperlink_error_title,
                        getNavigationErrorMessage(null));
                } else if (!node.isDisposed()) {
                    NavigatorUtils.executeNodeAction(DBXTreeNodeHandler.Action.open, node, site);
                }
            });
            return Status.OK_STATUS;
        }
    }
}
