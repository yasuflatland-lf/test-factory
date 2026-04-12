package com.liferay.support.tools.utils;

import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.ProgressTracker;
import com.liferay.portal.kernel.util.ProgressTrackerThreadLocal;
import com.liferay.support.tools.constants.LDFPortletKeys;

import javax.portlet.PortletRequest;

/**
 * Progress bar manager.
 *
 * <p>
 * Accepts any {@link PortletRequest} so it can be driven from either an
 * {@code ActionRequest} (MVCActionCommand) or a {@code ResourceRequest}
 * (MVCResourceCommand). Liferay's {@link ProgressTracker#start(PortletRequest)}
 * and {@link ProgressTracker#finish(PortletRequest)} both accept
 * {@code PortletRequest}, so no request-type-specific adaptation is required.
 * </p>
 *
 * @author Yasuyuki Takeo
 */
public class ProgressManager {

	public void start(PortletRequest request) {
		_request = request;

		String commonProgressId = ParamUtil.getString(
			request, LDFPortletKeys.COMMON_PROGRESS_ID,
			LDFPortletKeys.COMMON_PROGRESS_ID);

		_progressTracker = new ProgressTracker(commonProgressId);
		ProgressTrackerThreadLocal.setProgressTracker(_progressTracker);
		_progressTracker.start(request);
	}

	public int getThreshold() {
		return _threshold;
	}

	public void setThreshold(int threshold) {
		_threshold = threshold;
	}

	/**
	 * Calculate the percentage of progress.
	 *
	 * @param  index current loop index
	 * @param  numberOfTotal total number of items
	 * @return percentage of progress as int, capped at {@link #getThreshold()}
	 */
	public int percentageCalcluation(long index, long numberOfTotal) {
		if (numberOfTotal <= 0) {
			return 0;
		}

		double dIndex = (double)index;
		double dNumberOfTotal = (double)numberOfTotal;

		int result = (int)(dIndex / dNumberOfTotal * 100.00);

		return (_threshold <= result) ? _threshold : result;
	}

	/**
	 * Track progress for the current iteration.
	 *
	 * @param index current loop index
	 * @param numberOfTotal total number of items
	 */
	public void trackProgress(long index, long numberOfTotal) {
		if (_progressTracker != null) {
			_loader = percentageCalcluation(index, numberOfTotal);
			_progressTracker.setPercent((int)_loader);
		}
	}

	/**
	 * Finish the progress bar. Sleeps briefly so the client has a chance to
	 * poll the final state before the tracker is torn down.
	 */
	public void finish() {
		if ((_progressTracker != null) && (_request != null)) {
			try {
				Thread.sleep(_sleep);
			}
			catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
			}

			_progressTracker.finish(_request);
		}
	}

	private double _loader = 1;
	private ProgressTracker _progressTracker;
	private PortletRequest _request;
	private long _sleep = 1500;
	private int _threshold = 100;

}
