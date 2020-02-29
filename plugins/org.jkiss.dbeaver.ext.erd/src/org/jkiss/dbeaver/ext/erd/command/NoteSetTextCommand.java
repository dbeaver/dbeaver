/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.erd.figures.NoteFigure;
import org.jkiss.dbeaver.ext.erd.part.NotePart;

/**
 * Change note text
 */
public class NoteSetTextCommand extends Command {

    private NotePart note;
    private String newText;
    private String oldText;

    public NoteSetTextCommand(NotePart note, String newText) {
        super("Set note text");
        this.note = note;

        this.oldText = this.note.getName();
        this.newText = newText;
    }

    @Override
    public void execute() {
        note.getNote().setObject(newText);
        ((NoteFigure) note.getFigure()).setText(newText);
    }

    @Override
    public void undo() {
        note.getNote().setObject(oldText);
        ((NoteFigure) note.getFigure()).setText(oldText);
    }

}