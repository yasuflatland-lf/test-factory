package com.liferay.support.tools.workflow.adapter.taxonomy;

import com.liferay.asset.kernel.model.AssetVocabulary;

public record VocabularyStepItem(
	long vocabularyId, long groupId, String name) {

	public static VocabularyStepItem fromAssetVocabulary(
		AssetVocabulary vocabulary) {

		return new VocabularyStepItem(
			vocabulary.getVocabularyId(), vocabulary.getGroupId(),
			vocabulary.getName());
	}

}
