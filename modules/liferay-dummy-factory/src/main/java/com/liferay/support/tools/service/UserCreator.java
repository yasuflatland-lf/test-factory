package com.liferay.support.tools.service;

import com.liferay.portal.kernel.exception.UserScreenNameException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserGroupRoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.support.tools.utils.CommonUtil;

import java.util.Calendar;

import net.datafaker.Faker;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = UserCreator.class)
public class UserCreator {

	public JSONObject create(
			long creatorUserId, long companyId, BatchSpec batchSpec,
			String emailDomain, String password,
			boolean male, String jobTitle, long[] organizationIds,
			long[] roleIds, long[] userGroupIds,
			long[] siteRoleIds, long[] orgRoleIds, boolean fakerEnable,
			String locale, boolean generatePersonalSiteLayouts,
			long publicLayoutSetPrototypeId, long privateLayoutSetPrototypeId,
			long[] groupIds)
		throws Throwable {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		final Faker faker = fakerEnable ?
			_commonUtil.createFaker(locale) : null;

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();

		for (int i = 0; i < count; i++) {
			final int idx = i;

			final String firstName;
			final String lastName;
			final String screenName;

			if (fakerEnable) {
				firstName = faker.name().firstName();
				lastName = faker.name().lastName();
				screenName =
					(firstName + "." + lastName + (idx + 1)).toLowerCase();
			}
			else {
				firstName = baseName;
				lastName = String.valueOf(idx + 1);
				screenName = baseName.toLowerCase() + (idx + 1);
			}

			final String emailAddress = screenName + "@" + emailDomain;

			final ServiceContext serviceContext = new ServiceContext();

			serviceContext.setCompanyId(companyId);
			serviceContext.setUserId(creatorUserId);

			User user;

			try {
				user = TransactionInvokerUtil.invoke(
					_transactionConfig,
					() -> {
						User u = _userLocalService.addUserWithWorkflow(
							creatorUserId, companyId, false, password,
							password, false, screenName, emailAddress,
							LocaleUtil.getDefault(), firstName, "",
							lastName, 0L, 0L, male,
							Calendar.JANUARY, 1, 1970, jobTitle, 0,
							new long[0], organizationIds, roleIds,
							userGroupIds, false, serviceContext);

						if ((organizationIds.length > 0) &&
							((siteRoleIds.length > 0) ||
								(orgRoleIds.length > 0))) {

							for (long orgId : organizationIds) {
								Organization org =
									_organizationLocalService.
										getOrganization(orgId);

								Group group = org.getGroup();
								long groupId = org.getGroupId();

								if ((siteRoleIds.length > 0) &&
									(group != null) && group.isSite() &&
									(groupId > 0)) {

									_userGroupRoleLocalService.
										addUserGroupRoles(
											u.getUserId(), groupId,
											siteRoleIds);
								}

								if ((orgRoleIds.length > 0) &&
									(groupId > 0)) {

									_userGroupRoleLocalService.
										addUserGroupRoles(
											u.getUserId(), groupId,
											orgRoleIds);
								}
							}
						}

						if ((groupIds != null) && (groupIds.length > 0)) {
							for (long groupId : groupIds) {
								if (groupId > 0) {
									_groupLocalService.addUserGroup(
										u.getUserId(), groupId);
								}
							}
						}

						if (generatePersonalSiteLayouts) {
							_userLayoutInitializer.init(u);
							_layoutSetPrototypeLinker.linkUserPersonalSite(
								u, publicLayoutSetPrototypeId,
								privateLayoutSetPrototypeId);
						}

						return u;
					});
			}
			catch (UserScreenNameException.MustNotBeDuplicate e) {
				_log.warn(
					"User '" + screenName + "' already exists, skipping",
					e);

				continue;
			}
			catch (UserScreenNameException e) {
				throw new Exception(
					"Invalid screen name '" + screenName + "' (" +
						e.getClass().getSimpleName() + "): " +
							e.getMessage(),
					e);
			}
			catch (Exception e) {
				throw new Exception(
					"Failed to create user '" + screenName + "' (" +
						(idx + 1) + " of " + count + "): " +
							e.getMessage(),
					e);
			}

			JSONObject userJson = JSONFactoryUtil.createJSONObject();

			userJson.put("emailAddress", user.getEmailAddress());
			userJson.put("screenName", user.getScreenName());
			userJson.put("userId", user.getUserId());

			created.put(userJson);
		}

		result.put("count", created.length());
		result.put("success", created.length() > 0);
		result.put("users", created);

		if (created.length() == 0) {
			result.put(
				"error",
				"No users were created (all screen names may already " +
					"exist)");
		}

		return result;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		UserCreator.class);

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private CommonUtil _commonUtil;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private LayoutSetPrototypeLinker _layoutSetPrototypeLinker;

	@Reference
	private OrganizationLocalService _organizationLocalService;

	@Reference
	private UserGroupRoleLocalService _userGroupRoleLocalService;

	@Reference
	private UserLayoutInitializer _userLayoutInitializer;

	@Reference
	private UserLocalService _userLocalService;

}
