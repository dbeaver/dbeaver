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
package org.jkiss.dbeaver.ui.controls.resultset.panel;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetListener;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetListenerAdapter;

public final class ResultSetPanelRefresher extends ResultSetListenerAdapter {
    private final IResultSetPanel resultSetPanel;

    private ResultSetPanelRefresher(@NotNull IResultSetPanel resultSetPanel) {
        this.resultSetPanel = resultSetPanel;
    }

    public static void installOn(@NotNull IResultSetPanel resultSetPanel, @NotNull IResultSetPresentation presentation) {
        IResultSetListener listener = new ResultSetPanelRefresher(resultSetPanel);
        presentation.getController().addListener(listener);
        presentation.getControl().addDisposeListener(e -> presentation.getController().removeListener(listener));
    }

    @Override
    public void handleResultSetLoad() {
        resultSetPanel.refresh(true);
    }
}
