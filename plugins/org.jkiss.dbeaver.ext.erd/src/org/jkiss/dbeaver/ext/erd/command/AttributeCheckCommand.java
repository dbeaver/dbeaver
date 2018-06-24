/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.command;

import org.eclipse.gef.commands.Command;
import org.jkiss.dbeaver.ext.erd.part.AttributePart;

/**
 * Change attribute checked state
 */
public class AttributeCheckCommand extends Command {

    private AttributePart attr;
    private boolean newValue;
    private boolean oldValue;

    public AttributeCheckCommand(AttributePart attr, boolean newValue) {
        super("Select attribute");
        this.attr = attr;

        this.oldValue = this.attr.getAttribute().isChecked();
        this.newValue = newValue;
    }

    @Override
    public void execute() {
        attr.getAttribute().setChecked(newValue);
        attr.getFigure().getCheckBox().setSelected(newValue);
    }

    @Override
    public void undo() {
        attr.getAttribute().setChecked(oldValue);
        attr.getFigure().getCheckBox().setSelected(oldValue);
    }

}