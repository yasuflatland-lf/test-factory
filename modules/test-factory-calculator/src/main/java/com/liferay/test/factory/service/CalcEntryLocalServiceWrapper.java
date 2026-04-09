/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.test.factory.service;

import com.liferay.portal.kernel.service.ServiceWrapper;
import com.liferay.portal.kernel.service.persistence.BasePersistence;

/**
 * Provides a wrapper for {@link CalcEntryLocalService}.
 *
 * @author Brian Wing Shun Chan
 * @see CalcEntryLocalService
 * @generated
 */
public class CalcEntryLocalServiceWrapper
	implements CalcEntryLocalService, ServiceWrapper<CalcEntryLocalService> {

	public CalcEntryLocalServiceWrapper() {
		this(null);
	}

	public CalcEntryLocalServiceWrapper(
		CalcEntryLocalService calcEntryLocalService) {

		_calcEntryLocalService = calcEntryLocalService;
	}

	/**
	 * Adds the calc entry to the database. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect CalcEntryLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param calcEntry the calc entry
	 * @return the calc entry that was added
	 */
	@Override
	public com.liferay.test.factory.model.CalcEntry addCalcEntry(
		com.liferay.test.factory.model.CalcEntry calcEntry) {

		return _calcEntryLocalService.addCalcEntry(calcEntry);
	}

	@Override
	public com.liferay.test.factory.model.CalcEntry addCalcEntry(
			long userId, double num1, double num2, String operator,
			com.liferay.portal.kernel.service.ServiceContext serviceContext)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _calcEntryLocalService.addCalcEntry(
			userId, num1, num2, operator, serviceContext);
	}

	/**
	 * Creates a new calc entry with the primary key. Does not add the calc entry to the database.
	 *
	 * @param calcEntryId the primary key for the new calc entry
	 * @return the new calc entry
	 */
	@Override
	public com.liferay.test.factory.model.CalcEntry createCalcEntry(
		long calcEntryId) {

		return _calcEntryLocalService.createCalcEntry(calcEntryId);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel createPersistedModel(
			java.io.Serializable primaryKeyObj)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _calcEntryLocalService.createPersistedModel(primaryKeyObj);
	}

	/**
	 * Deletes the calc entry from the database. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect CalcEntryLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param calcEntry the calc entry
	 * @return the calc entry that was removed
	 */
	@Override
	public com.liferay.test.factory.model.CalcEntry deleteCalcEntry(
		com.liferay.test.factory.model.CalcEntry calcEntry) {

		return _calcEntryLocalService.deleteCalcEntry(calcEntry);
	}

	/**
	 * Deletes the calc entry with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect CalcEntryLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry that was removed
	 * @throws PortalException if a calc entry with the primary key could not be found
	 */
	@Override
	public com.liferay.test.factory.model.CalcEntry deleteCalcEntry(
			long calcEntryId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _calcEntryLocalService.deleteCalcEntry(calcEntryId);
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel deletePersistedModel(
			com.liferay.portal.kernel.model.PersistedModel persistedModel)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _calcEntryLocalService.deletePersistedModel(persistedModel);
	}

	@Override
	public <T> T dslQuery(com.liferay.petra.sql.dsl.query.DSLQuery dslQuery) {
		return _calcEntryLocalService.dslQuery(dslQuery);
	}

	@Override
	public int dslQueryCount(
		com.liferay.petra.sql.dsl.query.DSLQuery dslQuery) {

		return _calcEntryLocalService.dslQueryCount(dslQuery);
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery() {
		return _calcEntryLocalService.dynamicQuery();
	}

	/**
	 * Performs a dynamic query on the database and returns the matching rows.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the matching rows
	 */
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery) {

		return _calcEntryLocalService.dynamicQuery(dynamicQuery);
	}

	/**
	 * Performs a dynamic query on the database and returns a range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.test.factory.model.impl.CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @return the range of matching rows
	 */
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery, int start,
		int end) {

		return _calcEntryLocalService.dynamicQuery(dynamicQuery, start, end);
	}

	/**
	 * Performs a dynamic query on the database and returns an ordered range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.test.factory.model.impl.CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching rows
	 */
	@Override
	public <T> java.util.List<T> dynamicQuery(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery, int start,
		int end,
		com.liferay.portal.kernel.util.OrderByComparator<T> orderByComparator) {

		return _calcEntryLocalService.dynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the number of rows matching the dynamic query
	 */
	@Override
	public long dynamicQueryCount(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery) {

		return _calcEntryLocalService.dynamicQueryCount(dynamicQuery);
	}

	/**
	 * Returns the number of rows matching the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @param projection the projection to apply to the query
	 * @return the number of rows matching the dynamic query
	 */
	@Override
	public long dynamicQueryCount(
		com.liferay.portal.kernel.dao.orm.DynamicQuery dynamicQuery,
		com.liferay.portal.kernel.dao.orm.Projection projection) {

		return _calcEntryLocalService.dynamicQueryCount(
			dynamicQuery, projection);
	}

	@Override
	public com.liferay.test.factory.model.CalcEntry fetchCalcEntry(
		long calcEntryId) {

		return _calcEntryLocalService.fetchCalcEntry(calcEntryId);
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.ActionableDynamicQuery
		getActionableDynamicQuery() {

		return _calcEntryLocalService.getActionableDynamicQuery();
	}

	/**
	 * Returns a range of all the calc entries.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>com.liferay.test.factory.model.impl.CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @return the range of calc entries
	 */
	@Override
	public java.util.List<com.liferay.test.factory.model.CalcEntry>
		getCalcEntries(int start, int end) {

		return _calcEntryLocalService.getCalcEntries(start, end);
	}

	/**
	 * Returns the number of calc entries.
	 *
	 * @return the number of calc entries
	 */
	@Override
	public int getCalcEntriesCount() {
		return _calcEntryLocalService.getCalcEntriesCount();
	}

	/**
	 * Returns the calc entry with the primary key.
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry
	 * @throws PortalException if a calc entry with the primary key could not be found
	 */
	@Override
	public com.liferay.test.factory.model.CalcEntry getCalcEntry(
			long calcEntryId)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _calcEntryLocalService.getCalcEntry(calcEntryId);
	}

	@Override
	public com.liferay.portal.kernel.dao.orm.IndexableActionableDynamicQuery
		getIndexableActionableDynamicQuery() {

		return _calcEntryLocalService.getIndexableActionableDynamicQuery();
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	@Override
	public String getOSGiServiceIdentifier() {
		return _calcEntryLocalService.getOSGiServiceIdentifier();
	}

	/**
	 * @throws PortalException
	 */
	@Override
	public com.liferay.portal.kernel.model.PersistedModel getPersistedModel(
			java.io.Serializable primaryKeyObj)
		throws com.liferay.portal.kernel.exception.PortalException {

		return _calcEntryLocalService.getPersistedModel(primaryKeyObj);
	}

	/**
	 * Updates the calc entry in the database or adds it if it does not yet exist. Also notifies the appropriate model listeners.
	 *
	 * <p>
	 * <strong>Important:</strong> Inspect CalcEntryLocalServiceImpl for overloaded versions of the method. If provided, use these entry points to the API, as the implementation logic may require the additional parameters defined there.
	 * </p>
	 *
	 * @param calcEntry the calc entry
	 * @return the calc entry that was updated
	 */
	@Override
	public com.liferay.test.factory.model.CalcEntry updateCalcEntry(
		com.liferay.test.factory.model.CalcEntry calcEntry) {

		return _calcEntryLocalService.updateCalcEntry(calcEntry);
	}

	@Override
	public BasePersistence<?> getBasePersistence() {
		return _calcEntryLocalService.getBasePersistence();
	}

	@Override
	public CalcEntryLocalService getWrappedService() {
		return _calcEntryLocalService;
	}

	@Override
	public void setWrappedService(CalcEntryLocalService calcEntryLocalService) {
		_calcEntryLocalService = calcEntryLocalService;
	}

	private CalcEntryLocalService _calcEntryLocalService;

}