/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.test.factory.service.persistence;

import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.service.ServiceContext;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.test.factory.model.CalcEntry;

import java.io.Serializable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The persistence utility for the calc entry service. This utility wraps <code>com.liferay.test.factory.service.persistence.impl.CalcEntryPersistenceImpl</code> and provides direct access to the database for CRUD operations. This utility should only be used by the service layer, as it must operate within a transaction. Never access this utility in a JSP, controller, model, or other front-end class.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see CalcEntryPersistence
 * @generated
 */
public class CalcEntryUtil {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify this class directly. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this class.
	 */

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#clearCache()
	 */
	public static void clearCache() {
		getPersistence().clearCache();
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#clearCache(com.liferay.portal.kernel.model.BaseModel)
	 */
	public static void clearCache(CalcEntry calcEntry) {
		getPersistence().clearCache(calcEntry);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#countWithDynamicQuery(DynamicQuery)
	 */
	public static long countWithDynamicQuery(DynamicQuery dynamicQuery) {
		return getPersistence().countWithDynamicQuery(dynamicQuery);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#fetchByPrimaryKeys(Set)
	 */
	public static Map<Serializable, CalcEntry> fetchByPrimaryKeys(
		Set<Serializable> primaryKeys) {

		return getPersistence().fetchByPrimaryKeys(primaryKeys);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#findWithDynamicQuery(DynamicQuery)
	 */
	public static List<CalcEntry> findWithDynamicQuery(
		DynamicQuery dynamicQuery) {

		return getPersistence().findWithDynamicQuery(dynamicQuery);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#findWithDynamicQuery(DynamicQuery, int, int)
	 */
	public static List<CalcEntry> findWithDynamicQuery(
		DynamicQuery dynamicQuery, int start, int end) {

		return getPersistence().findWithDynamicQuery(dynamicQuery, start, end);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#findWithDynamicQuery(DynamicQuery, int, int, OrderByComparator)
	 */
	public static List<CalcEntry> findWithDynamicQuery(
		DynamicQuery dynamicQuery, int start, int end,
		OrderByComparator<CalcEntry> orderByComparator) {

		return getPersistence().findWithDynamicQuery(
			dynamicQuery, start, end, orderByComparator);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#update(com.liferay.portal.kernel.model.BaseModel)
	 */
	public static CalcEntry update(CalcEntry calcEntry) {
		return getPersistence().update(calcEntry);
	}

	/**
	 * @see com.liferay.portal.kernel.service.persistence.BasePersistence#update(com.liferay.portal.kernel.model.BaseModel, ServiceContext)
	 */
	public static CalcEntry update(
		CalcEntry calcEntry, ServiceContext serviceContext) {

		return getPersistence().update(calcEntry, serviceContext);
	}

	/**
	 * Returns all the calc entries where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @return the matching calc entries
	 */
	public static List<CalcEntry> findByUserId(long userId) {
		return getPersistence().findByUserId(userId);
	}

	/**
	 * Returns a range of all the calc entries where userId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param userId the user ID
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @return the range of matching calc entries
	 */
	public static List<CalcEntry> findByUserId(
		long userId, int start, int end) {

		return getPersistence().findByUserId(userId, start, end);
	}

	/**
	 * Returns an ordered range of all the calc entries where userId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param userId the user ID
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching calc entries
	 */
	public static List<CalcEntry> findByUserId(
		long userId, int start, int end,
		OrderByComparator<CalcEntry> orderByComparator) {

		return getPersistence().findByUserId(
			userId, start, end, orderByComparator);
	}

	/**
	 * Returns an ordered range of all the calc entries where userId = &#63;.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param userId the user ID
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @param useFinderCache whether to use the finder cache
	 * @return the ordered range of matching calc entries
	 */
	public static List<CalcEntry> findByUserId(
		long userId, int start, int end,
		OrderByComparator<CalcEntry> orderByComparator,
		boolean useFinderCache) {

		return getPersistence().findByUserId(
			userId, start, end, orderByComparator, useFinderCache);
	}

	/**
	 * Returns the first calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching calc entry
	 * @throws NoSuchCalcEntryException if a matching calc entry could not be found
	 */
	public static CalcEntry findByUserId_First(
			long userId, OrderByComparator<CalcEntry> orderByComparator)
		throws com.liferay.test.factory.exception.NoSuchCalcEntryException {

		return getPersistence().findByUserId_First(userId, orderByComparator);
	}

	/**
	 * Returns the first calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching calc entry, or <code>null</code> if a matching calc entry could not be found
	 */
	public static CalcEntry fetchByUserId_First(
		long userId, OrderByComparator<CalcEntry> orderByComparator) {

		return getPersistence().fetchByUserId_First(userId, orderByComparator);
	}

	/**
	 * Returns the last calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the last matching calc entry
	 * @throws NoSuchCalcEntryException if a matching calc entry could not be found
	 */
	public static CalcEntry findByUserId_Last(
			long userId, OrderByComparator<CalcEntry> orderByComparator)
		throws com.liferay.test.factory.exception.NoSuchCalcEntryException {

		return getPersistence().findByUserId_Last(userId, orderByComparator);
	}

	/**
	 * Returns the last calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the last matching calc entry, or <code>null</code> if a matching calc entry could not be found
	 */
	public static CalcEntry fetchByUserId_Last(
		long userId, OrderByComparator<CalcEntry> orderByComparator) {

		return getPersistence().fetchByUserId_Last(userId, orderByComparator);
	}

	/**
	 * Returns the calc entries before and after the current calc entry in the ordered set where userId = &#63;.
	 *
	 * @param calcEntryId the primary key of the current calc entry
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the previous, current, and next calc entry
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	public static CalcEntry[] findByUserId_PrevAndNext(
			long calcEntryId, long userId,
			OrderByComparator<CalcEntry> orderByComparator)
		throws com.liferay.test.factory.exception.NoSuchCalcEntryException {

		return getPersistence().findByUserId_PrevAndNext(
			calcEntryId, userId, orderByComparator);
	}

	/**
	 * Removes all the calc entries where userId = &#63; from the database.
	 *
	 * @param userId the user ID
	 */
	public static void removeByUserId(long userId) {
		getPersistence().removeByUserId(userId);
	}

	/**
	 * Returns the number of calc entries where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @return the number of matching calc entries
	 */
	public static int countByUserId(long userId) {
		return getPersistence().countByUserId(userId);
	}

	/**
	 * Caches the calc entry in the entity cache if it is enabled.
	 *
	 * @param calcEntry the calc entry
	 */
	public static void cacheResult(CalcEntry calcEntry) {
		getPersistence().cacheResult(calcEntry);
	}

	/**
	 * Caches the calc entries in the entity cache if it is enabled.
	 *
	 * @param calcEntries the calc entries
	 */
	public static void cacheResult(List<CalcEntry> calcEntries) {
		getPersistence().cacheResult(calcEntries);
	}

	/**
	 * Creates a new calc entry with the primary key. Does not add the calc entry to the database.
	 *
	 * @param calcEntryId the primary key for the new calc entry
	 * @return the new calc entry
	 */
	public static CalcEntry create(long calcEntryId) {
		return getPersistence().create(calcEntryId);
	}

	/**
	 * Removes the calc entry with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry that was removed
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	public static CalcEntry remove(long calcEntryId)
		throws com.liferay.test.factory.exception.NoSuchCalcEntryException {

		return getPersistence().remove(calcEntryId);
	}

	public static CalcEntry updateImpl(CalcEntry calcEntry) {
		return getPersistence().updateImpl(calcEntry);
	}

	/**
	 * Returns the calc entry with the primary key or throws a <code>NoSuchCalcEntryException</code> if it could not be found.
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	public static CalcEntry findByPrimaryKey(long calcEntryId)
		throws com.liferay.test.factory.exception.NoSuchCalcEntryException {

		return getPersistence().findByPrimaryKey(calcEntryId);
	}

	/**
	 * Returns the calc entry with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry, or <code>null</code> if a calc entry with the primary key could not be found
	 */
	public static CalcEntry fetchByPrimaryKey(long calcEntryId) {
		return getPersistence().fetchByPrimaryKey(calcEntryId);
	}

	/**
	 * Returns all the calc entries.
	 *
	 * @return the calc entries
	 */
	public static List<CalcEntry> findAll() {
		return getPersistence().findAll();
	}

	/**
	 * Returns a range of all the calc entries.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @return the range of calc entries
	 */
	public static List<CalcEntry> findAll(int start, int end) {
		return getPersistence().findAll(start, end);
	}

	/**
	 * Returns an ordered range of all the calc entries.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of calc entries
	 */
	public static List<CalcEntry> findAll(
		int start, int end, OrderByComparator<CalcEntry> orderByComparator) {

		return getPersistence().findAll(start, end, orderByComparator);
	}

	/**
	 * Returns an ordered range of all the calc entries.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to <code>QueryUtil#ALL_POS</code> will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent, then the query will include the default ORDER BY logic from <code>CalcEntryModelImpl</code>.
	 * </p>
	 *
	 * @param start the lower bound of the range of calc entries
	 * @param end the upper bound of the range of calc entries (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @param useFinderCache whether to use the finder cache
	 * @return the ordered range of calc entries
	 */
	public static List<CalcEntry> findAll(
		int start, int end, OrderByComparator<CalcEntry> orderByComparator,
		boolean useFinderCache) {

		return getPersistence().findAll(
			start, end, orderByComparator, useFinderCache);
	}

	/**
	 * Removes all the calc entries from the database.
	 */
	public static void removeAll() {
		getPersistence().removeAll();
	}

	/**
	 * Returns the number of calc entries.
	 *
	 * @return the number of calc entries
	 */
	public static int countAll() {
		return getPersistence().countAll();
	}

	public static CalcEntryPersistence getPersistence() {
		return _persistence;
	}

	public static void setPersistence(CalcEntryPersistence persistence) {
		_persistence = persistence;
	}

	private static volatile CalcEntryPersistence _persistence;

}