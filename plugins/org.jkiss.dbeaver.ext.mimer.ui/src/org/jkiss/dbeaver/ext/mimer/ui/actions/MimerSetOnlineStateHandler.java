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
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabank;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabankShadow;
import org.jkiss.dbeaver.ext.mimer.model.MimerUtils;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayList;
import java.util.List;

/**
 * "Set Online State" navigator action for Mimer SQL databanks and shadows - runs
 * {@code SET DATABANK/SHADOW "a", "b" OFFLINE | ONLINE PRESERVE LOG | ONLINE RESET LOG}
 * (its own statement family, not {@code ALTER}). Works on a multi-selection as long as it's
 * homogeneous (all databanks or all shadows) and every object is in the same online state, so
 * the offered choices are unambiguous. This complements the read-only "Online" checkbox in the
 * properties view.
 *
 * @author Mimer Information Technology
 */
public class MimerSetOnlineStateHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);

        List<DBNDatabaseNode> shadowNodes = MimerOnlineActionUtils.collectNodes(selection, MimerDatabankShadow.class);
        final List<DBNDatabaseNode> nodes = shadowNodes.isEmpty()
            ? MimerOnlineActionUtils.collectNodes(selection, MimerDatabank.class)
            : shadowNodes;
        final String keyword = shadowNodes.isEmpty() ? "DATABANK" : "SHADOW";
        if (nodes.isEmpty()) {
            DBWorkbench.getPlatformUI().showMessageBox(
                MimerUIMessages.action_set_online_state_title,
                MimerUIMessages.action_set_online_state_mixed_types_message,
                true);
            return null;
        }

        final List<DBSObject> targets = new ArrayList<>(nodes.size());
        List<String> names = new ArrayList<>(nodes.size());
        boolean anyOnline = false;
        boolean anyOffline = false;
        for (DBNDatabaseNode node : nodes) {
            DBSObject object = node.getObject();
            targets.add(object);
            names.add(object.getName());
            if (isOnline(object)) {
                anyOnline = true;
            } else {
                anyOffline = true;
            }
        }
        if (anyOnline && anyOffline) {
            DBWorkbench.getPlatformUI().showMessageBox(
                MimerUIMessages.action_set_online_state_title,
                NLS.bind(MimerUIMessages.action_set_online_state_mixed_status_message, keyword.toLowerCase()),
                true);
            return null;
        }

        String[] states = MimerUtils.onlineStatesFor(anyOnline);
        MimerSetOnlineStateDialog dialog = new MimerSetOnlineStateDialog(
            HandlerUtil.getActiveShell(event), keyword, names, states);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }
        String state = dialog.getSelectedState();
        final boolean nowOnline = MimerUtils.stateGoesOnline(state);

        MimerDataSource dataSource = (MimerDataSource) targets.get(0).getDataSource();
        String sql = MimerUtils.buildSetOnlineDDL(keyword, names, state);
        if (sql == null) {
            return null;
        }
        MimerOnlineActionUtils.run(
            dataSource,
            "Set " + keyword.toLowerCase() + " online state",
            List.of(sql),
            true,
            () -> {
                for (DBSObject object : targets) {
                    setOnline(object, nowOnline);
                }
                MimerOnlineActionUtils.refresh(nodes);
            });
        return null;
    }

    private static boolean isOnline(DBSObject object) {
        return object instanceof MimerDatabank databank
            ? databank.isOnline()
            : ((MimerDatabankShadow) object).isOnline();
    }

    private static void setOnline(DBSObject object, boolean online) {
        if (object instanceof MimerDatabank databank) {
            databank.setOnline(online);
        } else {
            ((MimerDatabankShadow) object).setOnline(online);
        }
    }
}
