/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.rm.lock;

public class RMLockInfo {
    private final String applicationId;
    private final String projectId;
    private final String operationId;
    private final String operationName;
    private final long operationStartTime;

    private RMLockInfo(
        String applicationId,
        String projectId,
        String operationId,
        String operationName,
        long operationStartTime
    ) {
        this.applicationId = applicationId;
        this.projectId = projectId;
        this.operationId = operationId;
        this.operationName = operationName;
        this.operationStartTime = operationStartTime;
    }

    static RMLockInfo emptyLock(
        String projectId
    ) {
        return new RMLockInfo(
            "",
            projectId,
            "",
            "",
            System.currentTimeMillis()
        );
    }


    public boolean isBlank() {
        return operationId.isEmpty();
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getOperationName() {
        return operationName;
    }

    public long getOperationStartTime() {
        return operationStartTime;
    }

    public static final class Builder {
        private String applicationId;
        private final String projectId;
        private final String operationId;
        private String operationName;
        private long operationStartTime;

        public Builder(String projectId, String operationId) {
            this.projectId = projectId;
            this.operationId = operationId;
        }


        public Builder setApplicationId(String applicationId) {
            this.applicationId = applicationId;
            return this;
        }


        public Builder setOperationName(String operationName) {
            this.operationName = operationName;
            return this;
        }

        public Builder setOperationStartTime(long operationStartTime) {
            this.operationStartTime = operationStartTime;
            return this;
        }

        public RMLockInfo build() {
            return new RMLockInfo(applicationId, projectId, operationId, operationName, operationStartTime);
        }
    }
}
