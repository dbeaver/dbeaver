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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabankShadow;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * "Restore Shadow From Log" navigator action - {@code ALTER DATABANK "<shadow>" RESTORE USING LOG},
 * replaying LOGDB records into a shadow that has fallen behind its master so it can be brought
 * online again (the fix for the "Old version of the databank ... RESTORE" server error).
 *
 * @author Mimer Information Technology
 */
public class MimerShadowRestoreFromLogHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        final List<DBNDatabaseNode> nodes = MimerOnlineActionUtils.collectNodes(selection, MimerDatabankShadow.class);
        if (nodes.isEmpty()) {
            return null;
        }

        List<String> names = new ArrayList<>(nodes.size());
        for (DBNDatabaseNode node : nodes) {
            names.add(node.getObject().getName());
        }
        if (!UIUtils.confirmAction(
            HandlerUtil.getActiveShell(event),
            MimerUIMessages.action_shadow_restore_title,
            NLS.bind(MimerUIMessages.action_shadow_restore_message, String.join(", ", names)))) {
            return null;
        }

        List<String> statements = new ArrayList<>(names.size());
        for (String name : names) {
            statements.add("ALTER DATABANK \"" + name + "\" RESTORE USING LOG");
        }
        final List<DBNNode> parents = MimerOnlineActionUtils.parentsOf(nodes);
        MimerOnlineActionUtils.run(
            (MimerDataSource) nodes.get(0).getObject().getDataSource(),
            "Restore shadow from log",
            statements,
            false,
            () -> MimerOnlineActionUtils.refresh(parents));
        return null;
    }
}
