/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.jkiss.code.NotNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.Flow;

public class WaveformRenderer implements Flow.Subscriber<ByteBuffer> {
    private final Canvas canvas;
    private final boolean isBigEndian;
    private Flow.Subscription sub;

    private final float[] ring;
    private int head = 0;
    private final Object lock = new Object();

    private final int downsample;

    public WaveformRenderer(
        @NotNull Canvas canvas,
        int capacity,
        int downsample,
        float gain,
        boolean isBigEndian
    ) {
        this.canvas = canvas;
        this.ring = new float[capacity];
        this.downsample = Math.max(1, downsample);
        this.isBigEndian = isBigEndian;

        this.canvas.addPaintListener(e -> {
            float[] snapshot;
            int headSnapshot;
            synchronized (lock) {
                snapshot = ring.clone();
                headSnapshot = head;
            }
            var gc = e.gc;
            var rect = canvas.getClientArea();
            gc.setBackground(canvas.getDisplay().getSystemColor(SWT.COLOR_WHITE));
            gc.fillRectangle(rect);
            gc.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLUE));

            int midY = rect.height / 2;
            int width = rect.width;
            int n = snapshot.length;

            for (int i = 1; i < n; i++) {
                int idx1 = (headSnapshot - (n - i + 1));
                int idx2 = (headSnapshot - (n - i));
                float s1 = snapshot[((idx1 % n) + n) % n];
                float s2 = snapshot[((idx2 % n) + n) % n];

                int x1 = width - (n - i + 1) * width / n;
                int x2 = width - (n - i) * width / n;
                int y1 = midY - (int) (s1 * gain * midY);
                int y2 = midY - (int) (s2 * gain * midY);
                gc.drawLine(x1, y1, x2, y2);
            }
        });
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.sub = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(ByteBuffer item) {
        // RESET_SIGNAL:
        if (item == null || item.remaining() == 0) {
            synchronized (lock) {
                Arrays.fill(ring, 0f);
                head = 0;
            }
            asyncRedraw();
            sub.request(1);
            return;
        }

        int count = 0;
        for (int i = item.position(); i + 1 < item.limit(); i += 2) {
            if ((count++ % downsample) != 0) {
                continue;
            }

            float v = normalize(item, i, isBigEndian);

            synchronized (lock) {
                ring[head] = v;
                head = (head + 1) % ring.length;
            }
        }

        asyncRedraw();
        sub.request(1);
    }

    private static float normalize(ByteBuffer item, int i, boolean isBigEndian) {
        ByteBuffer buf = item.duplicate().order(isBigEndian
            ? ByteOrder.BIG_ENDIAN
            : ByteOrder.LITTLE_ENDIAN);

        short sample = buf.getShort(i);
        return sample / 32768f;
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }

    private void asyncRedraw() {
        if (!canvas.isDisposed()) {
            canvas.getDisplay().asyncExec(() -> {
                if (!canvas.isDisposed()) {
                    canvas.redraw();
                }
            });
        }
    }
}