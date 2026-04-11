package com.liferay.support.tools.service;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.DuplicateGroupException;
import com.liferay.portal.kernel.exception.GroupKeyException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.GroupConstants;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.sites.kernel.util.Sites;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SiteCreator.class)
public class SiteCreator {

	public JSONObject create(
			long userId, long companyId, BatchSpec batchSpec,
			SiteMembershipType membershipType, long parentGroupId,
			long siteTemplateId, boolean manualMembership,
			boolean inheritContent, boolean active, String description)
		throws Throwable {

		return TransactionInvokerUtil.invoke(
			_transactionConfig, () -> {
				int count = batchSpec.count();
				String baseName = batchSpec.baseName();

				JSONObject result = JSONFactoryUtil.createJSONObject();
				JSONArray created = JSONFactoryUtil.createJSONArray();

				int type = membershipType.toLiferayConstant();

				Map<Locale, String> descriptionMap =
					Collections.singletonMap(
						LocaleUtil.getDefault(), description);

				ServiceContext serviceContext = new ServiceContext();

				serviceContext.setCompanyId(companyId);
				serviceContext.setUserId(userId);

				for (int i = 0; i < count; i++) {
					String siteName = BatchNaming.resolve(
						baseName, count, i);

					Map<Locale, String> nameMap = Collections.singletonMap(
						LocaleUtil.getDefault(), siteName);

					Group group;

					try {
						group = _groupLocalService.addGroup(
							userId, parentGroupId, null, 0,
							GroupConstants.DEFAULT_LIVE_GROUP_ID, nameMap,
							descriptionMap, type, manualMembership,
							GroupConstants.DEFAULT_MEMBERSHIP_RESTRICTION,
							StringPool.BLANK, true, inheritContent, active,
							serviceContext);
					}
					catch (DuplicateGroupException e) {
						_log.warn(
							"Site '" + siteName +
								"' already exists, skipping");

						continue;
					}
					catch (GroupKeyException e) {
						_log.warn(
							"Invalid site name '" + siteName +
								"', skipping");

						continue;
					}

					if (siteTemplateId > 0) {
						try {
							_sites.updateLayoutSetPrototypesLinks(
								group, siteTemplateId, 0, true, false);
						}
						catch (Exception e) {
							_log.error(
								"Failed to apply site template " +
									siteTemplateId + " to site '" +
									siteName + "'",
								e);
						}
					}

					JSONObject siteJson =
						JSONFactoryUtil.createJSONObject();

					siteJson.put("groupId", group.getGroupId());
					siteJson.put("name", siteName);

					created.put(siteJson);
				}

				result.put("count", created.length());
				result.put("sites", created);
				result.put("success", created.length() > 0);

				if (created.length() == 0) {
					result.put(
						"error",
						"No sites were created (all names may already " +
							"exist)");
				}

				return result;
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SiteCreator.class);

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private Sites _sites;

}
