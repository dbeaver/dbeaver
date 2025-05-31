/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.prompt;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObjectWithDescription;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.List;

public class DefaultPromptFormatter implements AIPromptFormatter {
    @NotNull
    @Override
    public String postProcessGeneratedQuery(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSObjectContainer mainObject,
        @NotNull DBCExecutionContext executionContext,
        @NotNull String completionText
    ) {
        return completionText;
    }

    @NotNull
    @Override
    public List<String> getExtraInstructions() {
        return List.of();
    }

    @Override
    public void addExtraDescription(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity object,
        @NotNull StringBuilder description,
        @NotNull DBPObjectWithDescription lastAttr
    ) throws DBException {
        // nothing to do
    }

    public void addObjectDescriptionIfNeeded(
        @NotNull StringBuilder description,
        @NotNull DBPObjectWithDescription object,
        @NotNull DBRProgressMonitor monitor
    ) {
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AIConstants.AI_SEND_DESCRIPTION)
            && !CommonUtils.isEmptyTrimmed(object.getDescription())) {
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
    public void addColumnTypeIfNeeded(
        @NotNull StringBuilder description,
        @NotNull DBSEntityAttribute attribute,
        @NotNull DBRProgressMonitor monitor
    ) {
        if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AIConstants.AI_SEND_TYPE_INFO)) {
            description.append(" ").append(attribute.getTypeName());
        }
    }

    @Override
    public void addDataSample(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSDataContainer dataContainer,
        @NotNull StringBuilder description
    ) throws DBException {
        // nothing to do
    }

}
