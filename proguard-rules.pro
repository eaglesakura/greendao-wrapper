## greenDao本体
-keep class org.greenrobot.greendao.** { *; }
-keepnames class org.greenrobot.greendao.** { *; }

## 出力されたDao
-keep class * extends org.greenrobot.greendao.AbstractDaoMaster { *; }
-keep class * extends org.greenrobot.greendao.AbstractDao { *; }
-keep class **.*Dao$Properties { *; }
