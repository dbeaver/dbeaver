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
import org.eclipse.osgi.util.NLS;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.mimer.model.MimerDataSource;
import org.jkiss.dbeaver.ext.mimer.model.MimerDatabankShadow;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * "Add Pages" navigator action - {@code ALTER SHADOW "<name>" ADD <n> PAGES}, extending the
 * shadow's file by a number of 2K Mimer SQL pages.
 *
 * @author Mimer Information Technology
 */
public class MimerShadowAddPagesHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        final List<DBNDatabaseNode> nodes = MimerOnlineActionUtils.collectNodes(selection, MimerDatabankShadow.class);
        if (nodes.isEmpty()) {
            return null;
        }

        InputDialog dialog = new InputDialog(
            HandlerUtil.getActiveShell(event),
            MimerUIMessages.action_shadow_add_pages_title,
            NLS.bind(MimerUIMessages.action_shadow_add_pages_message,
                nodes.size() + (nodes.size() > 1 ? " shadows" : " shadow")),
            "100",
            new PositiveIntegerValidator());
        if (dialog.open() != Window.OK) {
            return null;
        }
        int pages = CommonUtils.toInt(dialog.getValue().trim());

        List<String> statements = new ArrayList<>(nodes.size());
        for (DBNDatabaseNode node : nodes) {
            statements.add(MimerDatabankShadow.buildAddPagesDDL(node.getObject().getName(), pages));
        }
        final List<DBNNode> parents = MimerOnlineActionUtils.parentsOf(nodes);
        MimerOnlineActionUtils.run(
            (MimerDataSource) nodes.get(0).getObject().getDataSource(),
            "Add pages to shadow",
            statements,
            false,
            () -> MimerOnlineActionUtils.refresh(parents));
        return null;
    }

    private static class PositiveIntegerValidator implements IInputValidator {
        @Override
        public String isValid(String newText) {
            int value;
            try {
                value = Integer.parseInt(newText.trim());
            } catch (NumberFormatException e) {
                return "Enter a whole number of pages.";
            }
            return value > 0 ? null : "The number of pages must be greater than zero.";
        }
    }
}
