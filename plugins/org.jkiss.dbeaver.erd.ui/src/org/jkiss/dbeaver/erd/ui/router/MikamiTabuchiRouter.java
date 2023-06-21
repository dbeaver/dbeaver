/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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


import org.eclipse.draw2d.Connection;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PointList;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.Rectangle;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.Pair;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Mikami-Tabuchi’s Algorithm
 * 1. Expand horizontal and vertical line from source to target
 * 2. In every iteration, expand from the last expanded line by STEP_SIZE
 * 3. Continue until a line from source intersects another line from target
 * 4. Backtrace from interception
 */
public class MikamiTabuchiRouter {

    private static final Log log = Log.getLog(MikamiTabuchiRouter.class);
    private static final int SOURCE_VERTICAL_LINES = 0;
    private static final int SOURCE_HORIZONTAL_LINES = 1;
    private static final int TARGET_VERTICAL_LINES = 2;
    private static final int TARGET_HORIZONTAL_LINES = 3;

    private int spacing = 15;
    private final Set<Rectangle> obstacles = new HashSet<>();

    private PrecisionPoint start;
    private PrecisionPoint finish;

    private OrthogonalPath activePath;
    private final List<OrthogonalPath> userPaths = new ArrayList<>();
    private final Map<OrthogonalPath, List<OrthogonalPath>> pathsToChildPaths = new HashMap<>();

    private final Map<OrthogonalPath, Map<Boolean, List<Pair<Point, Point>>>> resultMap = new HashMap<>();


    private final Map<Integer, Map<Integer, List<TrialLine>>> linesMap = new ConcurrentHashMap<>();
    private ResultPairWithFine result;

    //In worst case scenarios line search may become laggy,
    //if after this amount iterations nothing was found -> stop
    private static final int MAX_LINE_COUNT = 100000;
    private final AtomicInteger currentLineCount = new AtomicInteger();


    IFigure clientArea;

    public void setClientArea(IFigure clientArea) {
        this.clientArea = clientArea;
    }

    private void createLinesFromTrial(TrialLine pos, int iteration) {
        double from = pos.vertical ? pos.from.y : pos.from.x;
        double start = pos.start;
        double end = pos.finish;
        double startPosition = pos.hasForbiddenStart() ? pos.creationForbiddenStart : from;
        for (double i = startPosition - (startPosition - start) / 20; i > start; i -= (startPosition - start) / 20) {

            createTrial(pos, iteration, i);

            if (currentLineCount.incrementAndGet() > MAX_LINE_COUNT) {
                return;
            }
        }
        final double finishPosition = pos.hasForbiddenFinish() ? pos.creationForbiddenFinish : from;
        for (double i = finishPosition + (end - finishPosition) / 20; i < end; i += (end - finishPosition) / 20) {
            createTrial(pos, iteration, i);

            if (currentLineCount.incrementAndGet() > MAX_LINE_COUNT) {
                return;
            }
        }
    }

    private boolean createTrial(TrialLine pos, int iteration, double i) {
        TrialLine trialLine = createTrialLine(i, !pos.vertical, pos);
        getLinesMap(trialLine, iteration).add(trialLine);
        final TrialLine interception = trialLine.findIntersection();
        if (interception != null) {
            boolean isFined = trialLine.requiresStep || interception.requiresStep;
            if (!activePath.isSourceIsChild() && !activePath.isTargetIsChild()) {
                if (lineLiesOnPreviouslyCreatedLine(trialLine.from, getInterceptionPoint(trialLine, interception), trialLine.vertical)
                        || lineLiesOnPreviouslyCreatedLine(interception.from, getInterceptionPoint(trialLine, interception), interception.vertical)) {
                    isFined = true;
                }
            }

            synchronized (this) {
                if (result == null) {
                    result = new ResultPairWithFine(isFined, new Pair<>(trialLine, interception));
                    return true;
                } else {
                    Pair<TrialLine, TrialLine> trialLinePair = new Pair<>(trialLine, interception);
                    int resultMultiplier = result.fined ? 2 : 1;
                    int interceptionMultiplier = isFined ? 2 : 1;
                    if (calculateDistance(result.pair) * resultMultiplier > calculateDistance(trialLinePair) * interceptionMultiplier) {
                        result = new ResultPairWithFine(isFined, trialLinePair);
                        return true;
                    }
                    if (calculateDistance(result.pair) * resultMultiplier == calculateDistance(trialLinePair) * interceptionMultiplier) {
                        if (getInterceptionPoint(result.pair.getFirst(), result.pair.getSecond()).getDistance(start) > getInterceptionPoint(interception, trialLine).getDistance(start)) {
                            result = new ResultPairWithFine(isFined, trialLinePair);
                            return true;
                        }
                    }
                    return false;
                }

            }
        }
        return false;
    }

    boolean lineLiesOnPreviouslyCreatedLine(Point point, Point secondPoint, boolean vertical) {

        final List<Pair<Point, Point>> collect = new ArrayList<>();
        for (Map<Boolean, List<Pair<Point, Point>>> it : resultMap.values()) {
            List<Pair<Point, Point>> pairs = it.get(vertical);
            collect.addAll(pairs);
        }
        for (Pair<Point, Point> line : collect) {
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
        if ((trialLine.parent != null && trialLine.parent.requiresStep) || lineLiesOnPreviouslyCreatedLine(point, parentLine.from, !vertical)) {
            trialLine.requiresStep = true;
        }
        return trialLine;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }

    public boolean updateObstacle(Rectangle rectangle, Rectangle newBounds) {
        obstacles.remove(rectangle);
        obstacles.add(newBounds);
        return true;
    }

    public void addObstacle(Rectangle bounds) {
        obstacles.add(bounds);
    }

    public boolean removeObstacle(Rectangle bounds) {
        return obstacles.remove(bounds);
    }

    private PointList traceback(Pair<TrialLine, TrialLine> res) {
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
        boolean vertical = !previousLine.vertical;
        for (int i = 1; i < points.size() - 2; i++) {
            resultMap.get(activePath).get(vertical).add(new Pair<>(points.getPoint(i), points.getPoint(i + 1)));
            vertical = !vertical;
        }
        return points;
    }

    public void sortWorkingPaths() {
        userPaths.sort(Comparator.comparingInt(o -> o.start.x + o.start.y + o.end.x + o.end.y));
    }

    public List<OrthogonalPath> solve() {
        sortWorkingPaths();
        updateChildPaths();
        final List<OrthogonalPath> dirtyPaths = userPaths.stream().filter(OrthogonalPath::isDirty).collect(Collectors.toList());
        refreshResultMap(dirtyPaths);
        for (OrthogonalPath userPath : dirtyPaths) {
            List<OrthogonalPath> childPaths = pathsToChildPaths.get(userPath);
            if (childPaths == null) {
                updatePath(userPath, null);
            } else {
                Point point = null;
                for (OrthogonalPath childPath : childPaths) {
                    point = updatePath(childPath, point);
                }
            }
        }
        linesMap.clear();
        recombineChildrenPaths();
         userPaths.stream().filter(Objects::nonNull).filter(path -> path.getPoints().size() != 2).forEach(path -> path.setDirty(false));
        for (List<OrthogonalPath> value : pathsToChildPaths.values()) {
            value.stream().filter(path -> path.getPoints().size() != 2).forEach(path -> path.setDirty(false));
        }
        return Collections.unmodifiableList(userPaths);
    }

    private void refreshResultMap(List<OrthogonalPath> dirtyPaths) {
        for (OrthogonalPath dirtyPath : dirtyPaths) {
            List<OrthogonalPath> childPaths = pathsToChildPaths.get(dirtyPath);
            if (childPaths == null) {
                init(dirtyPath);
            } else {
                for (OrthogonalPath childPath : childPaths) {
                    init(childPath);
                }
            }
        }
    }

    @Nullable
    private Point updatePath(OrthogonalPath userPath,@Nullable Point point) {

        activePath = userPath;
        if (userPath.isSourceIsChild() && point != null) {
            userPath.updateForbiddenDirection(point);
        }
        PointList pointList = solvePath(userPath);
        if (pointList.size() >= 2) {
            point = pointList.getPoint(pointList.size() - 2);
        }
        userPath.setPoints(pointList);
        return point;
    }

    private void init(OrthogonalPath path) {
        if (path.isDirty() || resultMap.get(path) == null) {
            resultMap.put(path, new HashMap<>());
            resultMap.get(path).put(false, new ArrayList<>());
            resultMap.get(path).put(true, new ArrayList<>());
        }
    }

    private void resetUserPaths() {
        for (OrthogonalPath path : userPaths) {
            path.reset();
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
            currentCount = 0;
            orthogonalPaths = new ArrayList<>();
            this.pathsToChildPaths.put(path, orthogonalPaths);
        } else if (newCount == 1) {
            this.pathsToChildPaths.remove(path);
            return Collections.emptyList();
        }

        OrthogonalPath child;
        while (currentCount < newCount) {
            child = new OrthogonalPath(connection);
            orthogonalPaths.add(child);
            ++currentCount;
        }
        while (currentCount > newCount) {
            orthogonalPaths.remove(orthogonalPaths.size() - 1);
            --currentCount;
        }

        return orthogonalPaths;
    }

    private double calculateDistance(Pair<TrialLine, TrialLine> res) {
        final PrecisionPoint interceptionPoint = getInterceptionPoint(res.getFirst(), res.getSecond());
        return res.getFirst().distance +
            interceptionPoint.getDistance(res.getFirst().from) +
            interceptionPoint.getDistance(res.getSecond().from) + res.getSecond().distance;
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
        if (!clientArea.getClientArea().contains(path.start) || !clientArea.getClientArea().contains(path.end)) {
            clientArea.getUpdateManager().performUpdate();
        }
        linesMap.clear();
        this.start = new PrecisionPoint(path.start);
        result = null;
        this.finish = new PrecisionPoint(path.end);
        int iteration = 0;
        currentLineCount.lazySet(0);
        initStartingTrialLines(path, path.getForbiddenDirection());
        while (true) {
            linesMap.put(iteration + 1, new ConcurrentHashMap<>());
            initNewLayer(iteration + 1);
            boolean hasValues = false;
            for (int i = 0; i < 4; i++) {
                int finalIteration = iteration;
                linesMap.get(iteration).get(i).parallelStream().forEach(it -> createLinesFromTrial(it, finalIteration + 1));
            }
            if (currentLineCount.get() > MAX_LINE_COUNT) {
                if (result != null) {
                    return traceback(result.pair);
                }
                log.debug("[Routing] Max amount of tries exceeded, path can't be found");
                PointList pointList = new PointList();
                pointList.addPoint(start);
                pointList.addPoint(finish);
                return pointList;
            }
            if (result != null && !result.fined) {
                return traceback(result.pair);
            }
            for (int i = 0; i < 4; i++) {
                if (linesMap.get(iteration + 1).get(i).size() != 0) {
                    hasValues = true;
                }
            }
            if (!hasValues) {
                PointList pointList = new PointList();
                pointList.addPoint(start);
                pointList.addPoint(finish);
                return pointList;
            }
            iteration++;
        }
    }


    private void recombineChildrenPaths() {

        for (OrthogonalPath path : this.pathsToChildPaths.keySet()) {
            if (path.getPoints() != null) {
                path.getPoints().removeAllPoints();
            } else {
                path.setPoints(new PointList());
            }
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
     * @param iteration number of algorithm iteration
     */
    private void initNewLayer(int iteration) {
        for (int i = 0; i < 4; i++) {
            linesMap.get(iteration).put(i, Collections.synchronizedList(new ArrayList<>()));
        }
    }

    public void removePath(OrthogonalPath path) {
        this.userPaths.remove(path);
        List<OrthogonalPath> orthogonalPaths = this.pathsToChildPaths.get(path);
        resultMap.remove(path);
        if (orthogonalPaths != null) {
            for (OrthogonalPath orthogonalPath : orthogonalPaths) {
                resultMap.remove(orthogonalPath);
            }
            this.userPaths.remove(path);
        }
    }

    public void addPath(OrthogonalPath path) {
        path.setDirty(true);
        this.userPaths.add(path);
    }

    private class TrialLine {
        @Nullable
        private TrialLine parent;

        @NotNull
        private final PrecisionPoint from;
        private final boolean fromSource;
        private final boolean vertical;

        double distance = 0;

        private boolean requiresStep = false;
        private double start = Double.MIN_VALUE;
        private double finish = Double.MIN_VALUE;


        private double creationForbiddenStart = Double.MIN_VALUE;
        private double creationForbiddenFinish = Double.MIN_VALUE;


        //Starting line is always inside figure, we don't want to create trial line inside it

        /**
         * Due to starting point lying inside the figure we need to limit range where trial lines can be created
         */
        private void calculateForbiddenRange() {
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
        }

        /**
         * if path is child we need to restrict starting lines from moving into the same direction as it's parent last line
         */
        private void restrictByDirection(OrthogonalPath.Direction forbiddenDirection) {
            if (forbiddenDirection != null) {
                switch (forbiddenDirection) {
                    case UP:
                        if (vertical) {
                            start = this.from.y + spacing;
                        }
                        break;
                    case DOWN:
                        if (vertical) {
                            finish = this.from.y - spacing;
                        }
                        break;
                    case LEFT:
                        if (!vertical) {
                            start = this.from.x - spacing;
                        }
                        break;
                    case RIGHT:
                        if (!vertical) {
                            finish = this.from.x + spacing;
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
            distance += start.getDistance(parent.from);
            this.fromSource = parent.fromSource;
            this.vertical = !parent.vertical;
            cutByObstacles(false);
        }

        TrialLine(@NotNull PrecisionPoint start, boolean fromSource, boolean vertical, OrthogonalPath.Direction forbiddenDirection) {
            this.from = start;
            this.vertical = vertical;
            this.fromSource = fromSource;
            this.calculateForbiddenRange();
            if (fromSource) {
                this.restrictByDirection(forbiddenDirection);
            }
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
                    finish = clientArea.getClientArea().getBottom().y - 1;
                } else {
                    finish = clientArea.getClientArea().getRight().x - 1;
                }
            }
            if (start == Double.MIN_VALUE) {
                start = vertical ? clientArea.getClientArea().getTop().y + 1 : clientArea.getClientArea().getLeft().x + 1;
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

    private class ResultPairWithFine {
        boolean fined;
        private final Pair<TrialLine, TrialLine> pair;

        public ResultPairWithFine(boolean fined, Pair<TrialLine, TrialLine> pair) {
            this.fined = fined;
            this.pair = pair;
        }
    }

}