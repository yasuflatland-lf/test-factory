package com.liferay.support.tools.service;

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.service.OrganizationLocalService;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = OrganizationCreator.class)
public class OrganizationCreator {

	public JSONObject create(
			long userId, int count, String baseName,
			long parentOrganizationId, boolean site)
		throws Exception {

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();

		for (int i = 0; i < count; i++) {
			String name = (count == 1) ? baseName : baseName + " " + (i + 1);

			Organization organization =
				_organizationLocalService.addOrganization(
					userId, parentOrganizationId, name, site);

			JSONObject orgJson = JSONFactoryUtil.createJSONObject();

			orgJson.put("name", organization.getName());
			orgJson.put(
				"organizationId", organization.getOrganizationId());

			created.put(orgJson);
		}

		result.put("count", created.length());
		result.put("organizations", created);
		result.put("success", true);

		return result;
	}

	@Reference
	private OrganizationLocalService _organizationLocalService;

}
