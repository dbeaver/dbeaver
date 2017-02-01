/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * SQLReconcilingStrategy
 */
public class SQLReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension
{
    private SQLEditor editor;
    private IDocument document;

    /**
     * The offset of the next character to be read
     */
    protected int regionOffset;

    /**
     * The end offset of the range to be scanned
     */
    protected int regionLength;

    /**
     * @return Returns the editor.
     */
    public SQLEditor getEditor()
    {
        return editor;
    }

    public void setEditor(SQLEditor editor)
    {
        this.editor = editor;
    }

    @Override
    public void setDocument(IDocument document)
    {
        this.document = document;
    }

    @Override
    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion)
    {
        regionOffset = dirtyRegion.getOffset();
        regionLength = dirtyRegion.getLength();
        calculatePositions();
    }

    @Override
    public void reconcile(IRegion partition)
    {
        regionOffset = partition.getOffset();
        regionLength = partition.getLength();
        calculatePositions();
    }

    @Override
    public void setProgressMonitor(IProgressMonitor monitor)
    {
    }

    @Override
    public void initialReconcile()
    {
        regionOffset = 0;
        regionLength = document.getLength();
        calculatePositions();
    }

    protected void calculatePositions()
    {
        ProjectionAnnotationModel annotationModel = editor.getAnnotationModel();
        if (annotationModel == null) {
            return;
        }
        Set<SQLScriptPosition> removedPositions = editor.getRuleManager().getRemovedPositions(true);
        Set<SQLScriptPosition> addedPositions = editor.getRuleManager().getAddedPositions(true);

        Annotation[] removedAnnotations = null;
        if (!removedPositions.isEmpty()) {
            removedAnnotations = new Annotation[removedPositions.size()];
            int index = 0;
            for (SQLScriptPosition pos : removedPositions) {
                removedAnnotations[index++] = pos.getFoldingAnnotation();
            }
        }
        Map<Annotation, Position> addedAnnotations = null;
        if (!addedPositions.isEmpty()) {
            addedAnnotations = new HashMap<>();
            for (SQLScriptPosition pos : addedPositions) {
                addedAnnotations.put(pos.getFoldingAnnotation(), pos);
            }
        }
        if (removedAnnotations != null || addedAnnotations != null) {
            annotationModel.modifyAnnotations(
                removedAnnotations,
                addedAnnotations,
                null);
        }
/*
        final List<Position> positions;

        try {
            positions = parseRegion();
        } catch (BadLocationException e) {
            e.printStackTrace();
            return;
        }
*/

/*
        Display.getDefault().asyncExec(new Runnable()
        {
            public void run()
            {
                editor.updateFoldingStructure(regionOffset, regionLength, positions);
            }

        });
*/
    }

    /**
     * P
     * @throws BadLocationException
     *
    private List<Position> parseRegion()
        throws BadLocationException
    {
        List<Position> positions = new ArrayList<Position>();
        int endPosition = regionOffset + regionLength;
        int statementStartPos = 0;
        for (int pos = regionOffset; pos < endPosition; pos++) {
            char ch = document.getChar(pos);
            if (ch == STATEMENT_DIV) {
                positions.add(new Position(statementStartPos, pos - statementStartPos));

                statementStartPos = pos + 1;
            }
        }

        // Add trailing position
        if (statementStartPos < endPosition) {
            positions.add(new Position(statementStartPos, endPosition));
        }
        return positions;
    }*/

    /**
     * emits tokens to {@link #positions}.
     *
     * @return number of newLines
     * @throws BadLocationException
     *
    protected int recursiveTokens(int depth)
        throws BadLocationException
    {
        int newLines = 0;
        while (cNextPos < regionLength) {
            while (cNextPos < regionLength) {
                char ch = document.getChar(cNextPos++);
                switch (ch) {
                    case '<':
                        int startOffset = cNextPos - 1;
                        int startNewLines = newLines;
                        int classification = classifyTag();
                        String tagString = document.get(startOffset,
                            Math.min(cNextPos - startOffset,
                                regionLength - startOffset)); // this is to see where we are in the debugger
                        newLines += cNewLines; // cNewLines is written by
                        // classifyTag()

                        switch (classification) {
                            case START_TAG:
                                newLines += recursiveTokens(depth + 1);
                                if (newLines > startNewLines + 1) {
                                    emitPosition(startOffset, cNextPos - startOffset);
                                }
                                break;
                            case LEAF_TAG:
                                if (newLines > startNewLines + 1) {
                                    emitPosition(startOffset, cNextPos - startOffset);
                                }
                                break;
                            case COMMENT_TAG:
                                if (newLines > startNewLines + 1) {
                                    emitPosition(startOffset, cNextPos - startOffset);
                                }
                                break;
                            case PI_TAG:
                                break;
                            case END_TAG:
                            case EOR_TAG:
                                return newLines;
                            default:
                                break;
                        }
                        break;
                    case '\n':
                    case '\r':
                        if ((ch == cLastNLChar) || (' ' == cLastNLChar)) {
                            newLines++;
                            cLastNLChar = ch;
                        }
                        break;
                    default:
                        break;
                }
            }

        }
        return newLines;
    }

    protected void emitPosition(int startOffset, int length)
    {
        positions.add(new Position(startOffset, length));
    }*/

    /**
     * classsifies a tag: <br />
     * &lt;?...?&gt;: {@link #PI_TAG} <br />
     * &lt;!...--&gt;: {@link #COMMENT_TAG} <br />
     * &lt;...&gt;: {@link #START_TAG} <br />
     * &lt;.../&gt;: {@link #LEAF_TAG} <br />
     * &lt;/...&gt;: {@link #END_TAG} <br />
     * &lt;...: {@link #EOR_TAG} (end of range reached before closing &gt; is
     * found). <br />
     * when this method is called, {@link #cNextPos} must point to the character
     * after &lt;, when it returns, it points to the character after &gt; or
     * after the range. About syntax errors: this method is not a validator, it
     * is useful. Side effect: writes number of found newLines to
     * {@link #cNewLines}.
     *
     * @return the tag classification
     *
    protected int classifyTag()
    {
        try {
            char ch = document.getChar(cNextPos++);
            cNewLines = 0;

            // processing instruction?
            if ('?' == ch) {
                boolean piFlag = false;
                while (cNextPos < regionLength) {
                    ch = document.getChar(cNextPos++);
                    if (('>' == ch) && piFlag)
                        return PI_TAG;
                    piFlag = ('?' == ch);
                }
                return EOR_TAG;
            }

            // comment?
            if ('!' == ch) {
                cNextPos++; // must be '-' but we don't care if not
                cNextPos++; // must be '-' but we don't care if not
                int commEnd = 0;
                while (cNextPos < regionLength) {
                    ch = document.getChar(cNextPos++);
                    if (('>' == ch) && (commEnd >= 2))
                        return COMMENT_TAG;
                    if (('\n' == ch) || ('\r' == ch)) {
                        if ((ch == cLastNLChar) || (' ' == cLastNLChar)) {
                            cNewLines++;
                            cLastNLChar = ch;
                        }
                    }
                    if ('-' == ch) {
                        commEnd++;
                    } else {
                        commEnd = 0;
                    }
                }
                return EOR_TAG;
            }

            // consume whitespaces
            while ((' ' == ch) || ('\t' == ch) || ('\n' == ch) || ('\r' == ch)) {
                ch = document.getChar(cNextPos++);
                if (cNextPos > regionLength)
                    return EOR_TAG;
            }

            // end tag?
            if ('/' == ch) {
                while (cNextPos < regionLength) {
                    ch = document.getChar(cNextPos++);
                    if ('>' == ch) {
                        cNewLines += eatToEndOfLine();
                        return END_TAG;
                    }
                    if ('"' == ch) {
                        ch = document.getChar(cNextPos++);
                        while ((cNextPos < regionLength) && ('"' != ch)) {
                            ch = document.getChar(cNextPos++);
                        }
                    } else if ('\'' == ch) {
                        ch = document.getChar(cNextPos++);
                        while ((cNextPos < regionLength) && ('\'' != ch)) {
                            ch = document.getChar(cNextPos++);
                        }
                    }
                }
                return EOR_TAG;
            }

            // start tag or leaf tag?
            while (cNextPos < regionLength) {
                ch = document.getChar(cNextPos++);
                // end tag?
                s:
                switch (ch) {
                    case '/':
                        while (cNextPos < regionLength) {
                            ch = document.getChar(cNextPos++);
                            if ('>' == ch) {
                                cNewLines += eatToEndOfLine();
                                return LEAF_TAG;
                            }
                        }
                        return EOR_TAG;
                    case '"':
                        while (cNextPos < regionLength) {
                            ch = document.getChar(cNextPos++);
                            if ('"' == ch)
                                break s;
                        }
                        return EOR_TAG;
                    case '\'':
                        while (cNextPos < regionLength) {
                            ch = document.getChar(cNextPos++);
                            if ('\'' == ch)
                                break s;
                        }
                        return EOR_TAG;
                    case '>':
                        cNewLines += eatToEndOfLine();
                        return START_TAG;
                    default:
                        break;
                }

            }
            return EOR_TAG;

        } catch (BadLocationException e) {
            // should not happen, but we treat it as end of range
            return EOR_TAG;
        }
    }

    protected int eatToEndOfLine()
        throws BadLocationException
    {
        if (cNextPos >= regionLength) {
            return 0;
        }
        char ch = document.getChar(cNextPos++);
        // 1. eat all spaces and tabs
        while ((cNextPos < regionLength) && ((' ' == ch) || ('\t' == ch))) {
            ch = document.getChar(cNextPos++);
        }
        if (cNextPos >= regionLength) {
            cNextPos--;
            return 0;
        }

        // now ch is a new line or a non-whitespace
        if ('\n' == ch) {
            if (cNextPos < regionLength) {
                ch = document.getChar(cNextPos++);
                if ('\r' != ch) {
                    cNextPos--;
                }
            } else {
                cNextPos--;
            }
            return 1;
        }

        if ('\r' == ch) {
            if (cNextPos < regionLength) {
                ch = document.getChar(cNextPos++);
                if ('\n' != ch) {
                    cNextPos--;
                }
            } else {
                cNextPos--;
            }
            return 1;
        }

        return 0;
    }*/
}

