/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
 * Standalone Value Editor.
 * Visualizes cell editor in separate non-modal dialog or editor
 */
public interface IValueEditorStandalone extends IValueEditor
{
    /**
     * Brings editor to the top of screen
     */
    void showValueEditor();

    /**
     * Closes this editor.
     * Implementor must call unregisterEditor on it's value controller
     */
    void closeValueEditor();

}
