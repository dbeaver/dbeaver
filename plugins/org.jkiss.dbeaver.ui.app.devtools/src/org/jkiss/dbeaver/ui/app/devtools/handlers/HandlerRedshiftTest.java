/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.devtools.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayList;
import java.util.List;

public class HandlerRedshiftTest extends AbstractHandler {


    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (HandlerUtil.getCurrentSelection(event) instanceof IStructuredSelection selection) {
            if (selection.getFirstElement() instanceof DBNDatabaseNode databaseNode) {
                try {
                    if (databaseNode != null
                        && databaseNode.getDataSource() != null
                        && databaseNode.getDataSource().getContainer().getDriver().getProviderId().equals("redshift")) {

                        test(databaseNode.getDataSource());
                    } else {
                        DBWorkbench.getPlatformUI().showWarningNotification("Warning", "Please select connected Redshift connection");
                    }
                } catch (DBCException e) {
                    throw new ExecutionException("", e);
                }
            }
        }
        return null;

    }

    private void test(DBPDataSource dataSource) throws DBCException {
        try (final DBCSession session = DBUtils.openUtilSession(new VoidProgressMonitor(), dataSource, "test")) {
            System.out.println("Run tests");
            List<Thread> startedThreads = startThreads(session);

            for (Thread thread : startedThreads) {
                thread.join();
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Finished");

    }

    @NotNull
    private static List<Thread> startThreads(DBCSession session) {
        List<Thread> startedThreads = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            int finalI = i;
            Thread thread = new Thread("Thread " + finalI) {
                @Override
                public void run() {
                    try {
                        runQuery(session, finalI);
                    } catch (DBException e) {
                        e.printStackTrace();
                    }
                }
            };
            startedThreads.add(thread);
            thread.start();
        }
        return startedThreads;
    }

    private static void runQuery(DBCSession session, int index) throws DBException {
        //String query = "SELECT u.* FROM public.users AS u LIMIT 2 OFFSET %s".formatted(index * 2);
        String query = "SELECT 'hello world! - " + index + "'";

        System.out.println("Run for thread " + index);
        try (var stat = prepareStatement(session, query)) {

            if (stat.executeStatement()) {
                try (final DBCResultSet results1 = stat.openResultSet()) {
                    while (results1.nextRow()) {
                        System.out.println(results1.getAttributeValue(0));
                    }
                }
            }

        }
        System.out.println("\tFinished for thread " + index);
    }

    private static DBCStatement prepareStatement(DBCSession session, String query) throws DBCException {
        return session.prepareStatement(DBCStatementType.EXEC, query, true, false, false);
    }


}
