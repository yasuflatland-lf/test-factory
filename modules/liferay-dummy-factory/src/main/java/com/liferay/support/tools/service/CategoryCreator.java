package com.liferay.support.tools.service;

import com.liferay.asset.kernel.model.AssetCategory;
import com.liferay.asset.kernel.service.AssetCategoryLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.service.ServiceContext;
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
		throws PortalException {

		int count = batchSpec.count();
		String baseName = batchSpec.baseName();

		List<AssetCategory> categories = new ArrayList<>(count);

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(groupId);

		for (int i = 0; i < count; i++) {
			String name = BatchNaming.resolve(baseName, count, i, " ");

			Map<Locale, String> titleMap = Collections.singletonMap(
				LocaleUtil.getDefault(), name);

			AssetCategory category = _assetCategoryLocalService.addCategory(
				null, userId, groupId, 0L, titleMap, Collections.emptyMap(),
				vocabularyId, new String[0], serviceContext);

			categories.add(category);
		}

		return categories;
	}

	@Reference
	private AssetCategoryLocalService _assetCategoryLocalService;

}
