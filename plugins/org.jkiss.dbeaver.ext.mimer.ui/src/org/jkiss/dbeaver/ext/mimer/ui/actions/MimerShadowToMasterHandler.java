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

import java.util.List;

/**
 * "Switch Shadow To Master" navigator action - {@code ALTER SHADOW "<name>" TO MASTER}, which
 * promotes the shadow to be the master databank and demotes the current master to a shadow.
 * Single-selection only, with a strong confirmation given how disruptive the swap is.
 *
 * @author Mimer Information Technology
 */
public class MimerShadowToMasterHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        final List<DBNDatabaseNode> nodes = MimerOnlineActionUtils.collectNodes(selection, MimerDatabankShadow.class);
        if (nodes.size() != 1) {
            return null;
        }
        DBNDatabaseNode node = nodes.get(0);
        MimerDatabankShadow shadow = (MimerDatabankShadow) node.getObject();

        if (!UIUtils.confirmAction(
            HandlerUtil.getActiveShell(event),
            MimerUIMessages.action_shadow_to_master_title,
            NLS.bind(MimerUIMessages.action_shadow_to_master_message, shadow.getName(), shadow.getDatabank().getName()))) {
            return null;
        }

        final List<DBNNode> parents = MimerOnlineActionUtils.parentsOf(nodes);
        MimerOnlineActionUtils.run(
            (MimerDataSource) shadow.getDataSource(),
            "Switch shadow to master",
            List.of("ALTER SHADOW \"" + shadow.getName() + "\" TO MASTER"),
            false,
            () -> MimerOnlineActionUtils.refresh(parents));
        return null;
    }
}
