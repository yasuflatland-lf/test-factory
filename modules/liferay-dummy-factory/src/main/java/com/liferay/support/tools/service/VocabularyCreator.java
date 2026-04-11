package com.liferay.support.tools.service;

import com.liferay.asset.kernel.model.AssetVocabulary;
import com.liferay.asset.kernel.service.AssetVocabularyLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = VocabularyCreator.class)
public class VocabularyCreator {

	public List<AssetVocabulary> create(
			long userId, long groupId, BatchSpec batchSpec)
		throws PortalException {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);

		List<AssetVocabulary> vocabularies = new ArrayList<>(count);

		for (int i = 0; i < count; i++) {
			String name = BatchNaming.resolve(baseName, count, i, " ");

			AssetVocabulary vocabulary =
				_assetVocabularyLocalService.addVocabulary(
					userId, groupId, name, serviceContext);

			vocabularies.add(vocabulary);
		}

		return vocabularies;
	}

	@Reference
	private AssetVocabularyLocalService _assetVocabularyLocalService;

}
