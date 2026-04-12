package com.liferay.support.tools.workflow.adapter.taxonomy;

import com.liferay.asset.kernel.model.AssetCategory;

public record CategoryStepItem(
	long categoryId, long groupId, long vocabularyId, String name) {

	public static CategoryStepItem fromAssetCategory(AssetCategory category) {
		return new CategoryStepItem(
			category.getCategoryId(), category.getGroupId(),
			category.getVocabularyId(), category.getName());
	}

}
