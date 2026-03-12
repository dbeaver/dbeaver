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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public record AISchemaGenerationOptions(
    boolean sendObjectComment,
    boolean sendColumnTypes,
    boolean sendConstraints,
    boolean sendForeignKeys,
    boolean sendIndexes,
    boolean sendFullDDL
) {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
            .withSendObjectComment(sendObjectComment)
            .withSendColumnTypes(sendColumnTypes)
            .withSendConstraints(sendConstraints)
            .withSendForeignKeys(sendForeignKeys)
            .withSendIndexes(sendIndexes)
            .withSendFullDDL(sendFullDDL);
    }

    public static final class Builder {
        private boolean sendObjectComment;
        private boolean sendColumnTypes;
        private boolean sendConstraints;
        private boolean sendForeignKeys;
        private boolean sendIndexes;
        private boolean sendFullDDL;

        private Builder() {
            // Init default settings
            DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
            this.sendColumnTypes = preferenceStore.getBoolean(AIConstants.AI_SEND_TYPE_INFO);
            this.sendConstraints = preferenceStore.getBoolean(AIConstants.AI_SEND_CONSTRAINTS);
            this.sendForeignKeys = preferenceStore.getBoolean(AIConstants.AI_SEND_FOREIGN_KEYS);
            this.sendIndexes = preferenceStore.getBoolean(AIConstants.AI_SEND_INDEXES);
            this.sendObjectComment = preferenceStore.getBoolean(AIConstants.AI_SEND_DESCRIPTION);
        }

        public Builder withSendObjectComment(boolean sendObjectDescription) {
            this.sendObjectComment = sendObjectDescription;
            return this;
        }

        public Builder withSendColumnTypes(boolean sendColumnTypes) {
            this.sendColumnTypes = sendColumnTypes;
            return this;
        }

        public Builder withSendConstraints(boolean sendConstraints) {
            this.sendConstraints = sendConstraints;
            return this;
        }

        public Builder withSendForeignKeys(boolean sendForeignKeys) {
            this.sendForeignKeys = sendForeignKeys;
            return this;
        }

        @NotNull
        public Builder withSendIndexes(boolean sendIndexes) {
            this.sendIndexes = sendIndexes;
            return this;
        }

        @NotNull
        public Builder withSendFullDDL(boolean sendFullDDL) {
            this.sendFullDDL = sendFullDDL;
            return this;
        }

        public AISchemaGenerationOptions build() {
            return new AISchemaGenerationOptions(
                sendObjectComment,
                sendColumnTypes,
                sendConstraints,
                sendForeignKeys,
                sendIndexes,
                sendFullDDL
            );
        }
    }
}
