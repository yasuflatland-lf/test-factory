package com.liferay.support.tools.service;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;
import com.liferay.portal.kernel.util.LocaleUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = CategoryCreator.class)
public class CategoryCreator {

	public List<AssetCategory> create(
			long userId, long groupId, long vocabularyId, BatchSpec batchSpec)
		throws Throwable {

		int count = batchSpec.count();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);

		List<AssetCategory> categories = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			String name = BatchNaming.resolve(
				batchSpec.baseName(), count, i, " ");

			Map<Locale, String> titleMap = Collections.singletonMap(
				LocaleUtil.getDefault(), name);

			categories.add(
				TransactionInvokerUtil.invoke(
					_transactionConfig,
					() -> _assetCategoryLocalService.addCategory(
						null, userId, groupId, 0L, titleMap,
						Collections.emptyMap(), vocabularyId, new String[0],
						serviceContext)));
		}

		return categories;
	}

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private AssetCategoryLocalService _assetCategoryLocalService;

}
