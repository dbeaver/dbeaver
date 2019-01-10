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

import org.jkiss.dbeaver.ui.editors.binary.BinaryContent.Range;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of actions performed on a BinaryContent so they can be undone and redone.
 * Actions can be single or block deletes, inserts or overwrites.
 * Consecutive single actions are merged into a block action if they are of the same type, their data
 * is contiguous, and are performed with a time difference lower than MERGE_TIME.
 * Block actions are sequences of BinaryContent.Range. Single actions are one range of size 1.
 *
 * @author Jordi
 */
public class ActionHistory {

    static enum ActionType {
        DELETE,
        INSERT,
        OVERWRITE
    }

    /**
     * Waiting time before a single action is considered separate from the previous one.
     * Current value is 1500 milliseconds.
     */
    private static final int MERGE_TIME = 1500;  // milliseconds

    private BinaryContent.Range actionLastRange = null;
    private BinaryContent content = null;
    private List<Integer> deletedList = null;  // of Integers
    private boolean isBackspace = false;
    private List<Object[]> actionList = null;  // contains ArrayLists (from currentAction)
    private int actionsIndex = 0;
    private List<Range> currentAction = null;  // contains Ranges
    private ActionType currentActionType = null;
    private long mergedSinglesTop = -1L;
    private boolean mergingSingles = false;
    private long previousTime = 0L;
    private long newRangeLength = -1L;
    private long newRangePosition = -1L;

    /**
     * Create new action history storage object
     */
    ActionHistory(BinaryContent aContent)
    {
        if (aContent == null)
            throw new NullPointerException("null content");

        content = aContent;
        actionList = new ArrayList<>();
    }


    private long actionExclusiveEnd()
    {
        long result = 0L;
        if (currentAction != null && currentAction.size() > 0) {
            BinaryContent.Range highest =
                currentAction.get(currentAction.size() - 1);
            result = highest.exclusiveEnd();
        }
        long newRangeExclusiveEnd = newRangePosition + newRangeLength;
        if (newRangeExclusiveEnd > result)
            result = newRangeExclusiveEnd;

        return result;
    }


    private long actionPosition()
    {
        long result = -1L;
        if (currentAction != null && currentAction.size() > 0) {
            BinaryContent.Range lowest = currentAction.get(0);
            result = lowest.position;
        }
        if (result < 0 || newRangePosition >= 0 && newRangePosition < result)
            result = newRangePosition;

        return result;
    }


    /**
     * Adds a list of deleted integers to the current action. If possible, merges integerList with the list
     * in the previous call to this method.
     *
     * @param position    starting delete point
     * @param integerList deleted integers
     * @param isSingle    used when integerList.size == 1 to tell whether it is a single or a piece of a block
     *                    delete. When integerList.size() > 1 (a block delete for sure) isSingle is ignored.
     */
    void addDeleted(long position, List<Integer> integerList, boolean isSingle)
    {
        if (integerList.size() > 1L || !isSingle) {  // block delete
            BinaryContent.Range range = newRangeFromIntegerList(position, integerList);
            List<Range> oneElementList = new ArrayList<>();
            oneElementList.add(range);
            addLostRanges(oneElementList);
        } else {
            addLostByte(position, integerList.get(0));
        }
        previousTime = System.currentTimeMillis();
    }


    void addLostByte(long position, Integer integer)
    {
        if (deletedList == null)
            deletedList = new ArrayList<>();

        updateNewRange(position);
        if (isBackspace) {
            deletedList.add(0, integer);
        } else {  // delete(Del) or overwrite
            deletedList.add(integer);
        }
        previousTime = System.currentTimeMillis();
    }


    void addLostRange(BinaryContent.Range aRange)
    {
        if (mergingSingles) {
            if (mergedSinglesTop < 0L) {
                mergedSinglesTop = aRange.exclusiveEnd();
                // merging singles shifts aRange
            } else if (currentActionType == ActionType.DELETE && !isBackspace) {
                aRange.position = mergedSinglesTop++;
            }
            previousTime = System.currentTimeMillis();
        }
        mergeRange(aRange);
    }


    void addLostRanges(java.util.List<Range> ranges)
    {
        if (ranges == null)
            return;

        for (Range range : ranges) {
            addLostRange(range);
        }
    }


    void addRangeToCurrentAction(Range aRange)
    {
        if (actionPosition() <= aRange.position) {  // they're == when ending an overwrite action
            currentAction.add(aRange);
        } else {
            currentAction.add(0, aRange);
        }
        actionLastRange = aRange;
    }


    /**
     * Adds an inserted range to a new action. Does not merge Ranges nor single actions.
     *
     * @param aRange the range being inserted
     */
    void addInserted(BinaryContent.Range aRange)
    {
        currentAction.add(aRange);
        endAction();
    }


    /**
     * Tells whether a redo is possible
     *
     * @return true if something can be redone
     */
    public boolean canRedo()
    {
        return actionsIndex < actionList.size() && currentAction == null;
    }


    /**
     * Tells whether an undo is possible
     *
     * @return true if something can be undone
     */
    public boolean canUndo()
    {
        return currentAction != null || actionsIndex > 0;
    }


    void dispose()
    {
        if (actionList != null) {
            for (Object[] tuple : actionList) {
                @SuppressWarnings("unchecked")
				List<Range> ranges = (List<Range>) tuple[1];
                disposeRanges(ranges);
            }
            actionList = null;
        }
        if (currentAction != null) {
            disposeRanges(currentAction);
            currentAction = null;
        }
    }


    private void disposeRanges(java.util.List<Range> ranges)
    {
        if (ranges == null) {
            return;
        }

        for (Range range : ranges) {
            if (range.data instanceof Closeable) {
                ContentUtils.close((Closeable) range.data);
            }
        }
    }


    /**
     * Sets the last processed action as finished. Calling this method will prevent single action merging.
     * Must be called after each block action.
     */
    void endAction()
    {
        if (currentAction == null) return;

        if (mergingSingles)
            newRangeToCurrentAction();
        Object[] tuple = {currentActionType, currentAction};
        actionList.subList(actionsIndex, actionList.size()).clear();
        actionList.add(tuple);
        actionsIndex = actionList.size();

        isBackspace = false;
        currentActionType = null;
        currentAction = null;
        actionLastRange = null;
        newRangePosition = -1L;
        newRangeLength = -1L;
        mergedSinglesTop = -1L;
    }


    /**
     * User event: single/block delete/insert/overwrite. Called before any change has been done
     *
     * @param type
     * @param position
     */
    void eventPreModify(ActionType type, long position, boolean isSingle)
    {
        if (type != currentActionType ||
            !isSingle ||
            System.currentTimeMillis() - previousTime > MERGE_TIME ||
            (type == ActionType.INSERT || type == ActionType.OVERWRITE) && actionExclusiveEnd() != position ||
            type == ActionType.DELETE && actionPosition() != position && actionPosition() - 1L != position) {
            startAction(type, isSingle);
        } else {
            isBackspace = actionPosition() > position;
        }
        if (isSingle && type == ActionType.INSERT) {  // never calls addInserted...
            updateNewRange(position);
            previousTime = System.currentTimeMillis();
        }
    }


    /**
     * Closes all files for termination
     *
     * @see Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable
    {
        dispose();
        super.finalize();
    }


    private void mergeRange(BinaryContent.Range aRange)
    {
        if (actionLastRange == null || actionLastRange.data != aRange.data) {
            newRangeToCurrentAction();
            addRangeToCurrentAction(aRange);
        } else {
            if (actionLastRange.compareTo(aRange) > 0) {
                actionLastRange.position -= aRange.length;
                actionLastRange.dataOffset -= aRange.length;
                newRangePosition = aRange.position;
            }
            actionLastRange.length += aRange.length;
        }
        if (currentActionType == ActionType.OVERWRITE && mergingSingles) {
            if (newRangePosition < 0L) {
                newRangePosition = aRange.position;
                newRangeLength = 1L;
            } else {
                ++newRangeLength;
            }
        }
    }


    private ByteBuffer newBufferFromIntegerList(List<Integer> integerList)
    {
        ByteBuffer store = ByteBuffer.allocate(integerList.size());
        for (Integer anIntegerList : integerList) {
            store.put(anIntegerList.byteValue());
        }
        store.position(0);

        return store;
    }


    private BinaryContent.Range newRangeFromIntegerList(long position, List<Integer> integerList)
    {
        ByteBuffer store = newBufferFromIntegerList(integerList);

        return new BinaryContent.Range(position, store, true);
    }


    private void newRangeToCurrentAction()
    {
        BinaryContent.Range newRange;
        if (currentActionType == ActionType.DELETE) {
            if (deletedList == null)
                return;

            newRange = newRangeFromIntegerList(newRangePosition, deletedList);
            deletedList = null;
        } else {  // currentActionType == INSERT || currentActionType == OVERWRITE
            if (newRangePosition < 0L)
                return;

            content.actionsOn(false);
            content.commitChanges();
            content.actionsOn(true);
            newRange = (BinaryContent.Range) content.getRangeAt(newRangePosition).clone();
        }
        addRangeToCurrentAction(newRange);
    }


    /**
     * Redoes last action on BinaryContent.
     */
    Object[] redoAction()
    {
        if (!canRedo()) return null;

        return actionList.get(actionsIndex++);
    }


    /**
     * Starts the processing of a new action.
     *
     * @param type     one of DELETE, INSERT or OVERWRITE
     * @param isSingle whether the action is a single byte or more
     */
    private void startAction(ActionType type, boolean isSingle)
    {
        endAction();
        currentAction = new ArrayList<>();
        currentActionType = type;
        mergingSingles = isSingle;
    }


    public String toString()
    {
        return actionList.toString();
    }


    /**
     * Undoes last action on BinaryContent.
     */
    Object[] undoAction()
    {
        if (!canUndo()) return null;

        endAction();
        --actionsIndex;

        return actionList.get(actionsIndex);
    }


    private void updateNewRange(long position)
    {
        if (newRangePosition < 0L) {
            newRangePosition = position;
            newRangeLength = 1L;
        } else {
            if (newRangePosition > position) {  // Backspace (BS)
                newRangePosition = position;
            }
            ++newRangeLength;
        }
    }
}
