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
package org.jkiss.dbeaver.tools.transfer.ui;

import org.jkiss.dbeaver.model.runtime.features.DBRFeature;

public interface DataTransferFeatures {
    DBRFeature CATEGORY_DATA_TRANSFER = DBRFeature.createCategory("DATA_TRANSFER", "Data Transfer features");
    DBRFeature DATA_TRANSFER = DBRFeature.createFeature(CATEGORY_DATA_TRANSFER, "DATA_TRANSFER_START");

    String PARAM_TRANSFER_DATA_TYPE = "dataType";
    String PARAM_TRANSFER_TYPE = "transferType";
    String IS_TASK = "isTask";
}
