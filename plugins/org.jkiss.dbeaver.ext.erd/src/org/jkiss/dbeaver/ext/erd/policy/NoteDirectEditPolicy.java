/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.policy;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.requests.DirectEditRequest;
import org.eclipse.jface.viewers.CellEditor;
import org.jkiss.dbeaver.ext.erd.command.NoteSetTextCommand;
import org.jkiss.dbeaver.ext.erd.part.NotePart;

/**
 * EditPolicy for the direct editing of table names
 *
 * @author Serge Rider
 */
public class NoteDirectEditPolicy extends DirectEditPolicy {

    private String oldValue;

    @Override
    protected Command getDirectEditCommand(DirectEditRequest request) {
        CellEditor cellEditor = request.getCellEditor();
        String newNote = (String) cellEditor.getValue();

        return new NoteSetTextCommand((NotePart) getHost(), newNote);
    }

    @Override
    protected void showCurrentEditValue(DirectEditRequest request) {
        String value = (String) request.getCellEditor().getValue();
        NotePart notePart = (NotePart) getHost();
        notePart.handleNameChange(value);
    }

    @Override
    protected void storeOldEditValue(DirectEditRequest request) {

        CellEditor cellEditor = request.getCellEditor();
        oldValue = (String) cellEditor.getValue();
    }

    @Override
    protected void revertOldEditValue(DirectEditRequest request) {
        CellEditor cellEditor = request.getCellEditor();
        cellEditor.setValue(oldValue);
        NotePart entityPart = (NotePart) getHost();
        entityPart.revertNameChange();
    }

}