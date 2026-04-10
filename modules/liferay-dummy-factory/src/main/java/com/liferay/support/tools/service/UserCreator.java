package com.liferay.support.tools.service;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserGroupRoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.exception.UserScreenNameException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.LocaleUtil;

import java.util.Calendar;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = UserCreator.class)
public class UserCreator {

	public JSONObject create(
			long creatorUserId, long companyId, int count,
			String baseName, String emailDomain, String password,
			boolean male, String jobTitle, long[] organizationIds,
			long[] roleIds, long[] userGroupIds,
			long[] siteRoleIds, long[] orgRoleIds)
		throws Exception {

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();

		for (int i = 0; i < count; i++) {
			String screenName = baseName.toLowerCase() + (i + 1);
			String emailAddress = screenName + "@" + emailDomain;

			ServiceContext serviceContext = new ServiceContext();

			serviceContext.setCompanyId(companyId);
			serviceContext.setUserId(creatorUserId);

			User user;

			try {
				user = _userLocalService.addUserWithWorkflow(
					creatorUserId, companyId, false, password, password,
					false, screenName, emailAddress,
					LocaleUtil.getDefault(), baseName, "",
					String.valueOf(i + 1), 0L, 0L, male,
					Calendar.JANUARY, 1, 1970, jobTitle, 0, new long[0],
					organizationIds, roleIds, userGroupIds, false,
					serviceContext);
			}
			catch (UserScreenNameException e) {
				_log.warn(
					"User '" + screenName + "' already exists, skipping");

				continue;
			}
			catch (Exception e) {
				throw new Exception(
					"Failed to create user '" + screenName + "' (" +
						(i + 1) + " of " + count + "): " +
						e.getMessage(),
					e);
			}

			JSONObject userJson = JSONFactoryUtil.createJSONObject();

			userJson.put("emailAddress", user.getEmailAddress());
			userJson.put("screenName", user.getScreenName());
			userJson.put("userId", user.getUserId());

			if ((organizationIds.length > 0) &&
				((siteRoleIds.length > 0) || (orgRoleIds.length > 0))) {

				for (long orgId : organizationIds) {
					try {
						Organization org =
							_organizationLocalService.getOrganization(orgId);

						Group group = org.getGroup();
						long groupId = org.getGroupId();

						if ((siteRoleIds.length > 0) && (group != null) &&
							group.isSite() && (groupId > 0)) {

							_userGroupRoleLocalService.addUserGroupRoles(
								user.getUserId(), groupId, siteRoleIds);
						}

						if ((orgRoleIds.length > 0) && (groupId > 0)) {
							_userGroupRoleLocalService.addUserGroupRoles(
								user.getUserId(), groupId, orgRoleIds);
						}
					}
					catch (Exception e) {
						throw new Exception(
							"Failed to assign roles for user '" +
								screenName + "' in organization " + orgId +
								": " + e.getMessage(),
							e);
					}
				}
			}

			created.put(userJson);
		}

		result.put("count", created.length());
		result.put("success", created.length() > 0);
		result.put("users", created);

		if (created.length() == 0) {
			result.put(
				"error",
				"No users were created (all screen names may already exist)");
		}

		return result;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		UserCreator.class);

	@Reference
	private OrganizationLocalService _organizationLocalService;

	@Reference
	private UserGroupRoleLocalService _userGroupRoleLocalService;

	@Reference
	private UserLocalService _userLocalService;

}
