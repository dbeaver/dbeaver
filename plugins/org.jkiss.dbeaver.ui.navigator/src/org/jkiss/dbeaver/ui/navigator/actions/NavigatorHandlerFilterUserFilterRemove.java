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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.UserDBSObjectFilterUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.Collections;

public class NavigatorHandlerFilterUserFilterRemove extends AbstractHandler {

    private static final Log log = Log.getLog(NavigatorHandlerFilterUserFilterRemove.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBNNode node = NavigatorUtils.getSelectedNode(selection);
        DBNDatabaseNode parentNode = (DBNDatabaseNode) (node instanceof DBNDatabaseItem ? node.getParentNode() : node);
        if (parentNode == null) {
            return null;
        }
        try {
            if (UIUtils.confirmAction(
                UINavigatorMessages.actions_navigator_user_filters_remove_title,
                UINavigatorMessages.actions_navigator_user_filters_remove_question
            )) {
                UserDBSObjectFilterUtils.clearUserObjectFilers(parentNode.getDataSourceContainer());
                NavigatorHandlerRefresh.refreshNavigator(Collections.singletonList(parentNode));
            }
        } catch (DBException e) {
            log.error(e);
        }
        return null;
    }
}
