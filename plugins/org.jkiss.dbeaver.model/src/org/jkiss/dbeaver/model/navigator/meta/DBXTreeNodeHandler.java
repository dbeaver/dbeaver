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

package org.jkiss.dbeaver.model.navigator.meta;

/**
 * DBXTreeNodeHandler
 */
public class DBXTreeNodeHandler
{
    public enum Action {
        open,
        expand,
        collapse,
        create,
        delete,
    }

    public enum Perform {
        open,
        toggle,
        command,
        delete,
        none;
    };

    private final Action action;
    private final Perform perform;
    private final String command;

    public DBXTreeNodeHandler(Action action, Perform perform, String command) {
        this.action = action;
        this.perform = perform;
        this.command = command;
    }

    public Action getAction() {
        return action;
    }

    public Perform getPerform() {
        return perform;
    }

    public String getCommand() {
        return command;
    }
}
