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
package org.jkiss.dbeaver.model.fs.lock;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public class FileLockInfo {
    @NotNull
    private final String applicationId;
    @NotNull
    private final String operationId;
    @Nullable
    private final String operationName;
    private final long operationStartTime;

    private FileLockInfo(
        @NotNull String applicationId,
        @NotNull String operationId,
        @NotNull String operationName,
        long operationStartTime
    ) {
        this.applicationId = applicationId;
        this.operationId = operationId;
        this.operationName = operationName;
        this.operationStartTime = operationStartTime;
    }

    static FileLockInfo emptyLock() {
        return new FileLockInfo(
            "",
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


    public String getOperationId() {
        return operationId;
    }

    public String getOperationName() {
        return operationName;
    }

    public long getOperationStartTime() {
        return operationStartTime;
    }

    public static class Builder {
        private String applicationId;
        private final String operationId;
        private String operationName;
        private long operationStartTime;

        public Builder(String operationId) {
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

        public FileLockInfo build() {
            return new FileLockInfo(applicationId, operationId, operationName, operationStartTime);
        }
    }
}
