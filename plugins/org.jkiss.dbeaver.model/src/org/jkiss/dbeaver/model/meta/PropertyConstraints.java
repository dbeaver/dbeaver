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
package org.jkiss.dbeaver.model.meta;

import org.jkiss.code.Nullable;

public record PropertyConstraints(
    @Nullable Float min,
    @Nullable Float max,
    @Nullable Float step,
    @Nullable Integer minLength,
    @Nullable Integer maxLength) {

    @Nullable
    public static PropertyConstraints from(@Nullable Property property) {
        if (property == null) {
            return null;
        }
        Float min = Float.isNaN(property.min()) ? null : property.min();
        Float max = Float.isNaN(property.max()) ? null : property.max();
        Float step = Float.isNaN(property.step()) ? null : property.step();
        Integer minLength = property.minLength() < 0 ? null : property.minLength();
        Integer maxLength = property.maxLength() < 0 ? null : property.maxLength();
        return min == null && max == null && step == null && minLength == null && maxLength == null
            ? null
            : new PropertyConstraints(min, max, step, minLength, maxLength);
    }
}
