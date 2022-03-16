/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.qm.filters;

import org.jkiss.code.Nullable;

import java.time.LocalDateTime;

public class QMDateRange {
    @Nullable
    private final LocalDateTime from;
    @Nullable
    private final LocalDateTime to;

    public QMDateRange(@Nullable LocalDateTime from, @Nullable LocalDateTime to) {
        this.from = from;
        this.to = to;
    }

    @Nullable
    public LocalDateTime getFrom() {
        return from;
    }

    @Nullable
    public LocalDateTime getTo() {
        return to;
    }
}
