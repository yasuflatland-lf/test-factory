package com.liferay.support.tools.service;

import com.liferay.blogs.model.BlogsEntry;
import com.liferay.blogs.service.BlogsEntryLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.support.tools.utils.BatchTransaction;

import java.util.Date;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = BlogsCreator.class)
public class BlogsCreator {

	public JSONObject create(long userId, BlogsBatchSpec spec)
		throws Throwable {

		BatchSpec batch = spec.batch();
		int count = batch.count();
		String baseName = batch.baseName();

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(spec.groupId());
		serviceContext.setUserId(userId);

		Date displayDate = new Date();

		for (int i = 0; i < count; i++) {
			final String title = BatchNaming.resolve(
				baseName, count, i, " ");

			BlogsEntry entry = BatchTransaction.run(
				() -> _blogsEntryLocalService.addEntry(
					userId, title, spec.subtitle(), spec.description(),
					spec.content(), displayDate, spec.allowPingbacks(),
					spec.allowTrackbacks(), spec.trackbackURLs(),
					StringPool.BLANK, null, null, serviceContext));

			JSONObject entryJson = JSONFactoryUtil.createJSONObject();

			entryJson.put("entryId", entry.getEntryId());
			entryJson.put("title", entry.getTitle());

			created.put(entryJson);
		}

		int createdCount = created.length();
		boolean success = (createdCount == count);

		result.put("count", createdCount);
		result.put("items", created);
		result.put("requested", count);
		result.put("skipped", 0);
		result.put("success", success);

		if (!success) {
			result.put(
				"error",
				"Only " + createdCount + " of " + count +
					" blog entries were created.");
		}

		return result;
	}

	@Reference
	private BlogsEntryLocalService _blogsEntryLocalService;

}
