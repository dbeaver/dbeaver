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
package org.jkiss.dbeaver.dpi.model.client;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.List;

public class DPISmartObjectResponse {
    @Nullable
    private final Object methodInvocationResult;
    @NotNull
    private final List<DPISmartObjectWrapper> smartObjects;

    public DPISmartObjectResponse(
        @Nullable Object methodInvocationResult,
        @NotNull List<DPISmartObjectWrapper> smartObjects
    ) {
        this.methodInvocationResult = methodInvocationResult;
        this.smartObjects = smartObjects;
    }

    @Nullable
    public Object getMethodInvocationResult() {
        return methodInvocationResult;
    }

    @NotNull
    public List<DPISmartObjectWrapper> getSmartObjects() {
        return smartObjects;
    }
}
