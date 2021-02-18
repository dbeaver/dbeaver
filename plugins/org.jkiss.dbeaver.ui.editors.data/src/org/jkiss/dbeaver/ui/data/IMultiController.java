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
package org.jkiss.dbeaver.ui.data;

/**
 * Multi-value controller.
 * Supports value editors switch.
 */
public interface IMultiController
{
    /**
     * Closes current value editor.
     * This action may initiated by editor control (e.g. on Enter or Esc key)
     */
    void closeInlineEditor();

    /**
     * Closes current editor and activated next cell editor
     * @param next true for next and false for previous cell
     */
    void nextInlineEditor(boolean next);

}
