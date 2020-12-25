/*******************************************************************************
 * Copyright (c) 2004, 2011 Tasktop Technologies and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Tasktop Technologies - initial API and implementation
 *******************************************************************************/
package org.jkiss.dbeaver.ui.notifications;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Mik Kersten
 * @author Steffen Pingel
 */
public class AnimationUtil {

	public static final long FADE_RESCHEDULE_DELAY = 80;

	public static final int FADE_IN_INCREMENT = 15;

	public static final int FADE_OUT_INCREMENT = -20;

	public static FadeJob fastFadeIn(Shell shell, IFadeListener listener) {
        return new FadeJob(shell, 2 * FADE_IN_INCREMENT, FADE_RESCHEDULE_DELAY, listener);
	}

	public static FadeJob fadeIn(Shell shell, IFadeListener listener) {
		return new FadeJob(shell, FADE_IN_INCREMENT, FADE_RESCHEDULE_DELAY, listener);
	}

	public static FadeJob fadeOut(Shell shell, IFadeListener listener) {
		return new FadeJob(shell, FADE_OUT_INCREMENT, FADE_RESCHEDULE_DELAY, listener);
	}

	public static class FadeJob extends Job {

		private final Shell shell;

		private final int increment;

		private volatile boolean stopped;

		private volatile int currentAlpha;

		private final long delay;

		private final IFadeListener fadeListener;

		public FadeJob(Shell shell, int increment, long delay, IFadeListener fadeListener) {
			super("Fading");
			if (increment < -255 || increment == 0 || increment > 255) {
				throw new IllegalArgumentException("-255 <= increment <= 255 && increment != 0"); //$NON-NLS-1$
			}
			if (delay < 1) {
				throw new IllegalArgumentException("delay must be > 0"); //$NON-NLS-1$
			}
			this.currentAlpha = shell.getAlpha();
			this.shell = shell;
			this.increment = increment;
			this.delay = delay;
			this.fadeListener = fadeListener;

			setSystem(true);
			schedule(delay);
		}

		@Override
		protected void canceling() {
			stopped = true;
		}

		private void reschedule() {
			if (stopped) {
				return;
			}
			schedule(delay);
		}

		public void cancelAndWait(final boolean setAlpha) {
			if (stopped) {
				return;
			}
			cancel();
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					if (setAlpha) {
						shell.setAlpha(getLastAlpha());
					}
				}
			});
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (stopped) {
				return Status.OK_STATUS;
			}

			currentAlpha += increment;
			if (currentAlpha <= 0) {
				currentAlpha = 0;
			} else if (currentAlpha >= 255) {
				currentAlpha = 255;
			}

			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					if (stopped) {
						return;
					}

					if (shell.isDisposed()) {
						stopped = true;
						return;
					}

					shell.setAlpha(currentAlpha);

					if (fadeListener != null) {
						fadeListener.faded(shell, currentAlpha);
					}
				}
			});

			if (currentAlpha == 0 || currentAlpha == 255) {
				stopped = true;
			}

			reschedule();
			return Status.OK_STATUS;
		}

		private int getLastAlpha() {
			return (increment < 0) ? 0 : 255;
		}

	}

	public interface IFadeListener {

		void faded(Shell shell, int alpha);

	}

}