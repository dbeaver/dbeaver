/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.ui.router;


import org.eclipse.draw2dl.geometry.Point;
import org.eclipse.draw2dl.geometry.PointList;
import org.eclipse.draw2dl.geometry.PrecisionPoint;
import org.eclipse.draw2dl.geometry.Rectangle;
import org.eclipse.draw2dl.graph.Path;
import org.eclipse.jgit.annotations.Nullable;
import org.jkiss.code.NotNull;
import org.jkiss.utils.Pair;

import java.util.*;
/**
 * Mikami-Tabuchi’s Algorithm
 * 1. Expand horizontal and vertical line from source to target
 * 2. In every iteration, expand from the last expanded line by STEP_SIZE
 * 3. Continue until a line from source intersects another line from target
 * 4. Backtrace from interception
 */
//possible optimizations
//By the rules of math parallel lines couldn't collide, so we need to check only perpendicular lines of opposite source/target origin
//multi-dimensional arrays for trial lines?
public class MikamiTabuchiRouter {

    private int spacing = 4;
    private List<Rectangle> obstacles = new ArrayList<>();
    private PrecisionPoint start, finish;
    private List userPointLists = new ArrayList();
    //Increase for performance, increasing this parameter lowers accuracy.
    private static final int STEP_SIZE = 1;


    private static final int SOURCE_VERTICAL_LINES = 0;
    private static final int SOURCE_HORIZONTAL_LINES = 1;
    private static final int TARGET_VERTICAL_LINES = 2;
    private static final int TARGET_HORIZONTAL_LINES = 3;

    private Map<Integer, Map<Integer, List<TrialLine>>> linesMap;

    //In worst case scenarios line search may become laggy,
    //if after this amount iterations nothing was found -> stop
    private static final int MAX_ITER = 6;

    Rectangle clientArea;

    public void setObstacles(List<Rectangle> obstacles) {
        this.obstacles = obstacles;
    }

    public void setClientArea(Rectangle clientArea) {
        this.clientArea = clientArea;
    }



    private Pair<TrialLine, TrialLine> result;

    private void createLinesFromTrial(TrialLine pos, int iter) {
        //possible optimisation
        //We don't want to create line if line of the same orientation already crosses this point.

        float start = pos.start;
        float end = pos.finish;
        for (float i = start; i < end; i += STEP_SIZE) {
            TrialLine trialLine = createTrialLine(i, !pos.vertical, pos);
            getLinesMap(trialLine, iter).add(trialLine);
            final TrialLine interception = trialLine.findInterception();
            // We found needed line, finish execution
            if (interception != null) {
                result = new Pair<>(trialLine, interception);
                break;
            }
        }
    }

    @NotNull
    private List<TrialLine> getLinesMap(TrialLine line, int iteration) {
        if (line.vertical) {
            return line.fromSource ? linesMap.get(iteration).get(SOURCE_VERTICAL_LINES) : linesMap.get(iteration).get(TARGET_VERTICAL_LINES);
        } else {
            return line.fromSource ? linesMap.get(iteration).get(SOURCE_HORIZONTAL_LINES) : linesMap.get(iteration).get(TARGET_HORIZONTAL_LINES);
        }
    }

    @NotNull
    private List<TrialLine> getOpposingLinesMap(TrialLine line, int iteration) {
        if (line.vertical) {
            return line.fromSource ? linesMap.get(iteration).get(TARGET_HORIZONTAL_LINES) : linesMap.get(iteration).get(SOURCE_HORIZONTAL_LINES);
        } else {
            return line.fromSource ? linesMap.get(iteration).get(TARGET_VERTICAL_LINES) : linesMap.get(iteration).get(SOURCE_VERTICAL_LINES);
        }
    }

    private Point getInterceptionPoint(TrialLine source, TrialLine target) {
        if (source.vertical) {
            return new PrecisionPoint(source.from.x, target.from.y);
        } else {
            return new PrecisionPoint(target.from.x, source.from.y);
        }
    }

    private TrialLine createTrialLine(float pos, boolean vertical, @Nullable TrialLine parentLine) {
        final TrialLine trialLine;
        if (vertical) {
            trialLine = new TrialLine(new PrecisionPoint(pos, 0), parentLine);
        } else {
            trialLine = new TrialLine(new PrecisionPoint(0, pos), parentLine);
        }
        trialLine.cutByObstacles();
        return trialLine;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }

    public boolean updateObstacle(Rectangle rectangle, Rectangle newBounds) {
        return true;
    }

    public void addObstacle(Rectangle bounds) {
        obstacles.add(bounds);
    }

    public boolean removeObstacle(Rectangle bounds) {
        return true;
    }

    private List<Point> tracePath(TrialLine line) {
        List<Point> points = new LinkedList<>();
        do {
            points.add(line.from);
            line = line.getParent();
        } while (line != null);
        return points;
    }

    private PointList traceback() {
        PointList points = new PointList();
        TrialLine line = result.getFirst();
        do {
            points.addPoint(line.from);
            line = line.getParent();
        } while (line != null);
        points.reverse();
        points.addPoint(getInterceptionPoint(result.getFirst(), result.getSecond()));
        line = result.getSecond();
        do {
            points.addPoint(line.from);
            line = line.getParent();
        } while (line != null);
        return points;
    }


    public PointList solve(Point start, Point finish) {
        PointList traceback = solveConnection(start, finish);
        if (traceback != null) return traceback;
        return null;
    }

    @org.jkiss.code.Nullable
    private PointList solveConnection(Point start, Point finish) {
        linesMap = new HashMap<>();
        this.start = new PrecisionPoint(start);
        result = null;
        this.finish = new PrecisionPoint(finish);
        int iter = 0;
        initStartingTrialLines();
        if (result != null) {
            return traceback();
        }
        while (iter != MAX_ITER && result == null) {
            linesMap.put(iter + 1, new HashMap<>());
            initNewLayer(iter + 1);
            for (int i = 0; i < 4; i++) {
                for (TrialLine trialLine : linesMap.get(iter).get(i)) {
                    createLinesFromTrial(trialLine, iter + 1);
                    if (result != null) {
                        return traceback();
                    }
                }
            }
            iter++;
        }
        return null;
    }

    private void initStartingTrialLines() {
        final TrialLine horizontalStartTrial = new TrialLine(start, true, false);
        final TrialLine verticalStartTrial = new TrialLine(start, true, true);
        final TrialLine horizontalFinishTrial = new TrialLine(finish, false, false);
        final TrialLine verticalFinishTrial = new TrialLine(finish, false, true);

        linesMap.put(0, new HashMap<>());
        initNewLayer(0);
        //TODO this is bad and awful
        linesMap.get(0).get(SOURCE_HORIZONTAL_LINES).add(horizontalStartTrial);
        linesMap.get(0).get(SOURCE_VERTICAL_LINES).add(verticalStartTrial);
        linesMap.get(0).get(TARGET_HORIZONTAL_LINES).add(horizontalFinishTrial);
        linesMap.get(0).get(TARGET_VERTICAL_LINES).add(verticalFinishTrial);
        TrialLine interception = horizontalStartTrial.findInterception();

        if (interception != null) {
            result = new Pair<>(horizontalStartTrial, interception);
        }
        interception = verticalStartTrial.findInterception();
        if (interception != null) {
            result = new Pair<>(horizontalStartTrial, interception);
        }

    }

    private void initNewLayer(int iter) {
        linesMap.get(iter).put(0, new ArrayList<>());
        linesMap.get(iter).put(1, new ArrayList<>());
        linesMap.get(iter).put(2, new ArrayList<>());
        linesMap.get(iter).put(3, new ArrayList<>());
    }

    public void removePath(OrthogonalPath path) {
    }

    private class TrialLine {

        float start = -1;
        float finish = -1;
        boolean fromSource;

        final PrecisionPoint from;

        boolean vertical;


        @Nullable
        TrialLine parent;

        TrialLine(PrecisionPoint start, TrialLine parent) {
            this.from = start;
            this.parent = parent;
            this.fromSource = parent.fromSource;
            this.vertical = !parent.vertical;
            cutByObstacles();
        }

        TrialLine(PrecisionPoint start, boolean fromSource, boolean vertical) {
            this.from = start;
            this.vertical = vertical;
            this.fromSource = fromSource;
            cutByObstacles();

        }

        //Resize line if obstacle encountered, if none were encountered make line length equal to client area
        //TODO refactor and optimization &
        //possible optimisations
        //change data structure for bounds

        private void cutByObstacles() {
            //Check if object is on axis with line, if it is, reduce line size
            if (obstacles == null) {
                obstacles = new ArrayList<>();
            }
            for (Rectangle it : obstacles) {
                if (vertical && it.x - spacing <= from.x && it.x + it.width + spacing > from.x) {
                    //object is below need to cut start
                    if (from.y > it.y + it.height + spacing) {
                        if (start == -1 || start < it.y + spacing) {
                            start = it.y + it.height + spacing;
                        }
                    }
                    //object is above, need to cut finish
                    if (from.y <= it.y - spacing) {
                        if (finish == -1 || finish > it.y - spacing) {
                            finish = it.y - spacing;
                        }
                    }
                }
                if (!vertical && it.y - spacing <= from.y && it.y + it.height + spacing > from.y) {
                    //object is behind need to cut start
                    if (from.x > it.x + it.width + spacing) {
                        if (start == -1 || start < it.x + spacing) {
                            start = it.x + it.width + spacing;
                        }
                    }
                    //object is ahead, need to cut finish
                    if (from.x <= it.x - spacing) {
                        if (finish == -1 || finish > it.x - spacing) {
                            finish = it.x - spacing;
                        }
                    }
                    if (finish == -1) {
                        finish = clientArea.width - spacing;
                    }
                }
            }
            if (finish == -1) {
                if (vertical) {
                    finish = clientArea.height - spacing;
                } else {
                    finish = clientArea.width - spacing;
                }
            }
            if (start == -1) {
                start = spacing;
            }
        }

        @Nullable
        public TrialLine findInterception() {
            for (int i = linesMap.values().size() - 1; i >= 0; i--) {
                for (TrialLine trialLine : getOpposingLinesMap(this, i)) {
                    if (vertical) {
                        if (this.from.x >= this.start && this.from.x < this.finish) {
                            if (this.start < this.from.y && this.finish >= this.from.y) {
                                return trialLine;
                            }
                        }
                    } else {
                        if (this.from.y >= this.start && this.from.y < this.finish) {
                            if (this.start < this.from.x && this.finish >= this.from.x) {
                                return trialLine;
                            }
                        }
                    }
                }
            }
            return null;
        }

        public TrialLine getParent() {
            return parent;
        }
    }
}