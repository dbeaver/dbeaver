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
package org.jkiss.dbeaver.ui.actions.datasource;

/**
 * Connection commands
 */
public interface ConnectionCommands
{

    String CMD_CONNECT = "org.jkiss.dbeaver.core.connect";
    String CMD_DISCONNECT = "org.jkiss.dbeaver.core.disconnect";
    String CMD_DISCONNECT_ALL = "org.jkiss.dbeaver.core.disconnectAll";
    String CMD_DISCONNECT_OTHER = "org.jkiss.dbeaver.core.disconnectOther";
    String CMD_INVALIDATE = "org.jkiss.dbeaver.core.invalidate";
    String CMD_COMMIT = "org.jkiss.dbeaver.core.commit";
    String CMD_ROLLBACK = "org.jkiss.dbeaver.core.rollback";
    String CMD_TOGGLE_AUTOCOMMIT = "org.jkiss.dbeaver.core.txn.autocommit"; //$NON-NLS-1$
}
