package com.eaglesakura.android.db;

import java.util.Iterator;

import de.greenrobot.dao.query.CloseableListIterator;

public class GreenDaoUtil {
    public static <T> void close(Iterator<T> itr) {
        if (itr instanceof CloseableListIterator) {
            try {
                ((CloseableListIterator) itr).close();
            } catch (Exception e) {
            }
        }
    }
}
