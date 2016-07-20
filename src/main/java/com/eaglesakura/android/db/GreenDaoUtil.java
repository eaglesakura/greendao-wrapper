package com.eaglesakura.android.db;

import org.greenrobot.greendao.query.CloseableListIterator;

import java.util.Iterator;

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
