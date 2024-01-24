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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IParameterValues;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLPresentationRegistry;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Map;
import java.util.stream.Collectors;

public class SQLEditorHandlerSwitchPresentation extends AbstractHandler {
    public static final String CMD_SWITCH_PRESENTATION_ID = "org.jkiss.dbeaver.ui.editors.sql.switch.presentation";
    public static final String PARAM_PRESENTATION_ID = "presentationId";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final String presentationId = event.getParameter(PARAM_PRESENTATION_ID);
        if (CommonUtils.isEmpty(presentationId)) {
            return null;
        }

        final SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor == null) {
            return null;
        }

        final SQLPresentationDescriptor descriptor = editor.getExtraPresentationDescriptor();
        if (descriptor != null && descriptor.getId().equals(presentationId)) {
            editor.showExtraPresentation((SQLPresentationDescriptor) null);
        } else {
            editor.showExtraPresentation(presentationId);
        }

        return null;
    }

    public static class ParameterValues implements IParameterValues {
        @Override
        public Map<?, ?> getParameterValues() {
            return SQLPresentationRegistry.getInstance().getPresentations().stream()
                .collect(Collectors.toMap(
                    SQLPresentationDescriptor::getLabel,
                    SQLPresentationDescriptor::getId
                ));
        }
    }
}
