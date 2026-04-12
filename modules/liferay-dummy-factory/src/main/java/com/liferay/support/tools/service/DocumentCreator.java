package com.liferay.support.tools.service;

import com.liferay.document.library.kernel.service.DLAppLocalService;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.repository.model.FileEntry;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.TempFileEntryUtil;
import com.liferay.support.tools.portlet.actions.DocumentUploadResourceCommand;
import com.liferay.support.tools.utils.BatchTransaction;
import com.liferay.support.tools.utils.ProgressCallback;

import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = DocumentCreator.class)
public class DocumentCreator {

	public JSONObject create(
			long userId, long groupId, BatchSpec spec, long folderId,
			String description, String[] uploadedFiles,
			ProgressCallback progress)
		throws Throwable {

		int count = spec.count();
		String baseName = spec.baseName();

		JSONObject result = JSONFactoryUtil.createJSONObject();
		JSONArray created = JSONFactoryUtil.createJSONArray();
		int skipped = 0;

		List<FileEntry> tempFileEntries = _loadTempFiles(
			groupId, userId, uploadedFiles);

		try {
			for (int i = 0; i < count; i++) {
				final String title = BatchNaming.resolve(
					baseName, count, i, " ");

				try {
					FileEntry fileEntry;

					if (tempFileEntries.isEmpty()) {
						final String sourceFileName = title + ".txt";
						final byte[] content =
							("Test document: " + title).getBytes(
								StandardCharsets.UTF_8);

						fileEntry = BatchTransaction.run(
							() -> {
								ServiceContext serviceContext =
									new ServiceContext();

								serviceContext.setUserId(userId);

								return _dlAppLocalService.addFileEntry(
									null, userId, groupId, folderId,
									sourceFileName, ContentTypes.TEXT_PLAIN,
									title, "", description, "", content, null,
									null, null, serviceContext);
							});
					}
					else {
						final FileEntry tf = _getRandomFileEntry(
							tempFileEntries);
						final String fileName =
							title + "." + tf.getExtension();

						fileEntry = BatchTransaction.run(
							() -> {
								ServiceContext serviceContext =
									new ServiceContext();

								serviceContext.setUserId(userId);

								return _dlAppLocalService.addFileEntry(
									null, userId, groupId, folderId, fileName,
									tf.getMimeType(), fileName, "", description,
									"", tf.getContentStream(), tf.getSize(),
									null, null, null, serviceContext);
							});
					}

					JSONObject docJson = JSONFactoryUtil.createJSONObject();

					docJson.put("fileEntryId", fileEntry.getFileEntryId());
					docJson.put("title", fileEntry.getTitle());

					created.put(docJson);
				}
				catch (PortalException portalException) {
					_log.warn(
						"Document '" + title + "' could not be created: " +
							portalException.getMessage(),
						portalException);

					skipped++;
				}

				progress.onProgress(i + 1, count);
			}
		}
		finally {
			_cleanupTempFiles(tempFileEntries);
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
					"No documents were created (all titles may already " +
						"exist or upload sizes were invalid)";
			}
			else if (skipped > 0) {
				errorMessage =
					"Only " + createdCount + " of " + count +
						" documents were created; " + skipped +
							" skipped because the title already existed.";
			}
			else {
				errorMessage =
					"Only " + createdCount + " of " + count +
						" documents were created.";
			}

			result.put("error", errorMessage);
		}

		return result;
	}

	private void _cleanupTempFiles(List<FileEntry> entries) {
		for (FileEntry entry : entries) {
			try {
				TempFileEntryUtil.deleteTempFileEntry(entry.getFileEntryId());
			}
			catch (Exception e) {
				_log.warn(
					"Failed to delete temp file entry " +
						entry.getFileEntryId() + ": " + e.getMessage(),
					e);
			}
		}
	}

	private FileEntry _getRandomFileEntry(List<FileEntry> entries) {
		int index = ThreadLocalRandom.current().nextInt(entries.size());

		return entries.get(index);
	}

	private List<FileEntry> _loadTempFiles(
			long groupId, long userId, String[] fileNames) {

		List<FileEntry> entries = new ArrayList<>();

		if ((fileNames == null) || (fileNames.length == 0)) {
			return entries;
		}

		for (String name : fileNames) {
			if ((name == null) || name.isEmpty()) {
				continue;
			}

			try {
				entries.add(
					TempFileEntryUtil.getTempFileEntry(
						groupId, userId,
						DocumentUploadResourceCommand.TEMP_FOLDER_NAME, name));
			}
			catch (Exception e) {
				_log.warn(
					"Temp file '" + name + "' could not be loaded: " +
						e.getMessage(),
					e);
			}
		}

		return entries;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		DocumentCreator.class);

	@Reference
	private DLAppLocalService _dlAppLocalService;

}
