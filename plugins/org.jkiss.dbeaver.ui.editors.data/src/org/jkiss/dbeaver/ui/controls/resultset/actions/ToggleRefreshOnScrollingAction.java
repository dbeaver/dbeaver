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
package org.jkiss.dbeaver.ui.controls.resultset.actions;

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;

public class ToggleRefreshOnScrollingAction extends ToggleConnectionPreferenceAction {
    public ToggleRefreshOnScrollingAction(ResultSetViewer resultSetViewer) {
        super(resultSetViewer, ModelPreferences.RESULT_SET_REREAD_ON_SCROLLING, ResultSetMessages.pref_page_database_resultsets_label_reread_on_scrolling);
    }
}
