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

package org.jkiss.dbeaver.ui.charts;

import org.jfree.chart.plot.DefaultDrawingSupplier;

import java.awt.*;

/**
 * Base chart composite
 */
public class BaseChartDrawingSupplier extends DefaultDrawingSupplier {

    public static final Paint[] DBEAVER_DEFAULT_COLOR_SERIES = new Paint[] {
        new Color(206, 63, 34),
        new Color(44, 165, 233),
        new Color(138, 114, 99),
        new Color(242, 132, 35),
        new Color(124, 38, 19),
        new Color(157, 214, 245),
        new Color(34, 25, 21),
        new Color(249, 214, 205),
        new Color(71, 28, 18),
        new Color(83, 69, 60),
    };


    public BaseChartDrawingSupplier() {
        super(DBEAVER_DEFAULT_COLOR_SERIES,
            DEFAULT_FILL_PAINT_SEQUENCE,
            DEFAULT_OUTLINE_PAINT_SEQUENCE,
            DEFAULT_STROKE_SEQUENCE,
            DEFAULT_OUTLINE_STROKE_SEQUENCE,
            DEFAULT_SHAPE_SEQUENCE);
    }

}
