package com.liferay.support.tools.service;

import com.liferay.portal.kernel.exception.UserScreenNameException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Group;
import com.liferay.portal.kernel.model.LayoutSet;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.GroupLocalService;
import com.liferay.portal.kernel.service.LayoutSetLocalService;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserGroupRoleLocalService;
import com.liferay.portal.kernel.service.UserLocalService;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.CommonUtil;
import com.liferay.support.tools.utils.ProgressCallback;
import com.liferay.support.tools.utils.ScreenNameSanitizer;

import java.util.Calendar;

import net.datafaker.Faker;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = UserCreator.class)
public class UserCreator {

	public JSONObject create(
			long creatorUserId, long companyId, UserBatchSpec spec,
			ProgressCallback progress)
		throws Throwable {

		BatchSpec batchSpec = spec.batch();
		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		String emailDomain = spec.emailDomain();
		String password = spec.password();
		boolean male = spec.male();
		String jobTitle = spec.jobTitle();
		long[] organizationIds = spec.organizationIds();
		long[] roleIds = spec.roleIds();
		long[] userGroupIds = spec.userGroupIds();
		long[] siteRoleIds = spec.siteRoleIds();
		long[] orgRoleIds = spec.orgRoleIds();
		boolean fakerEnable = spec.fakerEnable();
		String locale = spec.locale();
		boolean generatePersonalSiteLayouts =
			spec.generatePersonalSiteLayouts();
		long publicLayoutSetPrototypeId = spec.publicLayoutSetPrototypeId();
		long privateLayoutSetPrototypeId =
			spec.privateLayoutSetPrototypeId();
		long[] groupIds = spec.groupIds();

		if (!fakerEnable) {
			String normalizedBaseName = baseName.toLowerCase();

			if (!normalizedBaseName.matches("^[a-z0-9._-]+$")) {
				throw new IllegalArgumentException(
					"Invalid baseName '" + baseName +
						"': must contain only lowercase letters, digits, " +
							"'.', '_', or '-'");
			}
		}

		final Faker faker = fakerEnable ?
			_commonUtil.createFaker(locale) : null;

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();
		int skippedDuplicates = 0;

		for (int i = 0; i < count; i++) {
			final int idx = i;

			final String firstName;
			final String lastName;
			final String screenName;

			if (fakerEnable) {
				firstName = faker.name().firstName();
				lastName = faker.name().lastName();
				screenName = ScreenNameSanitizer.sanitize(
					(firstName + "." + lastName + (idx + 1)).toLowerCase());
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
				user = BatchTransaction.run(
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

				skippedDuplicates++;

				progress.onProgress(i + 1, count);

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

			if (generatePersonalSiteLayouts) {
				Group personalSite = user.getGroup();

				if (personalSite != null) {
					long personalGroupId = personalSite.getGroupId();

					userJson.put("groupId", personalGroupId);

					LayoutSet publicLayoutSet =
						_layoutSetLocalService.fetchLayoutSet(
							personalGroupId, false);
					LayoutSet privateLayoutSet =
						_layoutSetLocalService.fetchLayoutSet(
							personalGroupId, true);

					if (publicLayoutSet != null) {
						userJson.put(
							"publicLayoutSetPrototypeUuid",
							publicLayoutSet.getLayoutSetPrototypeUuid());
					}

					if (privateLayoutSet != null) {
						userJson.put(
							"privateLayoutSetPrototypeUuid",
							privateLayoutSet.getLayoutSetPrototypeUuid());
					}
				}
			}

			created.put(userJson);

			progress.onProgress(i + 1, count);
		}

		boolean success = created.length() == count;

		result.put("count", created.length());
		result.put("requested", count);
		result.put("skipped", skippedDuplicates);
		result.put("success", success);
		result.put("items", created);

		if (!success) {
			if (created.length() == 0) {
				result.put(
					"error",
					"No users were created (all screen names may already " +
						"exist)");
			}
			else if (skippedDuplicates > 0) {
				result.put(
					"error",
					"Only " + created.length() + " of " + count +
						" users were created; " + skippedDuplicates +
							" skipped because the screen name already " +
								"existed.");
			}
			else {
				result.put(
					"error",
					"Only " + created.length() + " of " + count +
						" users were created.");
			}
		}

		return result;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		UserCreator.class);

	@Reference
	private CommonUtil _commonUtil;

	@Reference
	private GroupLocalService _groupLocalService;

	@Reference
	private LayoutSetLocalService _layoutSetLocalService;

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
