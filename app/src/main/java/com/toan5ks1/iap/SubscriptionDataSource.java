package com.toan5ks1.iap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * DAO class for sample purchase data
 * 
 * 
 */
public class SubscriptionDataSource {

    private static final String TAG = "SampleIAPManager";

    private SQLiteDatabase database;
    private final SampleSQLiteHelper dbHelper;

    private final String[] allColumns = { SampleSQLiteHelper.COLUMN_RECEIPT_ID, SampleSQLiteHelper.COLUMN_USER_ID,
            SampleSQLiteHelper.COLUMN_DATE_FROM, SampleSQLiteHelper.COLUMN_DATE_TO, SampleSQLiteHelper.COLUMN_SKU };

    public SubscriptionDataSource(final Context context) {
        dbHelper = new SampleSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    private SubscriptionRecord cursorToSubscriptionRecord(final Cursor cursor) {
        final SubscriptionRecord subsRecord = new SubscriptionRecord();
        subsRecord.setAmazonReceiptId(cursor.getString(cursor.getColumnIndexOrThrow(SampleSQLiteHelper.COLUMN_RECEIPT_ID)));
        subsRecord.setAmazonUserId(cursor.getString(cursor.getColumnIndexOrThrow(SampleSQLiteHelper.COLUMN_USER_ID)));
        subsRecord.setFrom(cursor.getLong(cursor.getColumnIndexOrThrow(SampleSQLiteHelper.COLUMN_DATE_FROM)));
        subsRecord.setTo(cursor.getLong(cursor.getColumnIndexOrThrow(SampleSQLiteHelper.COLUMN_DATE_TO)));
        subsRecord.setSku(cursor.getString(cursor.getColumnIndexOrThrow(SampleSQLiteHelper.COLUMN_SKU)));
        return subsRecord;
    }

    /**
     * Return all subscription records for the user
     * 
     * @param userId
     *            user id used to verify the purchase record
     * @return list of subscription records
     */
    public final List<SubscriptionRecord> getSubscriptionRecords(final String userId) {
        Log.d(TAG, "getSubscriptionRecord: userId (" + userId + ")");

        final String where = SampleSQLiteHelper.COLUMN_USER_ID + " = ?";
        final Cursor cursor = database.query(SampleSQLiteHelper.TABLE_SUBSCRIPTIONS,
                                             allColumns,
                                             where,
                                             new String[] { userId },
                                             null,
                                             null,
                                             null);
        cursor.moveToFirst();
        final List<SubscriptionRecord> results = new ArrayList<>();
        while (!cursor.isAfterLast()) {
            final SubscriptionRecord subsRecord = cursorToSubscriptionRecord(cursor);
            results.add(subsRecord);
            cursor.moveToNext();
        }
        Log.d(TAG, "getSubscriptionRecord: found " + results.size() + " records");
        cursor.close();
        return results;

    }

    /**
     * Insert or update the subscription record by receiptId
     * 
     * @param receiptId
     *            The receipt id
     * @param userId
     *            Amazon user id
     * @param dateFrom
     *            Timestamp for subscription's valid from date
     * @param dateTo
     *            Timestamp for subscription's valid to date. less than 1 means
     *            cancel date not set, the subscription in active status.
     * @param sku
     *            The sku
     */
    public void insertOrUpdateSubscriptionRecord(final String receiptId,
            final String userId,
            final long dateFrom,
            final long dateTo,
            final String sku) {
        Log.d(TAG, "insertOrUpdateSubscriptionRecord: receiptId (" + receiptId + "),userId (" + userId + ")");
        final String where = SampleSQLiteHelper.COLUMN_RECEIPT_ID + " = ? and "
                             + SampleSQLiteHelper.COLUMN_DATE_TO
                             + " > 0";

        final Cursor cursor = database.query(SampleSQLiteHelper.TABLE_SUBSCRIPTIONS,
                                             allColumns,
                                             where,
                                             new String[] { receiptId },
                                             null,
                                             null,
                                             null);
        final int count = cursor.getCount();
        cursor.close();
        if (count > 0) {
            // There are record with given receipt id and cancel_date>0 in the
            // table, this record should be final and cannot be overwritten
            // anymore.
            Log.w(TAG, "Record already in final state");
        } else {
            // Insert the record into database with CONFLICT_REPLACE flag.
            final ContentValues values = new ContentValues();
            values.put(SampleSQLiteHelper.COLUMN_RECEIPT_ID, receiptId);
            values.put(SampleSQLiteHelper.COLUMN_USER_ID, userId);
            values.put(SampleSQLiteHelper.COLUMN_DATE_FROM, dateFrom);
            values.put(SampleSQLiteHelper.COLUMN_DATE_TO, dateTo);
            values.put(SampleSQLiteHelper.COLUMN_SKU, sku);
            database.insertWithOnConflict(SampleSQLiteHelper.TABLE_SUBSCRIPTIONS,
                                          null,
                                          values,
                                          SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    /**
     * Cancel a subscription by set the cancel date for the subscription record
     * 
     * @param receiptId
     *            The receipt id
     * @param cancelDate
     *            Timestamp for the cancel date
     * @return true for successful update, false otherwise
     */
    public boolean cancelSubscription(final String receiptId, final long cancelDate) {
        Log.d(TAG, "cancelSubscription: receiptId (" + receiptId + "), cancelDate:(" + cancelDate + ")");

        final String where = SampleSQLiteHelper.COLUMN_RECEIPT_ID + " = ?";
        final ContentValues values = new ContentValues();
        values.put(SampleSQLiteHelper.COLUMN_DATE_TO, cancelDate);
        final int updated = database.update(SampleSQLiteHelper.TABLE_SUBSCRIPTIONS,
                                            values,
                                            where,
                                            new String[] { receiptId });
        Log.d(TAG, "cancelSubscription: updated " + updated);
        return updated > 0;

    }
}
