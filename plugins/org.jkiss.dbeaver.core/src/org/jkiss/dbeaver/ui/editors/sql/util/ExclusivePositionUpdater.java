/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql.util;

import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.Position;

public class ExclusivePositionUpdater implements IPositionUpdater
{

    private final String category;

    public ExclusivePositionUpdater(String category)
    {
        this.category = category;
    }

    @Override
    public void update(DocumentEvent event)
    {

        int eventOffset = event.getOffset();
        int eventOldLength = event.getLength();
        int eventNewLength = event.getText() == null ? 0 : event.getText().length();
        int deltaLength = eventNewLength - eventOldLength;

        try
        {
            Position[] positions = event.getDocument().getPositions(category);

            for (int i = 0; i != positions.length; i++)
            {

                Position position = positions[i];

                if (position.isDeleted())
                {
                    continue;
                }

                int offset = position.getOffset();
                int length = position.getLength();
                int end = offset + length;

                if (offset >= eventOffset + eventOldLength)
                {
                    // position comes
                    // after change - shift
                    position.setOffset(offset + deltaLength);
                }
                else if (end <= eventOffset)
                {
                    // position comes way before change -
                    // leave alone
                }
                else if (offset <= eventOffset && end >= eventOffset + eventOldLength)
                {
                    // event completely renderers to the position - adjust length
                    position.setLength(length + deltaLength);
                }
                else if (offset < eventOffset)
                {
                    // event extends over end of position - adjust length
                    position.setLength(eventOffset - offset);
                }
                else if (end > eventOffset + eventOldLength)
                {
                    // event extends from before position into it - adjust offset
                    // and length
                    // offset becomes end of event, length ajusted acordingly
                    int newOffset = eventOffset + eventNewLength;
                    position.setOffset(newOffset);
                    position.setLength(end - newOffset);
                }
                else
                {
                    // event consumes the position - delete it
                    position.delete();
                }
            }
        }
        catch (BadPositionCategoryException e)
        {
            // ignore and return
//            _log.debug(EditorMessages.error_badLocationException, e);
        }
    }

    public String getCategory()
    {
        return category;
    }

}