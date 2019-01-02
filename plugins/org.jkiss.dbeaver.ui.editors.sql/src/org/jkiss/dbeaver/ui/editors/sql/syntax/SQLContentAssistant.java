/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.contentassist.ContentAssistant;

/**
 * SQL Completion proposal
 */
public class SQLContentAssistant extends ContentAssistant {

    @Override
    protected AutoAssistListener createAutoAssistListener() {
        return new SQLAutoAssistListener();
    }

    private class SQLAutoAssistListener extends AutoAssistListener {
        @Override
        protected void showAssist(int showStyle) {
            SQLCompletionProcessor.setSimpleMode(true);
            try {
                super.showAssist(showStyle);
            } finally {
                SQLCompletionProcessor.setSimpleMode(false);
            }
        }
    }

    @Override
    public String showContextInformation() {
        SQLCompletionProcessor.setLookupTemplates(true);
        try {
            return super.showPossibleCompletions();
        } finally {
            SQLCompletionProcessor.setLookupTemplates(false);
        }
    }
}
