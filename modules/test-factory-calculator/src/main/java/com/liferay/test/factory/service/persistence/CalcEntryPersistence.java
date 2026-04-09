/**
 * SPDX-FileCopyrightText: (c) 2026 Liferay, Inc. https://liferay.com
 * SPDX-License-Identifier: LGPL-2.1-or-later OR LicenseRef-Liferay-DXP-EULA-2.0.0-2023-06
 */

package com.liferay.test.factory.service.persistence;

import com.liferay.portal.kernel.service.persistence.BasePersistence;
import com.liferay.test.factory.exception.NoSuchCalcEntryException;
import com.liferay.test.factory.model.CalcEntry;

import org.osgi.annotation.versioning.ProviderType;

/**
 * The persistence interface for the calc entry service.
 *
 * <p>
 * Caching information and settings can be found in <code>portal.properties</code>
 * </p>
 *
 * @author Brian Wing Shun Chan
 * @see CalcEntryUtil
 * @generated
 */
@ProviderType
public interface CalcEntryPersistence extends BasePersistence<CalcEntry> {

	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this interface directly. Always use {@link CalcEntryUtil} to access the calc entry persistence. Modify <code>service.xml</code> and rerun ServiceBuilder to regenerate this interface.
	 */

	/**
	 * Returns all the calc entries where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @return the matching calc entries
	 */
	public java.util.List<CalcEntry> findByUserId(long userId);

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
	public java.util.List<CalcEntry> findByUserId(
		long userId, int start, int end);

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
	public java.util.List<CalcEntry> findByUserId(
		long userId, int start, int end,
		com.liferay.portal.kernel.util.OrderByComparator<CalcEntry>
			orderByComparator);

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
	public java.util.List<CalcEntry> findByUserId(
		long userId, int start, int end,
		com.liferay.portal.kernel.util.OrderByComparator<CalcEntry>
			orderByComparator,
		boolean useFinderCache);

	/**
	 * Returns the first calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching calc entry
	 * @throws NoSuchCalcEntryException if a matching calc entry could not be found
	 */
	public CalcEntry findByUserId_First(
			long userId,
			com.liferay.portal.kernel.util.OrderByComparator<CalcEntry>
				orderByComparator)
		throws NoSuchCalcEntryException;

	/**
	 * Returns the first calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the first matching calc entry, or <code>null</code> if a matching calc entry could not be found
	 */
	public CalcEntry fetchByUserId_First(
		long userId,
		com.liferay.portal.kernel.util.OrderByComparator<CalcEntry>
			orderByComparator);

	/**
	 * Returns the last calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the last matching calc entry
	 * @throws NoSuchCalcEntryException if a matching calc entry could not be found
	 */
	public CalcEntry findByUserId_Last(
			long userId,
			com.liferay.portal.kernel.util.OrderByComparator<CalcEntry>
				orderByComparator)
		throws NoSuchCalcEntryException;

	/**
	 * Returns the last calc entry in the ordered set where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the last matching calc entry, or <code>null</code> if a matching calc entry could not be found
	 */
	public CalcEntry fetchByUserId_Last(
		long userId,
		com.liferay.portal.kernel.util.OrderByComparator<CalcEntry>
			orderByComparator);

	/**
	 * Returns the calc entries before and after the current calc entry in the ordered set where userId = &#63;.
	 *
	 * @param calcEntryId the primary key of the current calc entry
	 * @param userId the user ID
	 * @param orderByComparator the comparator to order the set by (optionally <code>null</code>)
	 * @return the previous, current, and next calc entry
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	public CalcEntry[] findByUserId_PrevAndNext(
			long calcEntryId, long userId,
			com.liferay.portal.kernel.util.OrderByComparator<CalcEntry>
				orderByComparator)
		throws NoSuchCalcEntryException;

	/**
	 * Removes all the calc entries where userId = &#63; from the database.
	 *
	 * @param userId the user ID
	 */
	public void removeByUserId(long userId);

	/**
	 * Returns the number of calc entries where userId = &#63;.
	 *
	 * @param userId the user ID
	 * @return the number of matching calc entries
	 */
	public int countByUserId(long userId);

	/**
	 * Caches the calc entry in the entity cache if it is enabled.
	 *
	 * @param calcEntry the calc entry
	 */
	public void cacheResult(CalcEntry calcEntry);

	/**
	 * Caches the calc entries in the entity cache if it is enabled.
	 *
	 * @param calcEntries the calc entries
	 */
	public void cacheResult(java.util.List<CalcEntry> calcEntries);

	/**
	 * Creates a new calc entry with the primary key. Does not add the calc entry to the database.
	 *
	 * @param calcEntryId the primary key for the new calc entry
	 * @return the new calc entry
	 */
	public CalcEntry create(long calcEntryId);

	/**
	 * Removes the calc entry with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry that was removed
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	public CalcEntry remove(long calcEntryId) throws NoSuchCalcEntryException;

	public CalcEntry updateImpl(CalcEntry calcEntry);

	/**
	 * Returns the calc entry with the primary key or throws a <code>NoSuchCalcEntryException</code> if it could not be found.
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry
	 * @throws NoSuchCalcEntryException if a calc entry with the primary key could not be found
	 */
	public CalcEntry findByPrimaryKey(long calcEntryId)
		throws NoSuchCalcEntryException;

	/**
	 * Returns the calc entry with the primary key or returns <code>null</code> if it could not be found.
	 *
	 * @param calcEntryId the primary key of the calc entry
	 * @return the calc entry, or <code>null</code> if a calc entry with the primary key could not be found
	 */
	public CalcEntry fetchByPrimaryKey(long calcEntryId);

	/**
	 * Returns all the calc entries.
	 *
	 * @return the calc entries
	 */
	public java.util.List<CalcEntry> findAll();

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
	public java.util.List<CalcEntry> findAll(int start, int end);

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
	public java.util.List<CalcEntry> findAll(
		int start, int end,
		com.liferay.portal.kernel.util.OrderByComparator<CalcEntry>
			orderByComparator);

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
	public java.util.List<CalcEntry> findAll(
		int start, int end,
		com.liferay.portal.kernel.util.OrderByComparator<CalcEntry>
			orderByComparator,
		boolean useFinderCache);

	/**
	 * Removes all the calc entries from the database.
	 */
	public void removeAll();

	/**
	 * Returns the number of calc entries.
	 *
	 * @return the number of calc entries
	 */
	public int countAll();

}