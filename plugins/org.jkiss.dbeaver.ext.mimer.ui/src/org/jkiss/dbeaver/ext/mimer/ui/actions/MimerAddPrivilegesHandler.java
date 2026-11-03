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
package org.jkiss.dbeaver.ext.mimer.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerObjectPrivilege;
import org.jkiss.dbeaver.ext.mimer.ui.config.MimerIdentPickerUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;

import java.util.ArrayList;
import java.util.List;

/**
 * "Add Privileges" navigator action - lets the user multi-select several tables/views and
 * {@code GRANT} the same privilege on all of them to one grantee in a single run, instead of
 * having to open each table's own Privileges folder and repeat "Create New Privilege" one at a
 * time. Requested by the user as a natural extension of the existing per-table grant flow;
 * built as its own navigator action (same shape as {@link MimerSetOnlineStateHandler} and
 * friends) rather than through the {@code SQLObjectEditor} create framework, since that
 * framework only ever creates one object against one container at a time - there's no hook for
 * "the same new object, once per several containers".
 * <p>
 * Mimer SQL addresses both tables and views with {@code GRANT ... ON TABLE} (no separate
 * {@code ON VIEW} form - see {@link MimerObjectPrivilege}), so the selection is filtered to
 * {@link GenericTableBase} broadly rather than {@code MimerTable}/{@code MimerView}
 * individually - every table/view in this plugin's tree is one or the other anyway (see
 * {@code MimerMetaModel#createTableOrViewImpl}), so a mixed table+view selection is allowed.
 *
 * @author Mimer Information Technology
 */
public class MimerAddPrivilegesHandler extends AbstractHandler {

    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        final List<DBNDatabaseNode> nodes = MimerOnlineActionUtils.collectNodes(selection, GenericTableBase.class);
        if (nodes.isEmpty()) {
            return null;
        }

        MimerDataSource dataSource = (MimerDataSource) nodes.get(0).getObject().getDataSource();
        List<String> tableNames = new ArrayList<>(nodes.size());
        for (DBNDatabaseNode node : nodes) {
            tableNames.add(node.getObject().getName());
        }

        MimerAddPrivilegesDialog dialog = new MimerAddPrivilegesDialog(
            HandlerUtil.getActiveShell(event), tableNames, MimerIdentPickerUtils.loadIdentNames(dataSource));
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        List<String> statements = new ArrayList<>(nodes.size());
        for (DBNDatabaseNode node : nodes) {
            GenericTableBase table = (GenericTableBase) node.getObject();
            MimerObjectPrivilege privilege = new MimerObjectPrivilege(table, dialog.getGrantee(), dialog.getPrivilegeType());
            privilege.setGrantable(dialog.isGrantable());
            statements.add(privilege.buildGrantDDL());
        }

        MimerOnlineActionUtils.run(
            dataSource,
            "Add privileges",
            statements,
            false,
            () -> MimerOnlineActionUtils.refresh(nodes));
        return null;
    }
}
