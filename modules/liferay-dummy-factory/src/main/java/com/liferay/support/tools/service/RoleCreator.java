package com.liferay.support.tools.service;

import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.exception.DuplicateRoleException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Role;
import com.liferay.portal.kernel.service.RoleLocalService;
import com.liferay.portal.kernel.util.LocaleUtil;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = RoleCreator.class)
public class RoleCreator {

	public JSONObject create(
			long userId, BatchSpec batchSpec,
			RoleType roleType, String description)
		throws Exception {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		int type = roleType.toLiferayConstant();

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();
		int skipped = 0;

		Map<Locale, String> descriptionMap = Collections.singletonMap(
			LocaleUtil.getDefault(), description);

		for (int i = 0; i < count; i++) {
			String name = BatchNaming.resolve(baseName, count, i);

			Map<Locale, String> titleMap = Collections.singletonMap(
				LocaleUtil.getDefault(), name);

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

	private static final Log _log = LogFactoryUtil.getLog(
		RoleCreator.class);

	@Reference
	private RoleLocalService _roleLocalService;

}
