package com.liferay.support.tools.service;

import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.support.tools.utils.BatchTransaction;

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
				BatchTransaction.run(
					() -> _assetVocabularyLocalService.addVocabulary(
						userId, groupId, name, serviceContext)));
		}

		return vocabularies;
	}

	@Reference
	private AssetVocabularyLocalService _assetVocabularyLocalService;

}
