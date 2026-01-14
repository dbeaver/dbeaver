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
package org.jkiss.dbeaver.ui.notifications.sounds;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.notifications.NotificationSound;
import org.jkiss.dbeaver.ui.notifications.NotificationSoundProvider;

import javax.sound.sampled.*;
import java.io.File;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FileSoundProvider implements NotificationSoundProvider {
    private final File file;

    public FileSoundProvider(@NotNull File file) {
        this.file = file;
    }

    @NotNull
    @Override
    public NotificationSound create() throws DBException {
        try {
            return new ClipSound(file);
        } catch (Exception e) {
            throw new DBException("Can't load sound from file " + file, e);
        }
    }

    private static class ClipSound implements NotificationSound {
        private final Clip clip;

        private final Lock lock = new ReentrantLock();
        private final Condition donePlaying = lock.newCondition();

        public ClipSound(@NotNull File file) throws Exception {
            try (AudioInputStream is = AudioSystem.getAudioInputStream(file)) {
                final AudioFormat format = is.getFormat();
                final DataLine.Info info = new DataLine.Info(Clip.class, format);

                clip = (Clip) AudioSystem.getLine(info);
                clip.open(is);
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        try {
                            lock.lock();
                            donePlaying.signal();
                        } finally {
                            lock.unlock();
                        }
                    }
                });
            }
        }

        @Override
        public void play(float volume) {
            if (volume > 0.0f && volume < 1.0f) {
                try {
                    // https://stackoverflow.com/a/40698149
                    final FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    if (control != null) {
                        control.setValue(20f * (float) Math.log10(volume));
                    }
                } catch (IllegalArgumentException ignored) {
                    // silently ignore
                }
            }

            clip.setMicrosecondPosition(0L);
            clip.start();

            try {
                lock.lock();
                donePlaying.awaitUninterruptibly();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void close() {
            clip.close();
        }
    }
}
