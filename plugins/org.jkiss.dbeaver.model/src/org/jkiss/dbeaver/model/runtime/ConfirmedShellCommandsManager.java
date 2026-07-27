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
package org.jkiss.dbeaver.model.runtime;/*
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


import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.registry.confirmation.runtime.ConfirmedShellCommandsRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

public class ConfirmedShellCommandsManager {

    public boolean isCommandApproved(@NotNull DBRShellCommand command) throws DBException {
        String rawCommand = command.getCommand();
        return CommonUtils.isNotEmpty(rawCommand)
            && (getConfirmedShellCommandsRegistry().isConfirmedShellCommand(rawCommand) || userWantsToAddCommand(rawCommand));

    }

    private boolean userWantsToAddCommand(@NotNull String command) throws DBException {
        if (DBWorkbench.getPlatformUI().confirmAction("add cmd", "add stuff?")) {
            getConfirmedShellCommandsRegistry().addConfirmedShellCommand(command);
            return true;
        }
        return false;
    }


    @NotNull
    private ConfirmedShellCommandsRegistry getConfirmedShellCommandsRegistry() {
        return ConfirmedShellCommandsRegistry.getInstance();
    }

}
