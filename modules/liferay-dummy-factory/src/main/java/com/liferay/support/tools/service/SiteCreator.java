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
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.sites.kernel.util.Sites;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SiteCreator.class)
public class SiteCreator {

	public JSONObject create(
			long userId, long companyId, int count, String baseName,
			String membershipType, long parentGroupId,
			long siteTemplateId, boolean manualMembership,
			boolean inheritContent, boolean active, String description)
		throws Exception {

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();

		int type = _toGroupType(membershipType);

		for (int i = 0; i < count; i++) {
			String siteName = (count == 1) ?
				baseName : baseName + (i + 1);

			Map<Locale, String> nameMap = new HashMap<>();

			nameMap.put(LocaleUtil.getDefault(), siteName);

			Map<Locale, String> descriptionMap = new HashMap<>();

			descriptionMap.put(LocaleUtil.getDefault(), description);

			ServiceContext serviceContext = new ServiceContext();

			serviceContext.setCompanyId(companyId);
			serviceContext.setUserId(userId);

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
					"Site '" + siteName + "' already exists, skipping");

				continue;
			}
			catch (GroupKeyException e) {
				_log.warn(
					"Invalid site name '" + siteName + "', skipping");

				continue;
			}

			if (siteTemplateId > 0) {
				_sites.updateLayoutSetPrototypesLinks(
					group, siteTemplateId, 0, true, false);
			}

			JSONObject siteJson = JSONFactoryUtil.createJSONObject();

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
				"No sites were created (all names may already exist)");
		}

		return result;
	}

	private int _toGroupType(String membershipType) {
		switch (membershipType) {
			case "restricted":
				return GroupConstants.TYPE_SITE_RESTRICTED;
			case "private":
				return GroupConstants.TYPE_SITE_PRIVATE;
			default:
				return GroupConstants.TYPE_SITE_OPEN;
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SiteCreator.class);

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private Sites _sites;

}
