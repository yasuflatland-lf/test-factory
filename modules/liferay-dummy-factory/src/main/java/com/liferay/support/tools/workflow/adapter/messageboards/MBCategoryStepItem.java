package com.liferay.support.tools.workflow.adapter.messageboards;

import com.liferay.message.boards.model.MBCategory;

public record MBCategoryStepItem(
	long categoryId, long groupId, String name) {

	public static MBCategoryStepItem fromMBCategory(MBCategory category) {
		return new MBCategoryStepItem(
			category.getCategoryId(), category.getGroupId(),
			category.getName());
	}

}
