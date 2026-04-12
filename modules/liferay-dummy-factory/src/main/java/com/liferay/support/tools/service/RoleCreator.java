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
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = RoleCreator.class)
public class RoleCreator {

	public JSONObject create(
			long userId, BatchSpec batchSpec,
			RoleType roleType, String description,
			ProgressCallback progress)
		throws Throwable {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		int type = roleType.toLiferayConstant();

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();
		int skipped = 0;

		Map<Locale, String> descriptionMap = Collections.singletonMap(
			LocaleUtil.getDefault(), description);

		for (int i = 0; i < count; i++) {
			final String name = BatchNaming.resolve(baseName, count, i);

			final Map<Locale, String> titleMap = Collections.singletonMap(
				LocaleUtil.getDefault(), name);

			try {
				Role role = BatchTransaction.run(
					() -> _roleLocalService.addRole(
						StringPool.BLANK, userId, null, 0, name, titleMap,
						descriptionMap, type, null, null));

				JSONObject roleJson = JSONFactoryUtil.createJSONObject();

				roleJson.put("name", role.getName());
				roleJson.put("roleId", role.getRoleId());
				roleJson.put("type", role.getType());

				created.put(roleJson);
			}
			catch (DuplicateRoleException e) {
				_log.warn(
					"Role '" + name + "' already exists, skipping");

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
					"No roles were created (all names may already exist)";
			}
			else if (skipped > 0) {
				errorMessage =
					"Only " + createdCount + " of " + count +
						" roles were created; " + skipped +
							" skipped because the name already existed.";
			}
			else {
				errorMessage =
					"Only " + createdCount + " of " + count +
						" roles were created.";
			}

			result.put("error", errorMessage);
		}

		return result;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		RoleCreator.class);

	@Reference
	private RoleLocalService _roleLocalService;

}
