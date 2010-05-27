/*
 * binary, a java binary editor
 * Copyright (C) 2006, 2009 Jordi Bergenthal, pestatije(-at_)users.sourceforge.net
 * The official binary site is sourceforge.net/projects/binary
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.jkiss.dbeaver.ui.editors.binary;

import org.eclipse.swt.custom.*;

import java.util.HashSet;
import java.util.Set;


/**
 * StyledTextContent customised for content that fills up to one page of the StyledText widget. No line
 * delimiters, content wraps lines.
 *
 * @author Jordi
 */
public class DisplayedContent implements StyledTextContent {


    StringBuffer myData = null;
    Set<TextChangeListener> myTextListeners = null;
    int numberOfColumns = -1;
    int numberOfLines = -1;
    int linesTimesColumns = -1;
    StyledText styledText = null;

    /**
     * Create empty content for a StyledText of the specified size
     *
     * @param numberOfLines
     * @param numberOfColumns
     */
    DisplayedContent(StyledText styledText, int numberOfColumns, int numberOfLines)
    {
        this.styledText = styledText;
        myData = new StringBuffer(numberOfColumns * numberOfLines * 2);  // account for replacements
        myTextListeners = new HashSet<TextChangeListener>();
        setDimensions(numberOfColumns, numberOfLines);
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#addTextChangeListener
     *      (org.eclipse.swt.custom.TextChangeListener)
     */
    public void addTextChangeListener(TextChangeListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("Cannot add a null listener");

        myTextListeners.add(listener);
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#getCharCount()
     */
    public int getCharCount()
    {
        return myData.length();
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#getLine(int)
     */
    public String getLine(int lineIndex)
    {
        return getTextRange(lineIndex * numberOfColumns, numberOfColumns);
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#getLineAtOffset(int)
     */
    public int getLineAtOffset(int offset)
    {
        int result = offset / numberOfColumns;
        if (result >= getLineCount())
            return getLineCount() - 1;

        return result;
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#getLineCount()
     */
    public int getLineCount()
    {
        return (myData.length() - 1) / numberOfColumns + 1;
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#getLineDelimiter()
     */
    public String getLineDelimiter()
    {
        return "";
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#getOffsetAtLine(int)
     */
    public int getOffsetAtLine(int lineIndex)
    {
        return lineIndex * numberOfColumns;
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#getTextRange(int, int)
     */
    public String getTextRange(int start, int length)
    {
        int dataLength = myData.length();
        if (start > dataLength)
            return "";

        return myData.substring(start, Math.min(dataLength, start + length));
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#removeTextChangeListener
     *      (org.eclipse.swt.custom.TextChangeListener)
     */
    public void removeTextChangeListener(TextChangeListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("Cannot remove a null listener");

        myTextListeners.remove(listener);
    }


    /**
     * Replaces part of the content with new text. Works only when the new text length is the same as
     * replaceLength (when the content's size won't change). For other cases use <code>setText()</code> or
     * <code>shiftLines()</code> instead.
     *
     * @see org.eclipse.swt.custom.StyledTextContent#replaceTextRange(int, int, java.lang.String)
     */
    public void replaceTextRange(int start, int replaceLength, String text)
    {
        int length = text.length();
        if (length != replaceLength || start + length > myData.length())
            return;

        myData.replace(start, start + length, text);
    }


    void setDimensions(int columns, int lines)
    {
        numberOfColumns = columns;
        numberOfLines = lines;
        linesTimesColumns = lines * columns;
        setText(myData.toString());
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#setText(java.lang.String)
     */
    public void setText(String text)
    {
        myData.setLength(0);
        myData.append(text.substring(0, Math.min(text.length(), linesTimesColumns)));

        TextChangedEvent changedEvent = new TextChangedEvent(this);
        for (TextChangeListener myTextListener : myTextListeners) myTextListener.textSet(changedEvent);
    }


    /**
     * Shifts full lines of text and fills the new empty space with text
     *
     * @param text    to replace new empty lines. Its size determines the number of lines to shift
     * @param forward shifts lines either forward or backward
     */
    public void shiftLines(String text, boolean forward)
    {
        if (text.length() == 0) return;

        int linesInText = (text.length() - 1) / numberOfColumns + 1;
        int currentLimit = Math.min(myData.length(), linesTimesColumns);
        TextChangingEvent event = new TextChangingEvent(this);
        event.start = forward ? 0 : currentLimit;
        event.newText = text;
        event.replaceCharCount = 0;
        event.newCharCount = text.length();
        event.replaceLineCount = 0;
        event.newLineCount = linesInText;
        for (TextChangeListener myTextListener : myTextListeners) myTextListener.textChanging(event);

        myData.insert(event.start, text);
//System.out.print("Event1:start:"+event.start+", newCCount:"+event.newCharCount+", newLCount:"+
//event.newLineCount+" ");System.out.flush();

        TextChangedEvent changedEvent = new TextChangedEvent(this);
        for (TextChangeListener myTextListener : myTextListeners) myTextListener.textChanged(changedEvent);

        event = new TextChangingEvent(this);
//	event.start = forward ? linesTimesColumns : 0;
        event.start = forward ? linesTimesColumns - 1 : 0;
        event.newText = "";
        event.replaceCharCount = linesInText * numberOfColumns - linesTimesColumns + currentLimit;
        event.newCharCount = 0;
        event.replaceLineCount = linesInText;
        event.newLineCount = 0;
        for (TextChangeListener myTextListener : myTextListeners) myTextListener.textChanging(event);

//	myData.delete(event.start, event.start + event.replaceCharCount);
        if (forward)
            myData.delete(linesTimesColumns, linesTimesColumns + event.replaceCharCount);
        else
            myData.delete(0, event.replaceCharCount);
//System.out.println("Event2:start:"+event.start+", replaceCCount:"+event.replaceCharCount+
//", replaceLCount:"+event.replaceLineCount+", text:"+text);System.out.flush();

        changedEvent = new TextChangedEvent(this);
        for (TextChangeListener myTextListener : myTextListeners) myTextListener.textChanged(changedEvent);
    }
}
