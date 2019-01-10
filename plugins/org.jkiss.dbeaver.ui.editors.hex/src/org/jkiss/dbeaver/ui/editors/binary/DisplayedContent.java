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
package org.jkiss.dbeaver.ui.editors.binary;

import org.eclipse.swt.custom.StyledTextContent;
import org.eclipse.swt.custom.TextChangeListener;
import org.eclipse.swt.custom.TextChangedEvent;
import org.eclipse.swt.custom.TextChangingEvent;

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
        textListeners = new HashSet<>();
        setDimensions(numberOfColumns, numberOfLines);
    }

    @Override
    public void addTextChangeListener(TextChangeListener listener)
    {
        if (listener == null) throw new IllegalArgumentException("Cannot add a null listener");

        textListeners.add(listener);
    }

    @Override
    public int getCharCount()
    {
        return data.length();
    }


    @Override
    public String getLine(int lineIndex)
    {
        return getTextRange(lineIndex * numberOfColumns, numberOfColumns);
    }


    @Override
    public int getLineAtOffset(int offset)
    {
        int result = offset / numberOfColumns;
        if (result >= getLineCount())
            return getLineCount() - 1;

        return result;
    }


    @Override
    public int getLineCount()
    {
        return (data.length() - 1) / numberOfColumns + 1;
    }

    @Override
    public String getLineDelimiter()
    {
        return "";
    }

    @Override
    public int getOffsetAtLine(int lineIndex)
    {
        return lineIndex * numberOfColumns;
    }


    @Override
    public String getTextRange(int start, int length)
    {
        int dataLength = data.length();
        if (start > dataLength)
            return "";

        return data.substring(start, Math.min(dataLength, start + length));
    }


    @Override
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
    @Override
    public void replaceTextRange(int start, int replaceLength, String text)
    {
        int length = text.length();
        if (length != replaceLength || start + length > data.length())
            return;

        data.replace(start, start + length, text);
    }


    void setDimensions(int columns, int lines)
    {
        numberOfColumns = columns <= 0 ? 1 : columns;
        //numberOfLines = lines;
        linesTimesColumns = lines * columns;
        setText(data.toString());
    }


    /**
     * @see org.eclipse.swt.custom.StyledTextContent#setText(java.lang.String)
     */
    @Override
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

        changedEvent = new TextChangedEvent(this);
        for (TextChangeListener myTextListener : textListeners) myTextListener.textChanged(changedEvent);
    }

}
