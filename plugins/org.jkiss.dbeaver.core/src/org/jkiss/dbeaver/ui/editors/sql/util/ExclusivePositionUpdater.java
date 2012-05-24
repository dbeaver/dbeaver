/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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