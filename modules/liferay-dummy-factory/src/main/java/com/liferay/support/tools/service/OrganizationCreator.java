package com.liferay.support.tools.service;

import com.liferay.portal.kernel.exception.DuplicateOrganizationException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.model.Organization;
import com.liferay.portal.kernel.service.OrganizationLocalService;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = OrganizationCreator.class)
public class OrganizationCreator {

	public JSONObject create(
			long userId, BatchSpec batchSpec,
			long parentOrganizationId, boolean site)
		throws Throwable {

		return TransactionInvokerUtil.invoke(
			_transactionConfig, () -> {
				int count = batchSpec.count();
				String baseName = batchSpec.baseName();

				JSONObject result = JSONFactoryUtil.createJSONObject();
				JSONArray created = JSONFactoryUtil.createJSONArray();
				int skipped = 0;

				for (int i = 0; i < count; i++) {
					String name = BatchNaming.resolve(
						baseName, count, i, " ");

					try {
						Organization organization =
							_organizationLocalService.addOrganization(
								userId, parentOrganizationId, name, site);

						JSONObject orgJson =
							JSONFactoryUtil.createJSONObject();

						orgJson.put("name", organization.getName());
						orgJson.put(
							"organizationId",
							organization.getOrganizationId());

						created.put(orgJson);
					}
					catch (DuplicateOrganizationException e) {
						_log.warn(
							"Organization '" + name +
								"' already exists, skipping");

						skipped++;
					}
				}

				int createdCount = created.length();

				result.put("count", createdCount);
				result.put("organizations", created);
				result.put("skipped", skipped);
				result.put("success", createdCount > 0);

				if (createdCount == 0) {
					result.put(
						"error",
						"No organizations were created (all names may " +
							"already exist)");
				}

				if (skipped > 0) {
					result.put(
						"message",
						skipped +
							" organization(s) already existed and were " +
								"skipped");
				}

				return result;
			});
	}

	private static final Log _log = LogFactoryUtil.getLog(
		OrganizationCreator.class);

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private OrganizationLocalService _organizationLocalService;

}
