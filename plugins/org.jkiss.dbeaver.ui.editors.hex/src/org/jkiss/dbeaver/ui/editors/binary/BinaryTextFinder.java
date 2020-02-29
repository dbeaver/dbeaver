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

package org.jkiss.dbeaver.ui.editors.binary;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Find helper class to find binary and string literals in files.
 * Given a literal, finds its position in the file. It is possible to get subsequent finds.
 * The search is either binary or text based. Text based search uses standard java unicode (all of big
 * and little endian, odd and even address) plus ascii when the literal falls within ascii char limits.
 *
 * @author Jordi
 */
public class BinaryTextFinder {


    public static final int MAP_SIZE = 64 * 1024;
    public static final int MAX_SEQUENCE_SIZE = 2 * 1024;  // a search string of 2K should be enough

    private long bufferPosition = -1L;
    private ByteBuffer byteBuffer = null;
    private int currentPartFound = -1;  // relative positions
    private boolean currentPartFoundIsUnicode = false;
    private long currentPosition = 0L;  // absolute value, start of forward finds, end(exclusive) of backward finds
    private byte[] byteFindSequence = null;
    private boolean caseSensitive = true;
    private BinaryContent content = null;
    private boolean directionForward = true;
    private CharSequence literal = null;
    private int literalByteLength = -1;
    private Pattern pattern = null;
    private boolean stopSearching = false;


    /**
     * Create a finder object for a sequence of characters; uses unicode and ascii traversing
     *
     * @param literal the char sequence to find
     * @param aContent provider to be traversed
     */
    public BinaryTextFinder(CharSequence literal, BinaryContent aContent)
    {
        this.literal = literal;
        initSearchUnicodeAscii();
        content = aContent;
        bufferPosition = 0L;
        currentPosition = 0L;
    }


    /**
     * Create a finder object for a raw sequence of bytes
     *
     * @param sequence the byte sequence to find
     * @param aContent  provider to be traversed
     */
    public BinaryTextFinder(byte[] sequence, BinaryContent aContent)
    {
        initSearchHex(sequence);
        content = aContent;
        bufferPosition = 0L;
        currentPosition = 0L;
    }


    void findAllMatches()
        throws IOException
    {
        currentPartFound = findHexAsciiMatchInPart();
        int currentPartFoundUnicode = findUnicodeMatchInPart();
        currentPartFoundIsUnicode = false;

        if (currentPartFoundUnicode >= 0 && (currentPartFound < 0 ||
            directionForward && currentPartFound > currentPartFoundUnicode ||
            !directionForward && currentPartFound < currentPartFoundUnicode)) {
            currentPartFound = currentPartFoundUnicode;
            currentPartFoundIsUnicode = true;
        }
    }


    private int findHexAsciiMatchInPart()
        throws IOException
    {
        if (byteFindSequence == null) return -1;

        int start = 0;
        int inclusiveEnd = byteBuffer.limit() - byteFindSequence.length;
        if (!directionForward) {
            start = inclusiveEnd;
            inclusiveEnd = 0;
        }

        for (int i = start;
            directionForward && i <= inclusiveEnd || !directionForward && i >= inclusiveEnd;
            i += directionForward ? 1 : -1)
        {
            boolean matchesSoFar = true;
            for (int j = 0; j < byteFindSequence.length && matchesSoFar; ++j) {
                byte existing = byteBuffer.get(i + j);
                byte matcher = byteFindSequence[j];
                if (existing != matcher) {
                    if (caseSensitive || existing < 'A' || existing > 'z' || matcher < 'A' ||
                        matcher > 'z' || existing - matcher != 32 && matcher - existing != 32)
                        matchesSoFar = false;
                }
            }
            if (matchesSoFar) {
                return i;
            }
        }

        return -1;
    }


    private int findUnicodeMatchInPart()
        throws IOException
    {
        if (pattern == null) return -1;

        int result = Integer.MAX_VALUE;
        if (!directionForward) {
            result = -1;
        }
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        Matcher matcher = pattern.matcher(byteBuffer.asCharBuffer());

        for (int encoding = 0; encoding < 4; ++encoding) {
            while (matcher.find()) {
                int index = matcher.start() * 2 + (encoding >= 2 ? 1 : 0);
                if (directionForward && result > index || !directionForward && result < index) {
                    result = index;
                }
                if (directionForward) {
                    break;
                }
            }
            if (encoding == 0) {
                byteBuffer.order(ByteOrder.BIG_ENDIAN);
            } else if (encoding == 1 && byteBuffer.limit() > 0) {
                byteBuffer.position(1);
            } else if (encoding == 2) {
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            }
            matcher.reset(byteBuffer.asCharBuffer());
        }
        if (result == Integer.MAX_VALUE) {
            result = -1;
        }

        return result;
    }


    long getContentLength()
    {
        if (content == null) {
            return 0L;
        }

        return content.length();
    }


    /**
     * Get the next position and length of a matching literal
     *
     * @return an array with 2 elements, the first one a Long (position in the file),
     *         and the second one an Integer (byte length of the matching literal),
     *         or null if there are no matches
     */
    public Number[] getNextMatch()
        throws IOException
    {
        stopSearching = false;
        populatePart();
        findAllMatches();

        while (currentPartFound < 0) { // end of part
            if (nextPart() == null || stopSearching) {
                stopSearching = false;
                return null;  // end of file
            }
            findAllMatches();
        }

        long resultPosition = bufferPosition + currentPartFound;
        int length = currentPartFoundIsUnicode ? literalByteLength : byteFindSequence.length;
        setNewStart(resultPosition + (directionForward ? 1 : length - 1));

        return new Number[]{resultPosition, length};
    }


    void initSearchHex(byte[] sequence)
    {
        byteFindSequence = sequence;

        if (sequence.length > MAX_SEQUENCE_SIZE) {
            byteFindSequence = new byte[MAX_SEQUENCE_SIZE];
            System.arraycopy(sequence, 0, byteFindSequence, 0, MAX_SEQUENCE_SIZE);
        }

        literalByteLength = byteFindSequence.length;
    }


    /**
     * Get the current location being searched in the content. Approximate value.
     *
     * @return position in the content
     */
    public long getSearchPosition()
    {
        return bufferPosition;
    }


    void initSearchUnicodeAscii()
    {
        StringBuilder regex = new StringBuilder("\\Q");  // everything-quoted regular expression

        if (literal.length() * 2 > MAX_SEQUENCE_SIZE)  // 16 bit Unicode chars
            literal = literal.subSequence(0, MAX_SEQUENCE_SIZE / 2);
        literalByteLength = literal.length() * 2;

        boolean isAsciiCompatible = true;
        byte[] tmpBytes = new byte[literal.length()];
        char previous = '\0';
        for (int i = 0; i < literal.length(); ++i) {
            char aChar = literal.charAt(i);
            regex.append(aChar);

            if (previous == '\\' && aChar == 'E')
                regex.append("\\\\E\\Q");

            previous = aChar;

            tmpBytes[i] = (byte) aChar;
            if (aChar > 255) isAsciiCompatible = false;
        }
        regex.append("\\E");

        int ignoreCaseFlags = 0;
        if (!caseSensitive) ignoreCaseFlags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        pattern = Pattern.compile(regex.toString(), ignoreCaseFlags);

        if (isAsciiCompatible)
            byteFindSequence = tmpBytes;
    }


    ByteBuffer nextPart()
        throws IOException
    {
        long newPos = bufferPosition + byteBuffer.limit() - literalByteLength + 1L;
        if (!directionForward)
            newPos = bufferPosition - MAP_SIZE + literalByteLength - 1L;
        if (newPos < 0L)
            newPos = 0L;

        int size = (int) Math.min(MAP_SIZE, getContentLength() - newPos);
        if (!directionForward)
            size = (int) (bufferPosition + literalByteLength - 1L - newPos);

        if (size < literalByteLength)
            return null;
        bufferPosition = newPos;
        populatePart(size);

        return byteBuffer;
    }


    void populatePart()
        throws IOException
    {
        int size = MAP_SIZE;
        if (!directionForward) {
            size = (int) Math.min(MAP_SIZE, currentPosition);
        }
        populatePart(size);
    }


    void populatePart(int size)
        throws IOException
    {
        if (content == null) return;

        byteBuffer = null;  // multiple FileChannel.read(byteBuffer) leak memory, so don't reuse buffer
        byteBuffer = ByteBuffer.allocate(MAP_SIZE);
//	if (byteBuffer == null)
//		byteBuffer = ByteBuffer.allocate(MAP_SIZE);
        byteBuffer.limit(size);
        byteBuffer.position(0);
//	try {
        content.get(byteBuffer, bufferPosition);
//	} catch (OutOfMemoryError e) {
//		byteBuffer = null;
//		byteBuffer = ByteBuffer.allocate(MAP_SIZE);
        //	byteBuffer.limit(size);
        //byteBuffer.position(0);
        //content.get(byteBuffer, myCurrentPosition);
        //}
        byteBuffer.limit(byteBuffer.position());
        byteBuffer.position(0);
    }


    /**
     * Sets the case sensitiveness. The default is always case sensitive (not ignore case)
     *
     * @param beSensitive set to true will not match 'a' with 'A'
     */
    public void setCaseSensitive(boolean beSensitive)
    {
        if (caseSensitive == beSensitive) return;

        caseSensitive = beSensitive;
        if (literal != null)
            initSearchUnicodeAscii();
    }


    /**
     * Sets the search direction. The default search direction is always forward
     *
     * @param goForward set to true for forward search
     */
    public void setDirectionForward(boolean goForward)
    {
        directionForward = goForward;
    }


    /**
     * Sets new search start point in the file. Inclusive in forward finds, exclusive in backward ones.
     *
     * @param startPoint next match search will start from this point
     */
    public void setNewStart(long startPoint)
    {
        if (startPoint < 0L || startPoint > getContentLength())
            return;

        currentPosition = startPoint;
        bufferPosition = startPoint;
        if (!directionForward) {
            bufferPosition = startPoint - MAP_SIZE;
        }
        if (bufferPosition < 0L)
            bufferPosition = 0L;
    }


    /**
     * Stop searching. Long running searches can be stopped from another thread.
     */
    public void stopSearching()
    {
        stopSearching = true;
    }
}
