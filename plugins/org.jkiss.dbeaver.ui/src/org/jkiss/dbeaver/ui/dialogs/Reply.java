/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.jkiss.code.NotNull;

public enum Reply {
    YES(IDialogConstants.YES_LABEL),
    NO(IDialogConstants.NO_LABEL),
    OK(IDialogConstants.OK_LABEL)
    ;

    @NotNull
    private final String displayString;

    Reply(@NotNull String displayString) {
        this.displayString = displayString;
    }

    @NotNull
    String getDisplayString() {
        return displayString;
    }
}
