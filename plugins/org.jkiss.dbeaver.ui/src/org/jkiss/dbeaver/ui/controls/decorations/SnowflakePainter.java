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
package org.jkiss.dbeaver.ui.controls.decorations;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class SnowflakePainter implements Painter {
    private static final double DENSITY = 0.0001;
    private static final Point2D.Double SWING = new Point2D.Double(0.1, 1.0);
    private static final Point2D.Double SPEED = new Point2D.Double(40.0, 100.0);
    private static final Point2D.Double AMPLITUDE = new Point2D.Double(25.0, 50.0);

    private final List<Particle> particles;
    private final Random random;
    private final SnowflakeAtlas atlas;

    public SnowflakePainter(@NotNull Display display) {
        this.particles = new ArrayList<>();
        this.random = new Random();
        this.atlas = SnowflakeAtlas.generate(
            display,
            List.of(
                SnowflakeIcons.FLAKE_1,
                SnowflakeIcons.FLAKE_2,
                SnowflakeIcons.FLAKE_3,
                SnowflakeIcons.FLAKE_4,
                SnowflakeIcons.FLAKE_5,
                SnowflakeIcons.FLAKE_6,
                SnowflakeIcons.FLAKE_7,
                SnowflakeIcons.FLAKE_8,
                SnowflakeIcons.FLAKE_9,
                SnowflakeIcons.FLAKE_10,
                SnowflakeIcons.FLAKE_11,
                SnowflakeIcons.FLAKE_12
            ),
            new RGB(204, 0.70f, 0.80f),
            40,
            8,
            4
        );
    }

    @Override
    public void paint(@NotNull GC gc) {
        for (Particle particle : particles) {
            paint(gc, particle);
        }
    }

    private void paint(@NotNull GC gc, @NotNull Particle particle) {
        var clip = atlas.getClip(particle.type, particle.mip);
        gc.drawImage(
            atlas.image(),
            clip.x,
            clip.y,
            clip.width,
            clip.height,
            (int) particle.position.x,
            (int) particle.position.y,
            clip.width,
            clip.height
        );
    }

    @Override
    public void update(int width, int height, double dt) {
        int limit = computeMaxSnowflakes(width, height);
        for (var it = particles.iterator(); it.hasNext(); ) {
            Particle particle = it.next();
            particle.update(dt);

            int size = atlas.getSize(particle.mip);
            if (particle.position.y - size > height) {
                if (particles.size() <= limit) {
                    var origin = random.nextDouble(width);
                    particle.origin.setLocation(origin, particle.origin.y);
                    particle.position.setLocation(origin, -size);
                    particle.swing = random.nextDouble(100);
                } else {
                    // Sorry little fella, you have to die
                    it.remove();
                }
            }
        }
    }

    @Override
    public void reset(int width, int height) {
        int limit = computeMaxSnowflakes(width, height);
        for (int i = particles.size(); i < limit; i++) {
            particles.add(new Particle(
                new Point2D.Double(random.nextDouble(width), -random.nextDouble(height)),
                new Point2D.Double(random.nextDouble(SWING.x, SWING.y), random.nextDouble(SPEED.x, SPEED.y)),
                random.nextInt(0, atlas.count()),
                random.nextInt(0, atlas.mips()),
                random.nextDouble(AMPLITUDE.x, AMPLITUDE.y),
                random.nextDouble(100)
            ));
        }
    }

    @Override
    public void dispose() {
        particles.clear();
        atlas.dispose();
    }

    private static int computeMaxSnowflakes(int width, int height) {
        return (int) Math.max(1, width * height * DENSITY);
    }

    private static class Particle {
        private final Point2D.Double origin;
        private final Point2D.Double position;
        private final Point2D.Double velocity;
        private final int type;
        private final int mip;
        private final double amplitude;
        private double swing;

        Particle(@NotNull Point2D.Double origin, @NotNull Point2D.Double velocity, int type, int mip, double amplitude, double swing) {
            this.origin = origin;
            this.position = (Point2D.Double) origin.clone();
            this.velocity = velocity;
            this.type = type;
            this.mip = mip;
            this.amplitude = amplitude;
            this.swing = swing;
        }

        void update(double deltaTime) {
            swing += velocity.x * deltaTime;
            position.setLocation(origin.x + amplitude * Math.sin(swing), position.y + velocity.y * deltaTime);
        }
    }
}
