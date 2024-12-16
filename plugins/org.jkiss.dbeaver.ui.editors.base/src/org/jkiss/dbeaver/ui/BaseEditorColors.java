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
package org.jkiss.dbeaver.ui;

/**
 * Colors for various connection-related objects
 */
public interface BaseEditorColors {
    String COLOR_SUCCESS = "org.jkiss.dbeaver.txn.color.committed.background";  //= new RGB(0xBD, 0xFE, 0xBF); //$NON-NLS-1$
    String COLOR_ERROR = "org.jkiss.dbeaver.txn.color.reverted.background";  // = new RGB(0xFF, 0x63, 0x47); //$NON-NLS-1$
    String COLOR_WARNING = "org.jkiss.dbeaver.txn.color.transaction.background";  // = new RGB(0xFF, 0xE4, 0xB5); //$NON-NLS-1$


    String COLOR_UNCOMMITTED = COLOR_SUCCESS;
    String COLOR_REVERTED = COLOR_ERROR;
    String COLOR_TRANSACTION = COLOR_WARNING;

}