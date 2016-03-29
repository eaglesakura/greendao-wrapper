package com.eaglesakura.android.db;

import org.junit.Test;

public class GreenDaoUtilTest {
    @Test
    public void closeメソッドにnullを入力してもNPEしない() {
        GreenDaoUtil.close(null);
    }
}
