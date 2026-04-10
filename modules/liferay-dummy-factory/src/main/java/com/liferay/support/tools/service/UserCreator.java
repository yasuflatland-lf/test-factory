package com.liferay.support.tools.service;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.User;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.service.UserLocalService;
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
			long[] roleIds, long[] userGroupIds)
		throws Exception {

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();

		for (int i = 0; i < count; i++) {
			String screenName = baseName.toLowerCase() + (i + 1);
			String emailAddress = screenName + "@" + emailDomain;

			User user = _userLocalService.addUser(
				creatorUserId, companyId, false, password, password,
				false, screenName, emailAddress,
				LocaleUtil.getDefault(), baseName, "",
				String.valueOf(i + 1), 0L, 0L, male,
				Calendar.JANUARY, 1, 1970, jobTitle, 0, null,
				organizationIds, roleIds, userGroupIds, false,
				new ServiceContext());

			JSONObject userJson = JSONFactoryUtil.createJSONObject();

			userJson.put("emailAddress", user.getEmailAddress());
			userJson.put("screenName", user.getScreenName());
			userJson.put("userId", user.getUserId());

			created.put(userJson);
		}

		result.put("count", created.length());
		result.put("success", true);
		result.put("users", created);

		return result;
	}

	@Reference
	private UserLocalService _userLocalService;

}
