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
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;


public abstract class DaoDatabase<SessionClass extends AbstractDaoSession, Self extends DaoDatabase> implements Closeable {

    private final Context mContext;

    private final Class<? extends AbstractDaoMaster> mDaoMasterClass;

    private AbstractDaoMaster mDaoMaster;

    private SessionClass mSession;

    final private Object refsLock = new Object();

    private final AtomicInteger mRefs = new AtomicInteger(0);

    /**
     * 読み込み専用モードで開く
     */
    public static final int FLAG_READ_ONLY = 0x1 << 0;

    /**
     * 新規に生成する
     */
    public DaoDatabase(Context context, Class<? extends AbstractDaoMaster> daoMasterClass) {
        this.mContext = context.getApplicationContext();
        this.mDaoMasterClass = daoMasterClass;
    }

    /**
     * DaoMasterを指定して生成する
     */
    public DaoDatabase(Context context, AbstractDaoMaster daoMaster) {
        this.mContext = context.getApplicationContext();
        this.mDaoMaster = daoMaster;
        this.mDaoMasterClass = daoMaster.getClass();
    }

    public Context getContext() {
        return mContext;
    }

    protected abstract SQLiteOpenHelper createHelper();

    @SuppressWarnings("unchecked")
    public Self open(int flags) {
        synchronized (refsLock) {
            if (mDaoMaster == null) {
                SQLiteOpenHelper helper = createHelper();
                SQLiteDatabase db = ((flags & FLAG_READ_ONLY) == FLAG_READ_ONLY) ? helper.getReadableDatabase() : helper.getWritableDatabase();

                try {
                    Constructor<? extends AbstractDaoMaster> constructor = mDaoMasterClass.getConstructor(SQLiteDatabase.class);
                    mDaoMaster = constructor.newInstance(db);
                    mSession = (SessionClass) mDaoMaster.newSession();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            mRefs.incrementAndGet();
            // 正常終了した
            return (Self) this;
        }
    }

    /**
     * Sessionを取得する
     * queryを投げるのに使う。
     */
    public SessionClass getSession() {
        return mSession;
    }

    /**
     * 戻り値と例外を許容してトランザクション実行を行う
     */
    public <RetType, ErrType extends Throwable> RetType runInTx(ThrowableRunnable<RetType, ErrType> runnable) throws ErrType {
        ThrowableRunner<RetType, ErrType> runner = new ThrowableRunner<>(runnable);
        mSession.runInTx(runner);
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
            if (mRefs.decrementAndGet() > 0) {
                // まだ開いているセッションがあるため、閉じる必要はない
                return;
            }

            if (mDaoMaster != null) {
                mDaoMaster.getDatabase().close();
                mDaoMaster = null;
            }
        }
    }

    protected Cursor rawQuery(String sql, String[] argments) {
        SQLiteDatabase db = (SQLiteDatabase) getSession().getDatabase().getRawDatabase();
        return db.rawQuery(sql, argments);
    }

    /**
     * クエリを実行し、カーソルを得る
     *
     * Cursorはラップされ、closeされたタイミングでDao側も閉じる
     *
     * @param readOnly read onlyでDBを開く場合true
     * @param sql      実行するSQL文
     * @param args     SQL引数
     * @return カーソル
     */
    public Cursor query(boolean readOnly, String sql, Object... args) {
        open(FLAG_READ_ONLY);

        String[] argments = new String[args.length];
        int index = 0;
        for (Object arg : args) {
            argments[index++] = arg.toString();
        }
        Cursor cursor = rawQuery(sql, argments);
        return new DaoCursor(cursor, this);
    }

    /**
     * データベースを削除する
     */
    public void drop() {
        try {
            Method dropAllTables = mDaoMasterClass.getMethod("dropAllTables", SQLiteDatabase.class, boolean.class);
            dropAllTables.invoke(mDaoMaster, mDaoMaster.getDatabase(), true);

            Method createAllTables = mDaoMasterClass.getMethod("createAllTables", SQLiteDatabase.class, boolean.class);
            createAllTables.invoke(mDaoMaster, mDaoMaster.getDatabase(), true);
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
