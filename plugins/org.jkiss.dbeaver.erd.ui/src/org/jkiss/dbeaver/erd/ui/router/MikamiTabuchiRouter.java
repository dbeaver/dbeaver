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


import org.eclipse.draw2dl.Connection;
import org.eclipse.draw2dl.IFigure;
import org.eclipse.draw2dl.geometry.Point;
import org.eclipse.draw2dl.geometry.PointList;
import org.eclipse.draw2dl.geometry.PrecisionPoint;
import org.eclipse.draw2dl.geometry.Rectangle;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
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

    private static final Log log = Log.getLog(MikamiTabuchiRouter.class);

    private int spacing = 15;
    private final List<Rectangle> obstacles = new ArrayList<>();
    private PrecisionPoint start;
    private PrecisionPoint finish;
    private final List<OrthogonalPath> workingPaths = new ArrayList<>();
    private final List<OrthogonalPath> userPaths = new ArrayList<>();
    private final Map<OrthogonalPath, List<OrthogonalPath>> pathsToChildPaths = new HashMap<>();
    //Increase for performance, increasing this parameter lowers accuracy.

    private Set<TrialLine> trialLinesWithPenalty;
    private Map<Boolean, List<Pair<Point, Point>>> resultMap;

    private static final int SOURCE_VERTICAL_LINES = 0;
    private static final int SOURCE_HORIZONTAL_LINES = 1;
    private static final int TARGET_VERTICAL_LINES = 2;
    private static final int TARGET_HORIZONTAL_LINES = 3;

    private Map<Integer, Map<Integer, List<TrialLine>>> linesMap;
    private Pair<TrialLine, TrialLine> result;

    //In worst case scenarios line search may become laggy,
    //if after this amount iterations nothing was found -> stop
    private static final int MAX_LINE_COUNT = 50000;
    private int currentLineCount;


    IFigure clientArea;
    private boolean requiresStep;
    private boolean resultIsNotPreferred;

    public void setClientArea(IFigure clientArea) {
        this.clientArea = clientArea;
    }

    private void createLinesFromTrial(TrialLine pos, int iter) {
        double from = pos.vertical ? pos.from.y : pos.from.x;
        double start = pos.start;
        double end = pos.finish;
        double startPosition = pos.hasForbiddenStart() ? pos.creationForbiddenStart : from;
        for (double i = startPosition - (startPosition - start) / 20; i > start; i -= (startPosition - start) / 20) {
            currentLineCount++;
            createTrial(pos, iter, i);

            if (currentLineCount > MAX_LINE_COUNT) {
                return;
            }
        }
        final double finishPosition = pos.hasForbiddenFinish() ? pos.creationForbiddenFinish : from;
        for (double i = finishPosition + (end - finishPosition) / 20; i < end; i += (end - finishPosition) / 20) {
            currentLineCount++;
            createTrial(pos, iter, i);

            if (currentLineCount > MAX_LINE_COUNT) {
                return;
            }
        }
    }

    private boolean createTrial(TrialLine pos, int iteration, double i) {
        TrialLine trialLine = createTrialLine(i, !pos.vertical, pos);
        getLinesMap(trialLine, iteration).add(trialLine);
        final TrialLine interception = trialLine.findIntersection();
        if (interception != null) {
            if (lineLiesOnPreviouslyCreatedLine(trialLine.from, getInterceptionPoint(trialLine, interception), trialLine.vertical)
            || lineLiesOnPreviouslyCreatedLine(interception.from, getInterceptionPoint(trialLine, interception), interception.vertical)){
                requiresStep = true;
            }
            if (trialLinesWithPenalty.contains(trialLine) || trialLinesWithPenalty.contains(interception)) {
                requiresStep = true;
            }
            if (result == null) {
                result = new Pair<>(trialLine, interception);
                resultIsNotPreferred = requiresStep;
                requiresStep = false;
                return true;
            } else {
                Pair<TrialLine, TrialLine> trialLinePair = new Pair<>(trialLine, interception);
                int resultMultiplier = resultIsNotPreferred ? 2 : 1;
                int interceptionMultiplier = requiresStep ? 2 : 1;
                if (calculateDistance(result) * resultMultiplier > calculateDistance(trialLinePair) * interceptionMultiplier) {
                    result = trialLinePair;
                    resultIsNotPreferred = requiresStep;
                    requiresStep = false;
                    return true;
                }
                requiresStep = false;
                return false;
            }
        }
        requiresStep = false;
        return false;
    }

    boolean lineLiesOnPreviouslyCreatedLine(Point point, Point secondPoint, boolean vertical) {
        for (Pair<Point, Point> line : resultMap.get(vertical)) {
            if (vertical) {
                if (point.x - 5 <= line.getFirst().x && point.x + spacing / 5 >= line.getFirst().x) {
                    if (isInsideLine(point.y, line.getFirst().y, line.getSecond().y) ||
                            isInsideLine(secondPoint.y, line.getFirst().y, line.getSecond().y) ||
                            isInsideLine(line.getFirst().y, point.y, secondPoint.y) ||
                            isInsideLine(line.getSecond().y, point.y, secondPoint.y)) {
                        return true;
                    }

                }
            } else {
                if (point.y - spacing / 5 <= line.getFirst().y && point.y + spacing / 5 >= line.getFirst().y) {
                    if (isInsideLine(point.x, line.getFirst().x, line.getSecond().x) ||
                            isInsideLine(secondPoint.x, line.getFirst().x, line.getSecond().x) ||
                            isInsideLine(line.getFirst().x, point.x, secondPoint.x) ||
                            isInsideLine(line.getSecond().x, point.x, secondPoint.x)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isInsideLine(int pointCoordinate, int lineCoordinateFirstPoint, int lineCoordinateSecondPoint) {
        return Math.min(lineCoordinateFirstPoint, lineCoordinateSecondPoint) <= pointCoordinate
                && Math.max(lineCoordinateFirstPoint, lineCoordinateSecondPoint) >= pointCoordinate;
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

    private PrecisionPoint getInterceptionPoint(TrialLine source, TrialLine target) {
        if (source.vertical) {
            return new PrecisionPoint(source.from.x, target.from.y);
        } else {
            return new PrecisionPoint(target.from.x, source.from.y);
        }
    }

    @NotNull
    private TrialLine createTrialLine(double pos, boolean vertical, @NotNull TrialLine parentLine) {
        final TrialLine trialLine;
        PrecisionPoint point;
        if (vertical) {
            point = new PrecisionPoint(pos, parentLine.from.y);
        } else {
            point = new PrecisionPoint(parentLine.from.x, pos);
        }
        trialLine = new TrialLine(point, parentLine);
        if (trialLinesWithPenalty.contains(parentLine) || lineLiesOnPreviouslyCreatedLine(point, parentLine.from, !vertical)) {
            trialLinesWithPenalty.add(trialLine);
            requiresStep = true;
        }
        return trialLine;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }

    public boolean updateObstacle(Rectangle rectangle, Rectangle newBounds) {
        boolean result = obstacles.remove(rectangle);
        result |= obstacles.add(newBounds);
        return result;
    }

    public void addObstacle(Rectangle bounds) {
        obstacles.add(bounds);
    }

    public boolean removeObstacle(Rectangle bounds) {
        return obstacles.remove(bounds);
    }

    private PointList traceback(Pair<TrialLine, TrialLine> res, boolean saveResults) {
        PointList points = new PointList();
        TrialLine line = res.getFirst().fromSource ? res.getFirst() : res.getSecond();
        TrialLine previousLine = res.getFirst();
        PrecisionPoint point = null;
        while (line != null) {
            if (point == null || !point.equals(line.from)) {
                points.addPoint(line.from);
            }
            previousLine = line;
            point = line.from;
            line = line.getParent();
        }
        points.reverse();
        point = getInterceptionPoint(res.getFirst(), res.getSecond());
        points.addPoint(point);
        line = res.getFirst().fromSource ? res.getSecond() : res.getFirst();
        while (line != null) {
            if (!line.from.equals(point)) {
                points.addPoint(line.from);
            }
            point = line.from;
            line = line.getParent();
        }
        if (saveResults) {
            boolean vertical = !previousLine.vertical;
            for (int i = 1; i < points.size() - 2; i++) {
                resultMap.get(vertical).add(new Pair<>(points.getPoint(i), points.getPoint(i + 1)));
                vertical = !vertical;
            }
        }
        return points;
    }

    public List<OrthogonalPath> solve() {
        resetWorkingPaths();
        updateChildPaths();
        Point point = null;
        init();
        for (OrthogonalPath userPath : workingPaths) {
            if (userPath.isSourceIsChild() && point != null) {
                userPath.updateForbiddenDirection(point);
            }
            final PointList pointList = solvePath(userPath);
            if (pointList != null && pointList.size() >= 2) {
                point = pointList.getPoint(pointList.size() - 2);
            }
            userPath.setPoints(pointList);
        }
        recombineChildrenPaths();
        return Collections.unmodifiableList(userPaths);
    }

    private void init() {
        resultMap = new HashMap<>();
        resultMap.put(false, new ArrayList<>());
        resultMap.put(true, new ArrayList<>());
    }

    private void resetWorkingPaths() {
        for (OrthogonalPath workingPath : workingPaths) {
            workingPath.reset();
        }
    }

    private void updateChildPaths() {
        for (OrthogonalPath path : userPaths) {
            if (path.isDirty()) {
                List<OrthogonalPath> children = this.pathsToChildPaths.get(path);
                int previousCount = 1;
                int newCount = 1;
                if (children == null) {
                    children = new ArrayList<>();
                } else {
                    previousCount = children.size();
                }
                if (path.getBendpoints() != null) {
                    newCount = path.getBendpoints().size() + 1;
                }

                if (previousCount != newCount) {
                    children = this.regenerateChildPaths(path, children, previousCount, newCount, path.getConnection());
                }

                this.refreshChildrenEndpoints(path, children);
            }
        }
    }

    private void refreshChildrenEndpoints(OrthogonalPath path, List<OrthogonalPath> children) {
        Point previous = path.getStart();
        PointList bendPoints = path.getBendpoints();

        for (int i = 0; i < children.size(); ++i) {
            Point next;
            if (i < bendPoints.size()) {
                next = bendPoints.getPoint(i);
            } else {
                next = path.getEnd();
            }
            OrthogonalPath child = children.get(i);
            child.setStartPoint(previous);
            child.setEndPoint(next);
            previous = next;
        }
        if (children.size() > 1) {
            children.get(0).setTargetIsChild(true);
            children.get(0).setSourceIsChild(false);
            children.get(children.size() - 1).setSourceIsChild(true);
            children.get(children.size() - 1).setTargetIsChild(false);
        }
        for (int i = 1; i < children.size() - 1; i++) {
            children.get(i).setSourceIsChild(true);
            children.get(i).setTargetIsChild(true);
        }
    }

    private List<OrthogonalPath> regenerateChildPaths(OrthogonalPath path, List<OrthogonalPath> orthogonalPaths, int currentCount, int newCount, Connection connection) {
        if (currentCount == 1) {
            this.workingPaths.remove(path);
            currentCount = 0;
            orthogonalPaths = new ArrayList<>();
            this.pathsToChildPaths.put(path, orthogonalPaths);
        } else if (newCount == 1) {
            this.workingPaths.removeAll(orthogonalPaths);
            this.workingPaths.add(path);
            this.pathsToChildPaths.remove(path);
            return Collections.emptyList();
        }

        OrthogonalPath child;
        while (currentCount < newCount) {
            child = new OrthogonalPath(connection);
            orthogonalPaths.add(child);
            this.workingPaths.add(child);
            ++currentCount;
        }
        while (currentCount > newCount) {
            child = orthogonalPaths.remove(orthogonalPaths.size() - 1);
            this.workingPaths.remove(child);
            --currentCount;
        }

        return orthogonalPaths;
    }

    private double calculateDistance(Pair<TrialLine, TrialLine> res) {
        double distance = 0;
        PointList traceback = traceback(res, false);
        for (int i = 0; i < traceback.size() - 1; i++) {
            Point first = traceback.getPoint(i);
            Point second = traceback.getPoint(i + 1);
            distance += first.getDistance(second);
        }
        return distance;
    }

    @NotNull
    private PointList solvePath(OrthogonalPath path) {
        if (path.getStart().equals(path.getEnd())) {
            log.debug("Origin point is the same as Destination point");
            PointList pointList = new PointList();
            pointList.addPoint(path.getStart());
            pointList.addPoint(path.getEnd());
            return pointList;
        }
        //Client are
        if (!clientArea.getClientArea().contains(path.start) || !clientArea.getClientArea().contains(path.end)) {
            clientArea.getUpdateManager().performUpdate();
        }
        trialLinesWithPenalty = new HashSet<>();
        linesMap = new HashMap<>();
        resultIsNotPreferred = false;
        this.start = new PrecisionPoint(path.start);
        result = null;
        this.finish = new PrecisionPoint(path.end);
        int iter = 0;
        currentLineCount = 0;
        initStartingTrialLines(path, path.getForbiddenDirection());
        while (true) {
            linesMap.put(iter + 1, new HashMap<>());
            initNewLayer(iter + 1);
            boolean hasValues = false;
            for (int i = 0; i < 4; i++) {
                for (TrialLine trialLine : linesMap.get(iter).get(i)) {
                    createLinesFromTrial(trialLine, iter + 1);
                    if (currentLineCount > MAX_LINE_COUNT) {
                        if (result != null) {
                            return traceback(result, true);
                        }
//                        log.debug("[Routing] Max amount of tries exceeded, path can't be found");
                        PointList pointList = new PointList();
                        pointList.addPoint(start);
                        pointList.addPoint(finish);
                        return pointList;
                    }
                }
            }
            if (result != null && !resultIsNotPreferred) {
                return traceback(result, true);
            }
            for (int i = 0; i < 4; i++) {
                if (linesMap.get(iter + 1).get(i).size() != 0) {
                    hasValues = true;
                }
            }
            if (!hasValues) {
                PointList pointList = new PointList();
                pointList.addPoint(start);
                pointList.addPoint(finish);
                return pointList;
            }
            iter++;
        }
    }


    private void recombineChildrenPaths() {

        for (OrthogonalPath path : this.pathsToChildPaths.keySet()) {
            if (path.getPoints() != null) {
                path.getPoints().removeAllPoints();
                List<OrthogonalPath> childPaths = this.pathsToChildPaths.get(path);
                OrthogonalPath childPath = null;

                for (OrthogonalPath orthogonalPath : childPaths) {
                    childPath = orthogonalPath;
                    if (!childPath.getStart().equals(childPath.getPoints().getPoint(0))) {
                        childPath.getPoints().reverse();
                    }
                    path.getPoints().addAll(childPath.getPoints());
                    path.getPoints().removePoint(path.getPoints().size() - 1);
                }

                path.getPoints().addPoint(childPath.getPoints().getLastPoint());
            }
        }
    }

    private void initStartingTrialLines(OrthogonalPath path, OrthogonalPath.Direction forbiddenDirection) {
        //Deviation from an original algorithm, we want only lines what connect with point horizontally
        linesMap.put(0, new HashMap<>());
        initNewLayer(0);
        final TrialLine horizontalStartTrial = new TrialLine(start, true, false, forbiddenDirection);
        final TrialLine horizontalFinishTrial = new TrialLine(finish, false, false, forbiddenDirection);
        if (path.isSourceIsChild()) {
            final TrialLine verticalStartTrial = new TrialLine(start, true, true, forbiddenDirection);
            linesMap.get(0).get(SOURCE_VERTICAL_LINES).add(verticalStartTrial);
        }
        if (path.isTargetIsChild()) {
            final TrialLine verticalFinishTrial = new TrialLine(finish, false, true, forbiddenDirection);
            linesMap.get(0).get(TARGET_VERTICAL_LINES).add(verticalFinishTrial);
        }
        linesMap.get(0).get(SOURCE_HORIZONTAL_LINES).add(horizontalStartTrial);
        linesMap.get(0).get(TARGET_HORIZONTAL_LINES).add(horizontalFinishTrial);
    }

    /**
     * init list for each type of trial line
     * Source horizontal, source vertical, target horizontal, target vertical
     *
     * @param iter number of algorithm iteration
     */
    private void initNewLayer(int iter) {
        for (int i = 0; i < 4; i++) {
            linesMap.get(iter).put(i, new ArrayList<>());
        }
    }

    public void removePath(OrthogonalPath path) {
        this.userPaths.remove(path);
        List<OrthogonalPath> orthogonalPaths = this.pathsToChildPaths.get(path);
        if (orthogonalPaths == null) {
            this.workingPaths.remove(path);
        } else {
            this.workingPaths.removeAll(orthogonalPaths);
        }
    }

    public void addPath(OrthogonalPath path) {
        this.workingPaths.add(path);
        this.userPaths.add(path);
    }

    private class TrialLine {
        @Nullable
        TrialLine parent;

        @NotNull
        final PrecisionPoint from;

        double start = Double.MIN_VALUE;
        double finish = Double.MIN_VALUE;

        boolean fromSource;

        double creationForbiddenStart = Double.MIN_VALUE;
        double creationForbiddenFinish = Double.MIN_VALUE;


        boolean vertical;

        //Starting line is always inside figure, we don't want to create trial line inside it

        private void calculateForbiddenRange(OrthogonalPath.Direction forbiddenDirection) {
            for (Rectangle it : obstacles) {
                if (isInsideFigure(it)) {
                    if (vertical) {
                        creationForbiddenStart = it.getTop().y - spacing;
                        creationForbiddenFinish = it.getBottom().y + spacing;
                    } else {
                        creationForbiddenStart = it.getLeft().x - spacing;
                        creationForbiddenFinish = it.getRight().x + spacing;
                    }
                }
            }
            if (forbiddenDirection == null) {
                if (creationForbiddenStart != Double.MIN_VALUE) {
                    start = creationForbiddenStart - spacing * 2;
                }
                if (creationForbiddenFinish != Double.MIN_VALUE) {
                    finish = creationForbiddenFinish + spacing * 2;
                }
            }
            if (forbiddenDirection != null) {
                switch (forbiddenDirection) {
                    case DOWN:
                        if (vertical) {
                            start = this.from.y;
                        }
                        break;
                    case UP:
                        if (vertical) {
                            finish = this.from.y;
                        }
                        break;
                    case LEFT:
                        if (!vertical) {
                            start = this.from.x;
                        }
                        break;
                    case RIGHT:
                        if (!vertical) {
                            finish = this.from.x;
                        }
                        break;
                }
            }
        }

        public boolean hasForbiddenStart() {
            return creationForbiddenStart != Double.MIN_VALUE;
        }

        public boolean hasForbiddenFinish() {
            return creationForbiddenFinish != Double.MIN_VALUE;
        }

        TrialLine(@NotNull PrecisionPoint start, @NotNull TrialLine parent) {
            this.from = start;
            this.parent = parent;
            this.fromSource = parent.fromSource;
            this.vertical = !parent.vertical;
            cutByObstacles(false);
        }

        TrialLine(@NotNull PrecisionPoint start, boolean fromSource, boolean vertical, OrthogonalPath.Direction forbiddenDirection) {
            this.from = start;
            this.vertical = vertical;
            this.fromSource = fromSource;
            this.calculateForbiddenRange(forbiddenDirection);
            this.cutByObstacles(true);
        }

        private boolean isInsideFigure(Rectangle it) {
            return (it.getLeft().x <= from.x && it.getRight().x > from.x
                    && it.getTop().y <= from.y && it.getBottom().y > from.y);
        }

        private void cutByObstacles(boolean startingLine) {
            //Check if object is on axis with line, if it is, reduce line size
            for (Rectangle it : obstacles) {
                if (isInsideFigure(it)) {
                    if (startingLine) {
                        continue;
                    } else {
                        cut(it);
                    }
                }
                if (vertical && it.getLeft().x - spacing <= from.x && it.getRight().x + spacing > from.x
                        || !vertical && it.getTop().y - spacing <= from.y && it.getBottom().y + spacing > from.y) {
                    //object is below need to cut start
                    cut(it);
                }
            }
            if (finish == Double.MIN_VALUE) {
                if (vertical) {
                    finish = clientArea.getClientArea().getBottom().y;
                } else {
                    finish = clientArea.getClientArea().getRight().x;
                }
            }
            if (start == Double.MIN_VALUE) {
                start = vertical ? clientArea.getClientArea().getTop().y : clientArea.getClientArea().getLeft().x;
            }
        }

        private void cut(Rectangle bound) {
            double fromPosition = vertical ? from.y : from.x;
            double startPoint = vertical ? bound.getTop().y : bound.getLeft().x;
            double endPoint = vertical ? bound.getBottom().y : bound.getRight().x;
            if (fromPosition > endPoint) {
                if (start == Double.MIN_VALUE || start < endPoint + spacing) {
                    start = endPoint + spacing;
                }
            }
            if (fromPosition <= startPoint) {
                if (finish == Double.MIN_VALUE || finish > startPoint - spacing) {
                    finish = startPoint - spacing;
                }
            }
        }

        @Nullable
        public TrialLine findIntersection() {
            for (int i = linesMap.values().size() - 1; i >= 0; i--) {
                for (TrialLine trialLine : getOpposingLinesMap(this, i)) {
                    if (intersect(trialLine)) {
                        return trialLine;
                    }
                }
            }
            return null;
        }

        private boolean intersect(TrialLine line) {
            double firstLinePos = vertical ? from.x : from.y;
            double secondLinePos = vertical ? line.from.y : line.from.x;

            return firstLinePos >= line.start && firstLinePos < line.finish && secondLinePos >= start && secondLinePos < finish;
        }

        @Nullable
        public TrialLine getParent() {
            return parent;
        }
    }
}