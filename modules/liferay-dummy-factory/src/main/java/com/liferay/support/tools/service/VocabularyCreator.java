package com.liferay.support.tools.service;

import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.transaction.Propagation;
import com.liferay.portal.kernel.transaction.TransactionConfig;
import com.liferay.portal.kernel.transaction.TransactionInvokerUtil;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = VocabularyCreator.class)
public class VocabularyCreator {

	public List<AssetVocabulary> create(
			long userId, long groupId, BatchSpec batchSpec)
		throws Throwable {

		int count = batchSpec.count();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);

		List<AssetVocabulary> vocabularies = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			String name = BatchNaming.resolve(
				batchSpec.baseName(), count, i, " ");

			vocabularies.add(
				TransactionInvokerUtil.invoke(
					_transactionConfig,
					() -> _assetVocabularyLocalService.addVocabulary(
						userId, groupId, name, serviceContext)));
		}

		return vocabularies;
	}

	private static final TransactionConfig _transactionConfig =
		TransactionConfig.Factory.create(
			Propagation.REQUIRED, new Class<?>[] {Exception.class});

	@Reference
	private AssetVocabularyLocalService _assetVocabularyLocalService;

}
