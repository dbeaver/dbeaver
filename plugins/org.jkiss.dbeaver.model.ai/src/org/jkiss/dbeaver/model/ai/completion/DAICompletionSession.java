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
package org.jkiss.dbeaver.model.ai.completion;

import org.jkiss.code.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Completion session
 */
public class DAICompletionSession {
    private final DAICompletionContext context;
    private final List<DAICompletionRequest> requests;

    public DAICompletionSession(@NotNull DAICompletionContext context) {
        this.context = context;
        this.requests = new ArrayList<>();
    }

    public void add(@NotNull DAICompletionRequest request) {
        requests.add(request);
    }

    public void clear() {
        requests.clear();
    }

    @NotNull
    public DAICompletionContext getContext() {
        return context;
    }

    @NotNull
    public List<DAICompletionRequest> getRequests() {
        return requests;
    }
}
