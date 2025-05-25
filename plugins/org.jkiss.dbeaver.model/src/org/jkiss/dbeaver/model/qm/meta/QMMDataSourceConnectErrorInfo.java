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
package org.jkiss.dbeaver.model.qm.meta;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;

/**
 * QM model data source connect error info.
 */
public class QMMDataSourceConnectErrorInfo extends QMMObject implements QMMDataSourceInfo {
    @NotNull
    private final String containerId;
    @NotNull
    private final String containerName;
    @NotNull
    private final String driverId;
    @Nullable
    private final String connectionUrl;
    @NotNull
    private final String errorType;
    @Nullable
    private final String errorMessage;

    public QMMDataSourceConnectErrorInfo(
        @NotNull DBPDataSourceContainer container,
        @NotNull String errorType,
        @Nullable String errorMessage
    ) {
        super(QMMetaObjectType.CONNECTION_ERROR_INFO);
        this.containerId = container.getId();
        this.containerName = container.getName();
        this.driverId = container.getDriver().getId();
        this.connectionUrl = container.getConnectionConfiguration().getUrl();
        this.errorType = errorType;
        this.errorMessage = errorMessage;
    }


    @NotNull
    @Override
    public String getContainerId() {
        return containerId;
    }

    @NotNull
    @Override
    public String getContainerName() {
        return containerName;
    }

    @NotNull
    @Override
    public String getDriverId() {
        return driverId;
    }

    @Nullable
    @Override
    public String getConnectionUrl() {
        return connectionUrl;
    }

    @NotNull
    public String getErrorType() {
        return errorType;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getText() {
        return "";
    }

    @Override
    public QMMConnectionInfo getConnection() {
        return null;
    }
}