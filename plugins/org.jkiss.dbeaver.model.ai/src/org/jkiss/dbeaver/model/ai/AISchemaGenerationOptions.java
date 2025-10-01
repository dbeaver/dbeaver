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
package org.jkiss.dbeaver.model.ai;

public record AISchemaGenerationOptions(
    int maxDbSnapshotTokens,
    boolean sendObjectComment,
    boolean sendColumnTypes,
    boolean sendConstraints,
    boolean sendForeignKeys,
    boolean sendSampleData
) {

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder()
            .withMaxDbSnapshotTokens(maxDbSnapshotTokens)
            .withSendObjectComment(sendObjectComment)
            .withSendColumnTypes(sendColumnTypes)
            .withSendConstraints(sendConstraints)
            .withSendForeignKeys(sendForeignKeys)
            .withSendSampleData(sendSampleData);
    }

    public static final class Builder {
        private int maxDbSnapshotTokens;
        private boolean sendObjectComment;
        private boolean sendColumnTypes;
        private boolean sendConstraints;
        private boolean sendForeignKeys;
        private boolean sendSampleData;

        private Builder() {
        }

        public Builder withMaxDbSnapshotTokens(int maxDbSnapshotTokens) {
            this.maxDbSnapshotTokens = maxDbSnapshotTokens;
            return this;
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

        public Builder withSendSampleData(boolean sendSampleData) {
            this.sendSampleData = sendSampleData;
            return this;
        }

        public AISchemaGenerationOptions build() {
            return new AISchemaGenerationOptions(
                maxDbSnapshotTokens,
                sendObjectComment,
                sendColumnTypes,
                sendConstraints,
                sendForeignKeys,
                sendSampleData
            );
        }
    }
}
