/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

import java.awt.*;
import java.awt.geom.Point2D;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * This class is responsible for drawing various random decorations over a control.
 * <p>
 * Currently, it only provides winter decorations, such as snowflakes.
 *
 * @see #install(Control)
 * @see #isEnabled()
 */
public class HolidayDecorations {
    public static final String PREF_UI_SHOW_HOLIDAY_DECORATIONS = "ui.show.holiday.decorations"; //$NON-NLS-1$

    private static final boolean ENABLED = isEnabled0();

    private static final double PARTICLES_PER_PIXEL = 0.0001;
    private static final Point2D.Double SIZE = new Point2D.Double(3.0, 15.0);
    private static final Point2D.Double SWING = new Point2D.Double(0.1, 1.0);
    private static final Point2D.Double SPEED = new Point2D.Double(40, 100);
    private static final Point2D.Double AMPLITUDE = new Point2D.Double(25, 50);
    private static final Point2D.Double SKEW = new Point2D.Double(Math.PI, Math.PI * 2);
    private static final Point PHASES = new Point(4, 8);

    private final Random random;
    private final List<Particle> particles;
    private double frameTime;

    private HolidayDecorations(@NotNull Control control) {
        this.particles = new ArrayList<>();
        this.random = new Random();

        final Listener listener = event -> {
            switch (event.type) {
                case SWT.Resize -> reset(((Control) event.widget).getSize());
                case SWT.Paint -> draw(event.gc);
            }
        };

        control.addListener(SWT.Resize, listener);
        control.addListener(SWT.Paint, listener);
    }

    public static void install(@NotNull Control control) {
        if (!isEnabled()) {
            return;
        }

        // Is there any better way to obtain display's refresh rate?
        final GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = environment.getDefaultScreenDevice();
        final int refreshRate = 1000 / Math.max(device.getDisplayMode().getRefreshRate(), 60);

        final HolidayDecorations decorations = new HolidayDecorations(control);
        final Display display = Display.getCurrent();

        final Runnable timer = new Runnable() {
            @Override
            public void run() {
                if (control.isDisposed()) {
                    return;
                }

                decorations.update(control.getSize());

                if (control.isVisible()) {
                    display.asyncExec(() -> {
                        if (!control.isDisposed()) {
                            control.redraw();
                        }
                    });

                    display.timerExec(refreshRate, this);
                } else {
                    // Throttle a bit
                    display.timerExec(refreshRate * 10, this);
                }
            }
        };

        display.timerExec(refreshRate, timer);

        UIUtils.enableDoubleBuffering(control);
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    private void reset(@NotNull Point size) {
        if (size.x <= 0 || size.y <= 0) {
            return;
        }

        frameTime = System.currentTimeMillis() / 1000.0;

        if (particles.isEmpty()) {
            final int count = (int) (size.x * size.y * PARTICLES_PER_PIXEL);
            for (int i = 0; i < count; i++) {
                particles.add(new Particle(
                    new Point2D.Double(random.nextDouble(size.x), -random.nextDouble(size.y)),
                    new Point2D.Double(random.nextDouble(SWING.x, SWING.y), random.nextDouble(SPEED.x, SPEED.y)),
                    random.nextDouble(SIZE.x, SIZE.y),
                    random.nextDouble(AMPLITUDE.x, AMPLITUDE.y),
                    random.nextDouble(SKEW.x, SKEW.y),
                    random.nextInt(PHASES.x, PHASES.y),
                    random.nextDouble(100)
                ));
            }
        }
    }

    private void update(@NotNull Point size) {
        if (size.x <= 0 || size.y <= 0) {
            return;
        }

        final double currentTime = System.currentTimeMillis() / 1000.0;
        final double deltaTime = currentTime - frameTime;

        for (Particle particle : particles) {
            particle.update(deltaTime);

            if (particle.position.getY() - particle.size > size.y) {
                final double origin = random.nextDouble(size.x);
                particle.origin.setLocation(origin, particle.origin.getY());
                particle.position.setLocation(origin, -particle.size);
                particle.swing = random.nextDouble(100);
            }
        }

        frameTime = currentTime;
    }

    private void draw(@NotNull GC gc) {
        gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

        for (Particle particle : particles) {
            drawSnowflake(gc, particle);
        }
    }

    private static void drawSnowflake(@NotNull GC gc, @NotNull Particle particle) {
        final double x = particle.position.getX();
        final double y = particle.position.getY();
        final double size1 = particle.size;
        final double size2 = particle.size * 0.75;

        final int steps = particle.phase;
        final double step1 = Math.PI * 2 / steps;
        final double step2 = Math.PI / 180 * 30;

        for (int i = 0; i < steps; i++) {
            final double angle = Math.PI + step1 * i + particle.rotation;

            gc.drawLine(
                (int) x,
                (int) y,
                (int) (x + size1 * Math.sin(angle)),
                (int) (y + size1 * Math.cos(angle))
            );

            gc.drawLine(
                (int) (x + 5 * Math.sin(angle)),
                (int) (y + 5 * Math.cos(angle)),
                (int) (x + size2 * Math.sin(angle + step2)),
                (int) (y + size2 * Math.cos(angle + step2))
            );

            gc.drawLine(
                (int) (x + 5 * Math.sin(angle)),
                (int) (y + 5 * Math.cos(angle)),
                (int) (x + size2 * Math.sin(angle - step2)),
                (int) (y + size2 * Math.cos(angle - step2))
            );
        }
    }

    private static boolean isEnabled0() {
        final boolean enabled = DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_UI_SHOW_HOLIDAY_DECORATIONS);

        if (enabled) { // Dec 20 <= Cur <= Jan 1
            final LocalDate current = LocalDate.now();
            return switch (current.getMonth()) {
                case DECEMBER -> current.getDayOfMonth() >= 20;
                case JANUARY -> current.getDayOfMonth() == 1;
                default -> false;
            };
        }

        return false;
    }

    private static class Particle {
        private final Point2D origin;
        private final Point2D position;
        private final Point2D velocity;
        private final double size;
        private final double amplitude;
        private final double skew;
        private final int phase;
        private double rotation;
        private double swing;

        public Particle(@NotNull Point2D origin, @NotNull Point2D velocity, double size, double amplitude, double skew, int phase, double swing) {
            this.origin = origin;
            this.position = (Point2D) origin.clone();
            this.velocity = velocity;
            this.size = size;
            this.amplitude = amplitude;
            this.skew = skew;
            this.phase = phase;
            this.swing = swing;
        }

        public void update(double deltaTime) {
            swing += velocity.getX() * deltaTime;
            rotation = skew * Math.sin(swing) / amplitude;
            position.setLocation(origin.getX() + amplitude * Math.sin(swing), position.getY() + velocity.getY() * deltaTime);
        }
    }
}
