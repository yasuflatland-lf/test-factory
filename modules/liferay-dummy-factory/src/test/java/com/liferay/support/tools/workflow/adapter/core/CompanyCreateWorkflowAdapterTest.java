package com.liferay.support.tools.workflow.adapter.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.liferay.portal.kernel.model.Company;
import com.liferay.support.tools.service.CompanyCreator;
import com.liferay.support.tools.workflow.adapter.TestModelProxyUtil;
import com.liferay.support.tools.workflow.adapter.core.dto.CompanyCreateRequest;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CompanyCreateWorkflowAdapterTest {

	@Test
	void executeSetsSkippedForPartialResults() throws Throwable {
		CompanyCreateWorkflowAdapter adapter =
			new CompanyCreateWorkflowAdapter();

		_setCompanyCreator(
			adapter,
			new CompanyCreator() {
			@Override
			public List<Company> create(
				int count, String webId, String virtualHostname, String mx,
				int maxUsers, boolean active,
				com.liferay.support.tools.utils.ProgressCallback progress) {

				return List.of(_company(101L, "company-1"));
			}
		});

		WorkflowStepResult result = adapter.execute(
			new CompanyCreateRequest(
				true, 2, 100, "mx.example.com", "vhost", "company"),
			null, null);

		assertEquals(2, result.requested());
		assertEquals(1, result.count());
		assertEquals(1, result.skipped());
		assertFalse(result.success());
		assertEquals(
			"Only 1 of 2 companies were created.", result.error());
		assertEquals(
			Map.of("companyId", 101L, "webId", "company-1"),
			result.items().get(0));
	}

	private static Company _company(long companyId, String webId) {
		return TestModelProxyUtil.proxy(
			Company.class,
			Map.of("getCompanyId", companyId, "getWebId", webId));
	}

	private static void _setCompanyCreator(
			CompanyCreateWorkflowAdapter adapter,
			CompanyCreator companyCreator)
		throws Exception {

		Field field = CompanyCreateWorkflowAdapter.class.getDeclaredField(
			"_companyCreator");

		field.setAccessible(true);
		field.set(adapter, companyCreator);
	}

}
