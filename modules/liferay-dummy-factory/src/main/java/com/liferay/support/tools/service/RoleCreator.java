package com.liferay.support.tools.service;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.DuplicateRoleException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.model.role.RoleConstants;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.util.LocaleUtil;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = RoleCreator.class)
public class RoleCreator {

	public JSONObject create(
			long userId, int count, String baseName,
			String roleType, String description)
		throws Exception {

		int type = _getRoleType(roleType);

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();
		int skipped = 0;

		for (int i = 0; i < count; i++) {
			String name = (count == 1) ? baseName : baseName + (i + 1);

			Map<Locale, String> titleMap = new HashMap<>();

			titleMap.put(LocaleUtil.getDefault(), name);

			Map<Locale, String> descriptionMap = new HashMap<>();

			descriptionMap.put(LocaleUtil.getDefault(), description);

			try {
				Role role = _roleLocalService.addRole(
					StringPool.BLANK, userId, null, 0, name,
					titleMap, descriptionMap, type, null, null);

				JSONObject roleJson = JSONFactoryUtil.createJSONObject();

				roleJson.put("name", role.getName());
				roleJson.put("roleId", role.getRoleId());
				roleJson.put("type", role.getType());

				created.put(roleJson);
			}
			catch (DuplicateRoleException e) {
				_log.warn("Role '" + name + "' already exists, skipping");

				skipped++;
			}
		}

		result.put("count", created.length());
		result.put("roles", created);
		result.put("skipped", skipped);
		result.put("success", created.length() > 0);

		if (skipped > 0) {
			result.put(
				"message",
				skipped + " role(s) already existed and were skipped");
		}

		return result;
	}

	private int _getRoleType(String roleType) {
		switch (roleType) {
			case "site":
				return RoleConstants.TYPE_SITE;
			case "organization":
				return RoleConstants.TYPE_ORGANIZATION;
			case "provider":
				return RoleConstants.TYPE_PROVIDER;
			case "depot":
				return RoleConstants.TYPE_DEPOT;
			case "account":
				return RoleConstants.TYPE_ACCOUNT;
			case "publications":
				return RoleConstants.TYPE_PUBLICATIONS;
			default:
				return RoleConstants.TYPE_REGULAR;
		}
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RoleCreator.class);

	@Reference
	private RoleLocalService _roleLocalService;

}
