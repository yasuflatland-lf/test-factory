package com.liferay.support.tools.service;

import com.liferay.portal.kernel.model.Company;
import com.liferay.portal.kernel.service.CompanyLocalService;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CompanyCreator.class)
public class CompanyCreator {

	public List<Company> create(
			int count, String webId, String virtualHostname, String mx,
			int maxUsers, boolean active)
		throws Throwable {

		List<Company> companies = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			String prefix = (count > 1) ? String.valueOf(i + 1) : "";

			companies.add(
				TransactionInvokerUtil.invoke(
					_transactionConfig,
					() -> _companyLocalService.addCompany(
						null, prefix + webId, prefix + virtualHostname,
						prefix + mx, maxUsers, active, false, null, null, null,
						null, null, null)));
		}

		return companies;
	}

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private CompanyLocalService _companyLocalService;

}
