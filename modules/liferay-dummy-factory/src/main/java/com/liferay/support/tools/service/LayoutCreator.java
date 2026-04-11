package com.liferay.support.tools.service;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.LayoutFriendlyURLException;
import com.liferay.portal.kernel.exception.LayoutNameException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Layout;
import com.liferay.portal.kernel.model.LayoutConstants;
import com.liferay.portal.kernel.service.LayoutLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.LocaleUtil;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = LayoutCreator.class)
public class LayoutCreator {

	public JSONObject create(
			long userId, BatchSpec batchSpec, long groupId, String type,
			boolean privateLayout, boolean hidden)
		throws Exception {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray layouts = JSONFactoryUtil.createJSONArray();
		int skipped = 0;

		for (int i = 0; i < count; i++) {
			final String name = BatchNaming.resolve(baseName, count, i, " ");

			try {
				Layout layout = _invokeAddLayout(
					userId, groupId, privateLayout, name, type, hidden);

				JSONObject layoutJson = JSONFactoryUtil.createJSONObject();

				layoutJson.put("friendlyURL", layout.getFriendlyURL());
				layoutJson.put("layoutId", layout.getLayoutId());
				layoutJson.put(
					"name", layout.getName(LocaleUtil.getSiteDefault()));
				layoutJson.put("plid", layout.getPlid());

				layouts.put(layoutJson);
			}
			catch (LayoutFriendlyURLException | LayoutNameException e) {
				_log.warn(
					"Layout '" + name + "' could not be created: " +
						e.getMessage(),
					e);

				skipped++;
			}
		}

		int createdCount = layouts.length();

		result.put("count", createdCount);
		result.put("layouts", layouts);
		result.put("skipped", skipped);
		result.put("success", createdCount > 0);

		if (createdCount == 0) {
			result.put(
				"error",
				"No layouts were created (all names may be invalid or " +
					"already exist)");
		}

		if (skipped > 0) {
			result.put(
				"message",
				skipped + " layout(s) were skipped");
		}

		return result;
	}

	private Layout _invokeAddLayout(
			long userId, long groupId, boolean privateLayout, String name,
			String type, boolean hidden)
		throws Exception {

		try {
			return TransactionInvokerUtil.invoke(
				_transactionConfig,
				() -> _layoutLocalService.addLayout(
					StringPool.BLANK, userId, groupId, privateLayout,
					LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, name,
					StringPool.BLANK, StringPool.BLANK, type, hidden,
					StringPool.BLANK, new ServiceContext()));
		}
		catch (Exception e) {
			throw e;
		}
		catch (Throwable t) {
			throw new Exception(t);
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(LayoutCreator.class);

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private LayoutLocalService _layoutLocalService;

}
