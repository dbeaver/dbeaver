/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors;

import java.util.Map;

/**
 * IDatabasePostSaveProcessor
 */
public interface IDatabasePostSaveProcessor {

    /**
     * Execute post-save commands.
     * May run database object compile or other complex stuff.
     * @param context can be used to save object state. Cleared on each save.
     */
    void runPostSaveCommands(Map<String, Object> context);

}
