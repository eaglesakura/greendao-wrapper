package com.eaglesakura.android.db;

import com.eaglesakura.lambda.Action1Throwable;
import com.eaglesakura.lambda.Action2Throwable;
import com.eaglesakura.util.ThrowableRunnable;
import com.eaglesakura.util.ThrowableRunner;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.AbstractDaoMaster;
import org.greenrobot.greendao.AbstractDaoSession;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.query.CloseableListIterator;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;


public abstract class DaoDatabase<SessionClass extends AbstractDaoSession> implements Closeable {

    protected final Context context;

    protected final Class<? extends AbstractDaoMaster> daoMasterClass;

    protected AbstractDaoMaster daoMaster;

    protected SessionClass session;

    final private Object refsLock = new Object();

    private int refs = 0;

    /**
     * 新規に生成する
     */
    public DaoDatabase(Context context, Class<? extends AbstractDaoMaster> daoMasterClass) {
        this.context = context.getApplicationContext();
        this.daoMasterClass = daoMasterClass;
    }

    /**
     * DaoMasterを指定して生成する
     */
    public DaoDatabase(Context context, AbstractDaoMaster daoMaster) {
        this.context = context.getApplicationContext();
        this.daoMaster = daoMaster;
        this.daoMasterClass = daoMaster.getClass();
    }

    public Context getContext() {
        return context;
    }

    protected abstract SQLiteOpenHelper createHelper();

    @SuppressWarnings("unchecked")
    public void open(boolean readOnly) {
        synchronized (refsLock) {
            if (daoMaster == null) {
                SQLiteOpenHelper helper = createHelper();
                SQLiteDatabase db = readOnly ? helper.getReadableDatabase() : helper.getWritableDatabase();

                try {
                    Constructor<? extends AbstractDaoMaster> constructor = daoMasterClass.getConstructor(SQLiteDatabase.class);
                    daoMaster = constructor.newInstance(db);
                    session = (SessionClass) daoMaster.newSession();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            ++refs;

            // 正常終了した
            return;
        }
    }

    public <T extends DaoDatabase<SessionClass>> T openReadOnly(Class<T> clazz) {
        openReadOnly();
        return (T) this;
    }

    public <T extends DaoDatabase<SessionClass>> T openWritable(Class<T> clazz) {
        openWritable();
        return (T) this;
    }

    public void openReadOnly() {
        open(true);
    }

    public void openWritable() {
        open(false);
    }

    /**
     * Sessionを取得する
     * queryを投げるのに使う。
     */
    public SessionClass getSession() {
        return session;
    }

    /**
     * 戻り値と例外を許容してトランザクション実行を行う
     */
    public <RetType, ErrType extends Throwable> RetType runInTx(ThrowableRunnable<RetType, ErrType> runnable) throws ErrType {
        ThrowableRunner<RetType, ErrType> runner = new ThrowableRunner<>(runnable);
        session.runInTx(runner);
        return runner.getOrThrow();
    }

    /**
     * イテレータで読み込めるオブジェクトに対して処理を行う
     */
    protected <T, R, E extends Throwable> R each(CloseableListIterator<T> iterator, Action2Throwable<T, R, E> action, R result) throws E {
        if (iterator == null) {
            return result;
        }

        try {
            while (iterator.hasNext()) {
                action.action(iterator.next(), result);
            }
        } finally {
            try {
                iterator.close();
            } catch (Exception e) {
            }
        }

        return result;
    }

    /**
     * イテレータで読み込めるオブジェクトに対して処理を行う
     */
    protected <T, E extends Throwable> void each(CloseableListIterator<T> iterator, Action1Throwable<T, E> action) throws E {
        if (iterator == null) {
            return;
        }

        try {
            while (iterator.hasNext()) {
                action.action(iterator.next());
            }
        } finally {
            try {
                iterator.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * 必要に応じてDBを閉じる
     */
    @Override
    public void close() {
        synchronized (refsLock) {
            --refs;
            if (refs > 0) {
                // まだ開いているセッションがあるため、閉じる必要はない
                return;
            }

            if (daoMaster != null) {
                daoMaster.getDatabase().close();
                daoMaster = null;
            }
        }
    }

    /**
     * データベースを削除する
     */
    public void drop() {
        try {
            Method dropAllTables = daoMasterClass.getMethod("dropAllTables", SQLiteDatabase.class, boolean.class);
            dropAllTables.invoke(daoMaster, daoMaster.getDatabase(), true);

            Method createAllTables = daoMasterClass.getMethod("createAllTables", SQLiteDatabase.class, boolean.class);
            createAllTables.invoke(daoMaster, daoMaster.getDatabase(), true);
        } catch (Exception e) {
        }
    }

    /**
     * insertを試行し、失敗したらupdateを行う
     */
    protected <T, K> void insertOrUpdate(T entity, AbstractDao<T, K> session) {
        try {
            session.insertOrReplace(entity);
        } catch (SQLiteConstraintException e) {
            session.update(entity);
        }
    }

    protected static void addIntegerColumn(SQLiteDatabase db, String tableName, Property property) {
        String sql = "ALTER" +
                " TABLE " + tableName + "" +
                " ADD COLUMN '" + property.columnName + "' INTEGER;";
        db.execSQL(sql);
    }

    protected static void addDoubleColumn(SQLiteDatabase db, String tableName, Property property) {
        String sql = "ALTER" +
                " TABLE " + tableName + "" +
                " ADD COLUMN '" + property.columnName + "' REAL;";
        db.execSQL(sql);
    }

    protected static void addStringColumn(SQLiteDatabase db, String tableName, Property property) {
        String sql = "ALTER" +
                " TABLE " + tableName + "" +
                " ADD COLUMN '" + property.columnName + "' TEXT;";
        db.execSQL(sql);
    }

    protected static void addByteArrayColumn(SQLiteDatabase db, String tableName, Property property) {
        String sql = "ALTER" +
                " TABLE " + tableName + "" +
                " ADD COLUMN '" + property.columnName + "' BLOB;";
        db.execSQL(sql);
    }
}
