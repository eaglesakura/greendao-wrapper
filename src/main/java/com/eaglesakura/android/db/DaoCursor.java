package com.eaglesakura.android.db;

import com.eaglesakura.util.IOUtil;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

/**
 * ラップされたカーソル情報
 */
public class DaoCursor implements Cursor {
    Cursor mCursor;

    DaoDatabase mDatabase;

    public DaoCursor(Cursor cursor, DaoDatabase database) {
        mCursor = cursor;
        mDatabase = database;
    }

    @Override
    public int getCount() {
        return mCursor.getCount();
    }

    @Override
    public int getPosition() {
        return mCursor.getPosition();
    }

    @Override
    public boolean move(int i) {
        return mCursor.move(i);
    }

    @Override
    public boolean moveToPosition(int i) {
        return mCursor.moveToPosition(i);
    }

    @Override
    public boolean moveToFirst() {
        return mCursor.moveToFirst();
    }

    @Override
    public boolean moveToLast() {
        return mCursor.moveToLast();
    }

    @Override
    public boolean moveToNext() {
        return mCursor.moveToNext();
    }

    @Override
    public boolean moveToPrevious() {
        return mCursor.moveToPrevious();
    }

    @Override
    public boolean isFirst() {
        return mCursor.isFirst();
    }

    @Override
    public boolean isLast() {
        return mCursor.isLast();
    }

    @Override
    public boolean isBeforeFirst() {
        return mCursor.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast() {
        return mCursor.isAfterLast();
    }

    @Override
    public int getColumnIndex(String s) {
        return mCursor.getColumnIndex(s);
    }

    @Override
    public int getColumnIndexOrThrow(String s) throws IllegalArgumentException {
        return mCursor.getColumnIndexOrThrow(s);
    }

    @Override
    public String getColumnName(int i) {
        return mCursor.getColumnName(i);
    }

    @Override
    public String[] getColumnNames() {
        return mCursor.getColumnNames();
    }

    @Override
    public int getColumnCount() {
        return mCursor.getColumnCount();
    }

    @Override
    public byte[] getBlob(int i) {
        return mCursor.getBlob(i);
    }

    @Override
    public String getString(int i) {
        return mCursor.getString(i);
    }

    @Override
    public void copyStringToBuffer(int i, CharArrayBuffer charArrayBuffer) {
        mCursor.copyStringToBuffer(i, charArrayBuffer);
    }

    @Override
    public short getShort(int i) {
        return mCursor.getShort(i);
    }

    @Override
    public int getInt(int i) {
        return mCursor.getInt(i);
    }

    @Override
    public long getLong(int i) {
        return mCursor.getLong(i);
    }

    @Override
    public float getFloat(int i) {
        return mCursor.getFloat(i);
    }

    @Override
    public double getDouble(int i) {
        return mCursor.getDouble(i);
    }

    @SuppressLint("NewApi")
    @Override
    public int getType(int i) {
        return mCursor.getType(i);
    }

    @Override
    public boolean isNull(int i) {
        return mCursor.isNull(i);
    }

    @Override
    @Deprecated
    public void deactivate() {
        mCursor.deactivate();
    }

    @Override
    @Deprecated
    public boolean requery() {
        return mCursor.requery();
    }

    public void close() {
        IOUtil.close(mCursor);
        IOUtil.close(mDatabase);

        mCursor = null;
        mDatabase = null;
    }

    @Override
    public boolean isClosed() {
        return mCursor.isClosed();
    }

    @Override
    public void registerContentObserver(ContentObserver contentObserver) {
        mCursor.registerContentObserver(contentObserver);
    }

    @Override
    public void unregisterContentObserver(ContentObserver contentObserver) {
        mCursor.unregisterContentObserver(contentObserver);
    }

    @Override
    public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        mCursor.registerDataSetObserver(dataSetObserver);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
        mCursor.unregisterDataSetObserver(dataSetObserver);
    }

    @Override
    public void setNotificationUri(ContentResolver contentResolver, Uri uri) {
        mCursor.setNotificationUri(contentResolver, uri);
    }

    @SuppressLint("NewApi")
    @Override
    public Uri getNotificationUri() {
        return mCursor.getNotificationUri();
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return mCursor.getWantsAllOnMoveCalls();
    }

    @SuppressLint("NewApi")
    @Override
    public void setExtras(Bundle bundle) {
        mCursor.setExtras(bundle);
    }

    @Override
    public Bundle getExtras() {
        return mCursor.getExtras();
    }

    @Override
    public Bundle respond(Bundle bundle) {
        return mCursor.respond(bundle);
    }
}
