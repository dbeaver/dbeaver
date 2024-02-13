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
package org.jkiss.dbeaver.model.ai.format;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObjectWithDescription;
import org.jkiss.dbeaver.model.ai.AICompletionConstants;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public class DefaultRequestFormatter implements IAIFormatter {
    @Override
    public String postProcessGeneratedQuery(
        DBRProgressMonitor monitor,
        DBSObjectContainer mainObject,
        DBCExecutionContext executionContext,
        String completionText
    ) {
        return completionText;
    }

    @Nullable
    @Override
    public String getExtraInstructions(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer mainObject,
        @NotNull DBCExecutionContext executionContext
    ) {
        // nothing to do
        return null;
    }

    @Override
    public void addExtraDescription(
        DBRProgressMonitor monitor,
        DBSEntity object,
        StringBuilder description,
        DBPObjectWithDescription lastAttr
    ) throws DBException {
        // nothing to do
    }

    public void addObjectDescriptionIfNeeded(
        @NotNull StringBuilder description,
        @NotNull DBPObjectWithDescription object,
        @NotNull DBRProgressMonitor monitor
    ) {
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_SEND_DESCRIPTION)
            && object.getDescription() != null) {
            boolean attribute = object instanceof DBSEntityAttribute;
            String objectComment = object.getDescription().replace("\n", attribute ? "\n\t" : "\n");
            if (attribute) {
                description.append(" ");
            }
            description.append("-- ").append(objectComment);
            if (!attribute) {
                description.append("\n");
            }
        }
    }

    @Override
    public void addColumnTypeIfNeeded(@NotNull StringBuilder description, @NotNull DBSEntityAttribute attribute, @NotNull DBRProgressMonitor monitor) {
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_SEND_TYPE_INFO)) {
            description.append(" ").append(attribute.getTypeName());
        }
    }

}
