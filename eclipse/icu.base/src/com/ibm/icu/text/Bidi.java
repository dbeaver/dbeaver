/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.ibm.icu.text;

public class Bidi {

    public static final byte LEVEL_DEFAULT_LTR = (byte)0x7e;
    public static final byte LEVEL_DEFAULT_RTL = (byte)0x7f;
    public static final byte MAX_EXPLICIT_LEVEL = 61;
    public static final byte LEVEL_OVERRIDE = (byte)0x80;
    public static final int MAP_NOWHERE = -1;

    public static final byte LTR = 0;
    public static final byte RTL = 1;
    public static final byte MIXED = 2;

    public static final short KEEP_BASE_COMBINING = 1;
    public static final short DO_MIRRORING = 2;
    public static final short INSERT_LRM_FOR_NUMERIC = 4;
    public static final short REMOVE_BIDI_CONTROLS = 8;

    public static final short OUTPUT_REVERSE = 16;
    public static final short REORDER_DEFAULT = 0;
    public static final short REORDER_NUMBERS_SPECIAL = 1;
    public static final short REORDER_GROUP_NUMBERS_WITH_R = 2;
    public static final short REORDER_RUNS_ONLY = 3;
    public static final short REORDER_INVERSE_NUMBERS_AS_L = 4;
    public static final short REORDER_INVERSE_LIKE_DIRECT = 5;
    public static final short REORDER_INVERSE_FOR_NUMBERS_SPECIAL = 6;

    public static final int OPTION_DEFAULT = 0;

    public static final int OPTION_INSERT_MARKS = 1;
    public static final int OPTION_REMOVE_CONTROLS = 2;
    public static final int OPTION_STREAMING = 4;

    private java.text.Bidi javaBidi;

    private Bidi(java.text.Bidi javaBidi)
    {
        this.javaBidi = javaBidi;
    }

    public void setInverse(boolean isInverse) {
    }

    public boolean isInverse() {
        return false;
    }

    public void setReorderingMode(int reorderingMode) {
    }

    public int getReorderingMode() {
        return 0;
    }

    public void setReorderingOptions(int options) {
    }

    public int getReorderingOptions() {
        return 0;
    }

    public void setPara(String text, byte paraLevel, byte[] embeddingLevels)
    {
    }

    public void setPara(char[] chars, byte paraLevel, byte[] embeddingLevels)
    {
    }

    public void orderParagraphsLTR(boolean ordarParaLTR) {
    }

    public boolean isOrderParagraphsLTR() {
        return false;
    }

    public byte getDirection()
    {
        return 0;
    }

    public String getTextAsString()
    {
        return null;
    }

    public char[] getText()
    {
        return null;
    }

    public int getLength()
    {
        return 0;
    }

    public int getProcessedLength() {
        return 0;
    }

    public int getResultLength() {
        return 0;
    }

    public byte getParaLevel()
    {
        return 0;
    }

    public int countParagraphs()
    {
        return 0;
    }

    public int getParagraphIndex(int charIndex)
    {
        return 0;
    }

    public int getCustomizedClass(int c) {
        return 0;
    }

    public Bidi setLine(int start, int limit)
    {
        return this;
    }

    public byte getLevelAt(int charIndex)
    {
        return 0;
    }

    public byte[] getLevels()
    {
        return null;
    }

    public int getVisualIndex(int logicalIndex)
    {
        return 0;
    }


    public int getLogicalIndex(int visualIndex)
    {
        return 0;
    }

    public int[] getLogicalMap()
    {
        return null;
    }

    public int[] getVisualMap()
    {
        return null;
    }

    public static int[] reorderLogical(byte[] levels)
    {
        return null;
    }

    public static int[] reorderVisual(byte[] levels)
    {
        return null;
    }

    public static int[] invertMap(int[] srcMap)
    {
        return null;
    }

    public static final int DIRECTION_LEFT_TO_RIGHT = LTR;
    public static final int DIRECTION_RIGHT_TO_LEFT = RTL;
    public static final int DIRECTION_DEFAULT_LEFT_TO_RIGHT = LEVEL_DEFAULT_LTR;
    public static final int DIRECTION_DEFAULT_RIGHT_TO_LEFT = LEVEL_DEFAULT_RTL;

    public Bidi(String paragraph, int flags)
    {
        this(paragraph.toCharArray(), 0, null, 0, paragraph.length(), flags);
    }


    public Bidi(char[] text,
            int textStart,
            byte[] embeddings,
            int embStart,
            int paragraphLength,
            int flags)
    {
        javaBidi = new java.text.Bidi(text, textStart, embeddings, embStart, paragraphLength, flags);
    }

    public Bidi createLineBidi(int lineStart, int lineLimit)
    {
        return new Bidi(javaBidi.createLineBidi(lineStart, lineLimit));
    }

    public boolean isMixed()
    {
        return javaBidi.isMixed();
    }

    public boolean isLeftToRight()
    {
        return javaBidi.isLeftToRight();
    }

    public boolean isRightToLeft()
    {
        return javaBidi.isRightToLeft();
    }

    public boolean baseIsLeftToRight()
    {
        return javaBidi.baseIsLeftToRight();
    }

    public int getBaseLevel()
    {
        return javaBidi.getBaseLevel();
    }

    public int getRunCount()
    {
        return javaBidi.getRunCount();
    }

    public static boolean requiresBidi(char[] text,
            int start,
            int limit)
    {
        return java.text.Bidi.requiresBidi(text, start, limit);
    }

    public static void reorderVisually(byte[] levels,
            int levelStart,
            Object[] objects,
            int objectStart,
            int count)
    {
        java.text.Bidi.reorderVisually(levels, levelStart, objects, objectStart, count);
    }

    public String writeReordered(int options)
    {
        return null;
    }

    public static String writeReverse(String src, int options)
    {
        return "";
    }

}
