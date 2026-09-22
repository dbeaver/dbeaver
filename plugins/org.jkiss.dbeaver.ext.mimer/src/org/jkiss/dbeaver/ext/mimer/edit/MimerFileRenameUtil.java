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
package org.jkiss.dbeaver.ext.mimer.edit;

import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mimer.internal.MimerMessages;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

/**
 * Every Mimer SQL statement that changes a databank/shadow file name in the data dictionary
 * ({@code ALTER DATABANK ... SET FILE}, {@code ... ALTER FILE ... SET FILE}, {@code ALTER SHADOW
 * ... INTO}) only touches the dictionary - the file on the database server's file system must
 * already have been renamed/moved to match. This wraps such an action in a confirmation the user
 * must accept before it runs; declining throws to abort the save so nothing is executed.
 *
 * @author Mimer Information Technology
 */
final class MimerFileRenameUtil {

    private MimerFileRenameUtil() {
    }

    @NotNull
    static SQLDatabasePersistAction confirmedRenameAction(
        @NotNull String title,
        @NotNull String script,
        @Nullable String oldFile,
        @NotNull String newFile,
        @Nullable Runnable onSuccess
    ) {
        return new SQLDatabasePersistAction(title, script) {
            @Override
            public void beforeExecute(@NotNull DBCSession session) throws DBCException {
                String transition = CommonUtils.isEmptyTrimmed(oldFile)
                    ? "'" + newFile + "'"
                    : "'" + oldFile + "'  →  '" + newFile + "'";
                boolean confirmed = DBWorkbench.getPlatformUI().confirmAction(
                    MimerMessages.action_file_rename_title,
                    NLS.bind(MimerMessages.action_file_rename_message, transition),
                    true);
                if (!confirmed) {
                    throw new DBCException("Cancelled - the file name was not changed.");
                }
                super.beforeExecute(session);
            }

            @Override
            public void afterExecute(@NotNull DBCSession session, @Nullable Throwable error) {
                if (error == null && onSuccess != null) {
                    onSuccess.run();
                }
            }
        };
    }
}
