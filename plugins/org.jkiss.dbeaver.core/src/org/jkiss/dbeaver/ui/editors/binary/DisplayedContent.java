/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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


    private StringBuilder data = null;
    private Set<TextChangeListener> textListeners = null;
    private int numberOfColumns = -1;
    //private int numberOfLines = -1;
    private int linesTimesColumns = -1;

    /**
     * Create empty content for a StyledText of the specified size
     *
     * @param numberOfLines
     * @param numberOfColumns
     */
    DisplayedContent(int numberOfColumns, int numberOfLines)
    {
        data = new StringBuilder(numberOfColumns * numberOfLines * 2);  // account for replacements
        textListeners = new HashSet<TextChangeListener>();
        setDimensions(numberOfColumns, numberOfLines);
    }

    public void addTextChangeListener(TextChangeListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("Cannot add a null listener");

        textListeners.add(listener);
    }

    public int getCharCount()
    {
        return data.length();
    }


    public String getLine(int lineIndex)
    {
        return getTextRange(lineIndex * numberOfColumns, numberOfColumns);
    }


    public int getLineAtOffset(int offset)
    {
        int result = offset / numberOfColumns;
        if (result >= getLineCount())
            return getLineCount() - 1;

        return result;
    }


    public int getLineCount()
    {
        return (data.length() - 1) / numberOfColumns + 1;
    }

    public String getLineDelimiter()
    {
        return "";
    }

    public int getOffsetAtLine(int lineIndex)
    {
        return lineIndex * numberOfColumns;
    }


    public String getTextRange(int start, int length)
    {
        int dataLength = data.length();
        if (start > dataLength)
            return "";

        return data.substring(start, Math.min(dataLength, start + length));
    }


    public void removeTextChangeListener(TextChangeListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("Cannot remove a null listener");

        textListeners.remove(listener);
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
        if (length != replaceLength || start + length > data.length())
            return;

        data.replace(start, start + length, text);
    }


    void setDimensions(int columns, int lines)
    {
        numberOfColumns = columns;
        //numberOfLines = lines;
        linesTimesColumns = lines * columns;
        setText(data.toString());
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#setText(java.lang.String)
     */
    public void setText(String text)
    {
        data.setLength(0);
        data.append(text.substring(0, Math.min(text.length(), linesTimesColumns)));

        TextChangedEvent changedEvent = new TextChangedEvent(this);
        for (TextChangeListener textListener : textListeners) {
            textListener.textSet(changedEvent);
        }
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
        int currentLimit = Math.min(data.length(), linesTimesColumns);
        TextChangingEvent event = new TextChangingEvent(this);
        event.start = forward ? 0 : currentLimit;
        event.newText = text;
        event.replaceCharCount = 0;
        event.newCharCount = text.length();
        event.replaceLineCount = 0;
        event.newLineCount = linesInText;
        for (TextChangeListener myTextListener : textListeners) myTextListener.textChanging(event);

        data.insert(event.start, text);
//System.out.print("Event1:start:"+event.start+", newCCount:"+event.newCharCount+", newLCount:"+
//event.newLineCount+" ");System.out.flush();

        TextChangedEvent changedEvent = new TextChangedEvent(this);
        for (TextChangeListener myTextListener : textListeners) myTextListener.textChanged(changedEvent);

        event = new TextChangingEvent(this);
//	event.start = forward ? linesTimesColumns : 0;
        event.start = forward ? linesTimesColumns - 1 : 0;
        event.newText = "";
        event.replaceCharCount = linesInText * numberOfColumns - linesTimesColumns + currentLimit;
        event.newCharCount = 0;
        event.replaceLineCount = linesInText;
        event.newLineCount = 0;
        for (TextChangeListener myTextListener : textListeners) myTextListener.textChanging(event);

//	data.delete(event.start, event.start + event.replaceCharCount);
        if (forward)
            data.delete(linesTimesColumns, linesTimesColumns + event.replaceCharCount);
        else
            data.delete(0, event.replaceCharCount);
//System.out.println("Event2:start:"+event.start+", replaceCCount:"+event.replaceCharCount+
//", replaceLCount:"+event.replaceLineCount+", text:"+text);System.out.flush();

        changedEvent = new TextChangedEvent(this);
        for (TextChangeListener myTextListener : textListeners) myTextListener.textChanged(changedEvent);
    }

}
