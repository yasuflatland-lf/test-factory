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
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.sites.kernel.util.Sites;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

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
			boolean inheritContent, boolean active, String description,
			long publicLayoutSetPrototypeId, long privateLayoutSetPrototypeId,
			ProgressCallback progress)
		throws Throwable {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();
		int skipped = 0;

		final int type = membershipType.toLiferayConstant();

		final Map<Locale, String> descriptionMap = Collections.singletonMap(
			LocaleUtil.getDefault(), description);

		final ServiceContext serviceContext = new ServiceContext();

		serviceContext.setCompanyId(companyId);
		serviceContext.setUserId(userId);

		for (int i = 0; i < count; i++) {
			final String siteName = BatchNaming.resolve(baseName, count, i);

			final Map<Locale, String> nameMap = Collections.singletonMap(
				LocaleUtil.getDefault(), siteName);

			try {
				Group group = BatchTransaction.run(
					() -> {
						Group newGroup = _groupLocalService.addGroup(
							userId, parentGroupId, null, 0,
							GroupConstants.DEFAULT_LIVE_GROUP_ID, nameMap,
							descriptionMap, type, manualMembership,
							GroupConstants.DEFAULT_MEMBERSHIP_RESTRICTION,
							StringPool.BLANK, true, inheritContent, active,
							serviceContext);

						long resolvedPublicLayoutSetPrototypeId =
							(publicLayoutSetPrototypeId > 0) ?
								publicLayoutSetPrototypeId : siteTemplateId;

						boolean publicEnabled =
							resolvedPublicLayoutSetPrototypeId != 0;
						boolean privateEnabled =
							privateLayoutSetPrototypeId != 0;

						if (publicEnabled || privateEnabled) {
							_sites.updateLayoutSetPrototypesLinks(
								newGroup, resolvedPublicLayoutSetPrototypeId,
								privateLayoutSetPrototypeId, publicEnabled,
								privateEnabled);
						}

						return newGroup;
					});

				JSONObject siteJson = JSONFactoryUtil.createJSONObject();

				siteJson.put("groupId", group.getGroupId());
				siteJson.put("name", siteName);

				LayoutSet publicLayoutSet =
					_layoutSetLocalService.fetchLayoutSet(
						group.getGroupId(), false);
				LayoutSet privateLayoutSet =
					_layoutSetLocalService.fetchLayoutSet(
						group.getGroupId(), true);

				if (publicLayoutSet != null) {
					siteJson.put(
						"publicLayoutSetPrototypeUuid",
						publicLayoutSet.getLayoutSetPrototypeUuid());
				}

				if (privateLayoutSet != null) {
					siteJson.put(
						"privateLayoutSetPrototypeUuid",
						privateLayoutSet.getLayoutSetPrototypeUuid());
				}

				siteJson.put(
					"inheritContent", group.isInheritContent());
				siteJson.put("parentGroupId", group.getParentGroupId());

				created.put(siteJson);
			}
			catch (DuplicateGroupException e) {
				_log.warn(
					"Site '" + siteName + "' already exists, skipping");

				skipped++;
			}
			catch (GroupKeyException e) {
				_log.warn(
					"Invalid site name '" + siteName + "', skipping");

				skipped++;
			}

			progress.onProgress(i + 1, count);
		}

		int createdCount = created.length();
		boolean success = (createdCount == count);

		result.put("count", createdCount);
		result.put("items", created);
		result.put("requested", count);
		result.put("skipped", skipped);
		result.put("success", success);

		if (!success) {
			String errorMessage;

			if (createdCount == 0) {
				errorMessage =
					"No sites were created (all names may already exist)";
			}
			else if (skipped > 0) {
				errorMessage =
					"Only " + createdCount + " of " + count +
						" sites were created; " + skipped +
							" skipped because the name already existed.";
			}
			else {
				errorMessage =
					"Only " + createdCount + " of " + count +
						" sites were created.";
			}

			result.put("error", errorMessage);
		}

		return result;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		SiteCreator.class);

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private LayoutSetLocalService _layoutSetLocalService;

	@Reference
	private Sites _sites;

}
