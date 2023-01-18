/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceProviderSynchronizable;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.MessageBoxBuilder;
import org.jkiss.dbeaver.ui.dialogs.Reply;
import org.jkiss.dbeaver.utils.GeneralUtils;

public class DataSourceSynchronizeHandler extends AbstractDataSourceHandler {
    private static final Reply REPLY_KEEP_LOCAL_CHANGES = new Reply(CoreMessages.dialog_data_source_synchronize_reply_keep_local_label);
    private static final Reply REPLY_KEEP_REMOTE_CHANGES = new Reply(CoreMessages.dialog_data_source_synchronize_reply_keep_remote_label);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final DBCExecutionContext context = getActiveExecutionContext(event, false);

        if (context == null) {
            return null;
        }

        final var dataSource = context.getDataSource();
        final var dataSourceContainer = dataSource.getContainer();
        final var dataSourceProvider = (DBPDataSourceProviderSynchronizable) dataSourceContainer.getDriver().getDataSourceProvider();

        new AbstractJob("Synchronize data source [" + dataSource.getName() + "]") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {

                try {
                    final boolean localSynchronized = dataSourceProvider.isLocalDataSourceSynchronized(monitor, dataSourceContainer);
                    final boolean remoteSynchronized = dataSourceProvider.isRemoteDataSourceSynchronized(monitor, dataSourceContainer);

                    if (localSynchronized != remoteSynchronized) {
                        final Reply[] reply = new Reply[1];

                        UIUtils.syncExec(() -> reply[0] = MessageBoxBuilder.builder()
                            .setTitle(CoreMessages.dialog_data_source_synchronize_title)
                            .setMessage(NLS.bind(CoreMessages.dialog_data_source_synchronize_message, dataSource.getName()))
                            .setPrimaryImage(DBIcon.STATUS_QUESTION)
                            .setReplies(REPLY_KEEP_LOCAL_CHANGES, REPLY_KEEP_REMOTE_CHANGES, Reply.CANCEL)
                            .setDefaultReply(Reply.CANCEL)
                            .showMessageBox());

                        if (reply[0] == REPLY_KEEP_LOCAL_CHANGES) {
                            dataSourceProvider.syncRemoteDataSource(monitor, dataSourceContainer);
                        } else if (reply[0] == REPLY_KEEP_REMOTE_CHANGES) {
                            dataSourceProvider.syncLocalDataSource(monitor, dataSourceContainer);
                            DataSourceInvalidateHandler.invalidateDataSource(dataSourceContainer.getDataSource());
                        }
                    }

                    return Status.OK_STATUS;
                } catch (DBException e) {
                    return GeneralUtils.makeExceptionStatus(e);
                }
            }
        }.schedule();

        return null;
    }
}
