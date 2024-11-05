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
import org.jkiss.dbeaver.runtime.DBWorkbench;

public class ShowNotificationHandler extends AbstractHandler {
    private static String lastMessage = "This is a test message";

    @Override
    public Object execute(ExecutionEvent event) {
        String message = DBWorkbench.getPlatformUI().promptProperty("Enter message", lastMessage);
        if (message == null) {
            return null;
        }
        DBWorkbench.getPlatformUI().showNotification("Test notification", message, false, null);
        lastMessage = message;
        return null;
    }
}
