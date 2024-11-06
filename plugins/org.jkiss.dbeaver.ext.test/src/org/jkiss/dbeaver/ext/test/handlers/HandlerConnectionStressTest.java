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
package org.jkiss.dbeaver.ext.test.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class HandlerConnectionStressTest extends AbstractHandler {
    private static final int CONNECTIONS_COUNT = 50;
    private static final int COLUMNS_COUNT = 10;
    private static final String DUMMY_SQL =
        """
            with recursive all_rows as (
                select *
                from (values({x1},{x1}),({x2},{x2}),({x3},{x3}),({x4},{x4}),({x5},{x5})) as t_name(id, quantity)
            ),
            sub_select AS (
                select id as rows_id
                from all_rows AS data1
                UNION ALL\s
                select id as sub_id
                from sub_select
                JOIN all_rows on 1=1
            )
             \s
            SELECT rows_id,rows_id,rows_id,rows_id,rows_id,rows_id,rows_id,rows_id,rows_id,rows_id
            FROM sub_select\s
            limit {max};
            """;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        String connectionsCount = EnterNameDialog.chooseName(UIUtils.getActiveWorkbenchShell(),
            "Number of connections",
            "");
        if (CommonUtils.isEmpty(connectionsCount)) {
            connectionsCount = String.valueOf(CONNECTIONS_COUNT);
        }
        int maxConnections = Integer.parseInt(connectionsCount);

        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DBNDatabaseNode) {
                stressTest(((DBNDatabaseNode) element).getDataSource(), maxConnections);
            }
        }
        return null;
    }

    private void stressTest(DBPDataSource dataSource, int maxConnections) {
        var contexts = new ArrayList<DBCExecutionContext>();
        try {
            IntStream.range(0, maxConnections).parallel()
                .forEach(i -> {
                        try {
                            contexts.add(dataSource.getDefaultInstance()
                                .openIsolatedContext(new VoidProgressMonitor(), "Stress test", null));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                );

            contexts.stream().parallel()
                .forEach(context -> {
                    try (DBCSession session = context.openSession(new VoidProgressMonitor(),
                        DBCExecutionPurpose.USER_SCRIPT,
                        "Stress test")) {
                        int x1 = ThreadLocalRandom.current().nextInt(0, 100);
                        int x2 = ThreadLocalRandom.current().nextInt(0, 100);
                        int x3 = ThreadLocalRandom.current().nextInt(0, 100);
                        int x4 = ThreadLocalRandom.current().nextInt(0, 100);
                        int x5 = ThreadLocalRandom.current().nextInt(0, 100);
                        int max = ThreadLocalRandom.current().nextInt(1000, 100_000);
                        var sql = DUMMY_SQL.replace("{x1}", String.valueOf(x1))
                            .replace("{x2}", String.valueOf(x2))
                            .replace("{x3}", String.valueOf(x3))
                            .replace("{x4}", String.valueOf(x4))
                            .replace("{x5}", String.valueOf(x5))
                            .replace("{max}", String.valueOf(max));
                        try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.EXEC,
                            sql,
                            true,
                            false,
                            false)) {
                            if (dbStat.executeStatement()) {
                                try (final DBCResultSet dbResult = dbStat.openResultSet()) {
                                    int rowCount = 0;
                                    while (dbResult.nextRow()) {
                                        var row = new StringBuilder("|");
                                        for (int i = 0; i < COLUMNS_COUNT; i++) {
                                            row.append(dbResult.getAttributeValue(i))
                                                .append("|");
                                        }
                                        row.append(" - ")
                                            .append(++rowCount)
                                            .append(" " + session.getExecutionContext().getContextId());
//                                        System.out.println(row);
                                    }
                                }
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            contexts.forEach(DBCExecutionContext::close);
        }
    }
}