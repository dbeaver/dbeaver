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
package org.jkiss.dbeaver.ui.app.config.registry;

/**
 * An action that can be performed in Product Config as the last step.
 */
public sealed interface ProductConfigAction {
    /**
     * Whether this action is applicable in current context. If not, it won't be shown in the UI.
     */
    boolean isApplicable();

    /**
     * A checkbox action.
     */
    non-sealed interface OfCheckbox extends ProductConfigAction {
        boolean loadState();

        void applyState(boolean value);
    }
}
