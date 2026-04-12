package com.liferay.support.tools.service;

import com.liferay.blogs.model.BlogsEntry;
import com.liferay.blogs.service.BlogsEntryLocalService;
import com.liferay.petra.string.StringPool;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

import java.util.Date;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = BlogsCreator.class)
public class BlogsCreator {

	public JSONObject create(
			long userId, BlogsBatchSpec spec, ProgressCallback progress)
		throws Throwable {

		BatchSpec batch = spec.batch();
		int count = batch.count();

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();
		int skipped = 0;

		ServiceContext serviceContext = new ServiceContext();

		serviceContext.setScopeGroupId(spec.groupId());
		serviceContext.setUserId(userId);

		Date displayDate = new Date();

		for (int i = 0; i < count; i++) {
			final String title = BatchNaming.resolve(
				batch.baseName(), count, i, " ");

			try {
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
			catch (Exception e) {
				_log.warn(
					"Blog entry '" + title +
						"' could not be created, skipping: " +
							e.getMessage());

				skipped++;
			}

			progress.onProgress(i + 1, count);
		}

		int createdCount = created.length();
		boolean success = (createdCount == count);

		result.put("count", createdCount);
		result.put("items", created);
		result.put("requested", count);
		result.put("skipped", skipped);
		result.put("success", success);

		if (!success) {
			String errorMessage;

			if (createdCount == 0) {
				errorMessage =
					"No blog entries were created (all attempts failed)";
			}
			else {
				errorMessage =
					"Only " + createdCount + " of " + count +
						" blog entries were created; " + skipped +
							" skipped due to errors.";
			}

			result.put("error", errorMessage);
		}

		return result;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		BlogsCreator.class);

	@Reference
	private BlogsEntryLocalService _blogsEntryLocalService;

}
