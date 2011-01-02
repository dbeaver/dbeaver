/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
public class Finder {


    public static final int MAP_SIZE = 64 * 1024;
    public static final int MAX_SEQUENCE_SIZE = 2 * 1024;  // a search string of 2K should be enough

    private long bufferPosition = -1L;
    private ByteBuffer byteBuffer = null;
    private int currentPartFound = -1;  // relative positions
    private boolean currentPartFoundIsUnicode = false;
    private long currentPosition = 0L;  // absolute value, start of forward finds, end(exclusive) of backward finds
    private byte[] myByteFindSequence = null;
    private boolean myCaseSensitive = true;
    private BinaryContent myContent = null;
    private boolean myDirectionForward = true;
    private CharSequence myLiteral = null;
    private int myLiteralByteLength = -1;
    private Pattern myPattern = null;
    private boolean stopSearching = false;


    /**
     * Create a finder object for a sequence of characters; uses unicode and ascii traversing
     *
     * @param literal the char sequence to find
     * @param content provider to be traversed
     */
    public Finder(CharSequence literal, BinaryContent aContent)
    {
        myLiteral = literal;
        initSearchUnicodeAscii();
        myContent = aContent;
        bufferPosition = 0L;
        currentPosition = 0L;
    }


    /**
     * Create a finder object for a raw sequence of bytes
     *
     * @param sequence the byte sequence to find
     * @param content  provider to be traversed
     */
    public Finder(byte[] sequence, BinaryContent aContent)
    {
        initSearchHex(sequence);
        myContent = aContent;
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
            myDirectionForward && currentPartFound > currentPartFoundUnicode ||
            !myDirectionForward && currentPartFound < currentPartFoundUnicode)) {
            currentPartFound = currentPartFoundUnicode;
            currentPartFoundIsUnicode = true;
        }
    }


    private int findHexAsciiMatchInPart()
        throws IOException
    {
        if (myByteFindSequence == null) return -1;

        int start = 0;
        int inclusiveEnd = byteBuffer.limit() - myByteFindSequence.length;
        if (!myDirectionForward) {
            start = inclusiveEnd;
            inclusiveEnd = 0;
        }

        for (int i = start;
            myDirectionForward && i <= inclusiveEnd || !myDirectionForward && i >= inclusiveEnd;
            i += myDirectionForward ? 1 : -1)
        {
            boolean matchesSoFar = true;
            for (int j = 0; j < myByteFindSequence.length && matchesSoFar; ++j) {
                byte existing = byteBuffer.get(i + j);
                byte matcher = myByteFindSequence[j];
                if (existing != matcher) {
                    if (myCaseSensitive || existing < 'A' || existing > 'z' || matcher < 'A' ||
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
        if (myPattern == null) return -1;

        int result = Integer.MAX_VALUE;
        if (!myDirectionForward) {
            result = -1;
        }
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        Matcher matcher = myPattern.matcher(byteBuffer.asCharBuffer());

        for (int encoding = 0; encoding < 4; ++encoding) {
            while (matcher.find()) {
                int index = matcher.start() * 2 + (encoding >= 2 ? 1 : 0);
                if (myDirectionForward && result > index || !myDirectionForward && result < index) {
                    result = index;
                }
                if (myDirectionForward) {
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
        if (myContent == null) {
            return 0L;
        }

        return myContent.length();
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
        int length = currentPartFoundIsUnicode ? myLiteralByteLength : myByteFindSequence.length;
        setNewStart(resultPosition + (myDirectionForward ? 1 : length - 1));

        return new Number[]{new Long(resultPosition), new Integer(length)};
    }


    void initSearchHex(byte[] sequence)
    {
        myByteFindSequence = sequence;

        if (sequence.length > MAX_SEQUENCE_SIZE) {
            myByteFindSequence = new byte[MAX_SEQUENCE_SIZE];
            System.arraycopy(sequence, 0, myByteFindSequence, 0, MAX_SEQUENCE_SIZE);
        }

        myLiteralByteLength = myByteFindSequence.length;
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
        StringBuffer regex = new StringBuffer("\\Q");  // everything-quoted regular expression

        if (myLiteral.length() * 2 > MAX_SEQUENCE_SIZE)  // 16 bit Unicode chars
            myLiteral = myLiteral.subSequence(0, MAX_SEQUENCE_SIZE / 2);
        myLiteralByteLength = myLiteral.length() * 2;

        boolean isAsciiCompatible = true;
        byte[] tmpBytes = new byte[myLiteral.length()];
        char previous = '\0';
        for (int i = 0; i < myLiteral.length(); ++i) {
            char aChar = myLiteral.charAt(i);
            regex.append(aChar);

            if (previous == '\\' && aChar == 'E')
                regex.append("\\\\E\\Q");

            previous = aChar;

            tmpBytes[i] = (byte) aChar;
            if (aChar > 255) isAsciiCompatible = false;
        }
        regex.append("\\E");

        int ignoreCaseFlags = 0;
        if (!myCaseSensitive) ignoreCaseFlags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        myPattern = Pattern.compile(regex.toString(), ignoreCaseFlags);

        if (isAsciiCompatible)
            myByteFindSequence = tmpBytes;
    }


    ByteBuffer nextPart()
        throws IOException
    {
        long newPos = bufferPosition + byteBuffer.limit() - myLiteralByteLength + 1L;
        if (!myDirectionForward)
            newPos = bufferPosition - MAP_SIZE + myLiteralByteLength - 1L;
        if (newPos < 0L)
            newPos = 0L;

        int size = (int) Math.min(MAP_SIZE, getContentLength() - newPos);
        if (!myDirectionForward)
            size = (int) (bufferPosition + myLiteralByteLength - 1L - newPos);

        if (size < myLiteralByteLength)
            return null;
        bufferPosition = newPos;
        populatePart(size);

        return byteBuffer;
    }


    void populatePart()
        throws IOException
    {
        int size = MAP_SIZE;
        if (!myDirectionForward) {
            size = (int) Math.min(MAP_SIZE, currentPosition);
        }
        populatePart(size);
    }


    void populatePart(int size)
        throws IOException
    {
        if (myContent == null) return;

        byteBuffer = null;  // multiple FileChannel.read(byteBuffer) leak memory, so don't reuse buffer
        byteBuffer = ByteBuffer.allocate(MAP_SIZE);
//	if (byteBuffer == null)
//		byteBuffer = ByteBuffer.allocate(MAP_SIZE);
        byteBuffer.limit(size);
        byteBuffer.position(0);
//	try {
        myContent.get(byteBuffer, bufferPosition);
//	} catch (OutOfMemoryError e) {
//		byteBuffer = null;
//		byteBuffer = ByteBuffer.allocate(MAP_SIZE);
        //	byteBuffer.limit(size);
        //byteBuffer.position(0);
        //myContent.get(byteBuffer, myCurrentPosition);
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
        if (myCaseSensitive == beSensitive) return;

        myCaseSensitive = beSensitive;
        if (myLiteral != null)
            initSearchUnicodeAscii();
    }


    /**
     * Sets the search direction. The default search direction is always forward
     *
     * @param goForward set to true for forward search
     */
    public void setDirectionForward(boolean goForward)
    {
        myDirectionForward = goForward;
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
        if (!myDirectionForward) {
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
