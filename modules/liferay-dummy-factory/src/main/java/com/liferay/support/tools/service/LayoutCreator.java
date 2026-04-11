package com.liferay.support.tools.service;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
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
		throws Throwable {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray layouts = JSONFactoryUtil.createJSONArray();

		for (int i = 0; i < count; i++) {
			final String name = BatchNaming.resolve(baseName, count, i, " ");

			Layout layout = TransactionInvokerUtil.invoke(
				_transactionConfig,
				() -> _layoutLocalService.addLayout(
					StringPool.BLANK, userId, groupId, privateLayout,
					LayoutConstants.DEFAULT_PARENT_LAYOUT_ID, name,
					StringPool.BLANK, StringPool.BLANK, type, hidden,
					StringPool.BLANK, new ServiceContext()));

			JSONObject layoutJson = JSONFactoryUtil.createJSONObject();

			layoutJson.put("friendlyURL", layout.getFriendlyURL());
			layoutJson.put("layoutId", layout.getLayoutId());
			layoutJson.put(
				"name", layout.getName(LocaleUtil.getSiteDefault()));
			layoutJson.put("plid", layout.getPlid());

			layouts.put(layoutJson);
		}

		result.put("count", count);
		result.put("layouts", layouts);
		result.put("success", true);

		return result;
	}

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private LayoutLocalService _layoutLocalService;

}
