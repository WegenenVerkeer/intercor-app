package macq.intercor.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import macq.intercor.models.Icon;

public class AdminSQLiteOpenHelper extends SQLiteOpenHelper {

    // Tag which will appear in logcat
    private static final String TAG = "AdminSQLiteOpenHelper";
    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    // Constructor
    public AdminSQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table icons(id int primary key, detail text, message text, position int, type text, urlIcon text)");
        Log.i(TAG, "SQLite DB created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}

    public Task<ArrayList<Icon>> getIconsFromDB() {
        return Tasks.call(mExecutor, () -> {
            ArrayList<Icon> icons = new ArrayList<>();
            SQLiteDatabase db = this.getWritableDatabase();
            Cursor cursorIconsInDB = db.rawQuery("SELECT * FROM icons ORDER BY position", null);
            if (cursorIconsInDB.moveToFirst()) {
                do {
                    Icon icon = new Icon();
                    icon.setId(cursorIconsInDB.getString(cursorIconsInDB.getColumnIndex("id")));
                    icon.setDetail(cursorIconsInDB.getString(cursorIconsInDB.getColumnIndex("detail")));
                    icon.setMessage(cursorIconsInDB.getString(cursorIconsInDB.getColumnIndex("message")));
                    icon.setPosition(cursorIconsInDB.getInt(cursorIconsInDB.getColumnIndex("position")));
                    icon.setType(cursorIconsInDB.getString(cursorIconsInDB.getColumnIndex("type")));
                    icon.setUrlIcon(cursorIconsInDB.getString(cursorIconsInDB.getColumnIndex("urlIcon")));
                    icons.add(icon);
                } while (cursorIconsInDB.moveToNext());
            }
            cursorIconsInDB.close();
            db.close();
            return icons;
        });
    }

    public Task<Void> setIconsInDB(ArrayList<Icon> icons) {
        return Tasks.call(mExecutor, () -> {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("delete from icons");
            for(Icon icon: icons) {
                ContentValues registry = new ContentValues();
                registry.put("id", icon.getId());
                registry.put("detail", icon.getDetail());
                registry.put("message", icon.getMessage());
                registry.put("position", icon.getPosition());
                registry.put("type", icon.getType());
                registry.put("urlIcon", icon.getUrlIcon());
                db.insert("icons", null, registry);
            }
            db.close();
            return null;
        });
    }
}
