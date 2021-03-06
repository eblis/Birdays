/*
 * Copyright 2017 Evgeny Timofeev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eblis.whenwasit.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.eblis.whenwasit.models.AnniversaryType;
import com.eblis.whenwasit.models.Person;
import com.eblis.whenwasit.utils.Utils;

import org.joda.time.LocalDate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.eblis.whenwasit.database.DbHelper.COLUMN_CATEGORY;
import static com.eblis.whenwasit.database.DbHelper.DB_PERSONS;

public class DbQueryManager {

    private SQLiteDatabase database;

    DbQueryManager(SQLiteDatabase database) {
        this.database = database;
    }

    public Person getPerson(long timeStamp) {

        final List<Person> candidates = getPersons(DbHelper.DB_PERSONS, null, DbHelper.SELECTION_TIME_STAMP, new String[]{Long.toString(timeStamp)}, null, null, null, null);

        if (candidates.size() > 0) {
            return candidates.get(0);
        }

        return null;
    }

    public List<Person> getPersons() {
        return getPersons(DbHelper.DB_PERSONS, null, null, null, null, null, null, null);
    }

    public List<String> getContactCategories() {
        final Cursor cursor = database.query(true, DB_PERSONS, new String[]{COLUMN_CATEGORY}, null, null, null, null, null, null);
        List<String> categories = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                final String category = cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY));
                categories.add(category);
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        for (Iterator<String> iterator = categories.iterator(); iterator.hasNext();) {
            String category = iterator.next();
            if ((category == null) || (category.isEmpty())) {
                iterator.remove();
            }
        }
        try {
            Collections.sort(categories);
        }
        catch (Exception ex) {
            //ignore
        }
        return categories;
    }

    public interface Matches {
        public boolean match(Person person);
    }

    private List<Person> getPersons(final String db, final String[] columns, final String selection, final String[] selectionArgs, final String groupBy, final String having, final String orderBy, final Matches matcher) {
        final List<Person> persons = new ArrayList<>();

        final Cursor cursor = database.query(db, columns, selection, selectionArgs, groupBy, having, orderBy);

        if (cursor.moveToFirst()) {
            do {
                final Person person = getPerson(cursor);
                if ((matcher == null) || (matcher.match(person))) {
                    persons.add(getPerson(cursor));
                }

            } while (cursor.moveToNext());
        }
        cursor.close();

        return persons;
    }

    private Person getPerson(final Cursor cursor) {
        final Long contactId = getContactId(cursor);
        final String name = getName(cursor);
        final long date = getDate(cursor);
        final boolean isYearKnown = getYearUnknown(cursor);
        final String phoneNumber = getPhoneNumber(cursor);
        final AnniversaryType anniversaryType = getAnniversaryType(cursor);
        final String label = getAnniversaryLabel(cursor);
        final String email = getEmail(cursor);
        final String category = getCategory(cursor);
        final long timeStamp = getTimeStamp(cursor);

        return new Person(contactId, name, date, isYearKnown, phoneNumber, email, label, anniversaryType, category, timeStamp);
    }

    public List<Person> getSearchPerson(String selection, String[] selectionArgs, String orderBy) {
        return getPersons(DbHelper.DB_PERSONS, null, selection, selectionArgs, null, null, orderBy, null);
    }

    public List<Person> getThisMonthPersons() {
        return getSearchMonthPersons(null, null, null);
    }

    public List<Person> getSearchMonthPersons(String selection, String[] selectionArgs, String orderBy) {
        return getPersons(DbHelper.DB_PERSONS, null, selection, selectionArgs, null, null, orderBy, new Matches() {
            @Override
            public boolean match(Person person) {
                return Utils.isCurrentMonth(person.getDate());
            }
        });
    }

    public List<Person> getFamousBornThisDay(LocalDate dayOfBirthday) {
        final List<Person> persons = new ArrayList<>();

        final Cursor cursor = database.query(DbHelper.DB_FAMOUS, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                final String name = getName(cursor);
                final LocalDate date = new LocalDate(getDate(cursor));

                if (date.getMonthOfYear() == dayOfBirthday.getMonthOfYear() && date.getDayOfMonth() == dayOfBirthday.getDayOfMonth()) {
                    final Person person = new Person(name, date);
                    persons.add(person);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        return persons;
    }

    private Long getContactId(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(DbHelper.COLUMN_CONTACT_ID));
    }

    private String getName(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(DbHelper.COLUMN_NAME));
    }

    private long getDate(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(DbHelper.COLUMN_DATE));
    }

    private boolean getYearUnknown(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(DbHelper.COLUMN_IS_YEAR_KNOWN)) == 1;
    }

    private String getPhoneNumber(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(DbHelper.COLUMN_PHONE_NUMBER));
    }

    private String getAnniversaryLabel(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(DbHelper.COLUMN_ANNIVERSARY_LABEL));
    }

    private String getCategory(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(COLUMN_CATEGORY));
    }

    private AnniversaryType getAnniversaryType(Cursor cursor) {
        return AnniversaryType.valueOf(cursor.getString(cursor.getColumnIndex(DbHelper.COLUMN_ANNIVERSARY_TYPE)));
    }

    private String getEmail(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndex(DbHelper.COLUMN_EMAIL));
    }

    private long getTimeStamp(Cursor cursor) {
        return cursor.getLong(cursor.getColumnIndex(DbHelper.COLUMN_TIME_STAMP));
    }
}