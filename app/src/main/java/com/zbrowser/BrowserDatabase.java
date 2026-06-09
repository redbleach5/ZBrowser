package com.zbrowser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BrowserDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME = "zbrowser.db";
    private static final int DB_VERSION = 1;

    // Max history items to keep (prevent unlimited DB growth)
    private static final int MAX_HISTORY_ITEMS = 2000;

    // Bookmarks table
    private static final String TABLE_BOOKMARKS = "bookmarks";
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_URL = "url";
    private static final String COL_FAVICON = "favicon_url";
    private static final String COL_CREATED = "created_at";

    // History table
    private static final String TABLE_HISTORY = "history";
    private static final String COL_H_TITLE = "title";
    private static final String COL_H_URL = "url";
    private static final String COL_H_VISITED = "visited_at";

    public BrowserDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createBookmarks = "CREATE TABLE " + TABLE_BOOKMARKS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TITLE + " TEXT, " +
                COL_URL + " TEXT UNIQUE, " +
                COL_FAVICON + " TEXT, " +
                COL_CREATED + " INTEGER DEFAULT (strftime('%s','now')))";
        db.execSQL(createBookmarks);

        String createHistory = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_H_TITLE + " TEXT, " +
                COL_H_URL + " TEXT, " +
                COL_H_VISITED + " INTEGER DEFAULT (strftime('%s','now')))";
        db.execSQL(createHistory);

        db.execSQL("CREATE INDEX idx_history_visited ON " + TABLE_HISTORY + "(" + COL_H_VISITED + ")");
        db.execSQL("CREATE INDEX idx_bookmarks_url ON " + TABLE_BOOKMARKS + "(" + COL_URL + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    // ============ Bookmarks ============

    public long addBookmark(String title, String url) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, title);
        values.put(COL_URL, url);
        return db.insertWithOnConflict(TABLE_BOOKMARKS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public boolean isBookmarked(String url) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BOOKMARKS, new String[]{COL_ID},
                    COL_URL + " = ?", new String[]{url}, null, null, null);
            return cursor.getCount() > 0;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public void removeBookmark(String url) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_BOOKMARKS, COL_URL + " = ?", new String[]{url});
    }

    public List<JSONObject> getAllBookmarks() {
        List<JSONObject> bookmarks = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_BOOKMARKS, null, null, null, null, null,
                    COL_CREATED + " DESC");

            if (cursor.moveToFirst()) {
                do {
                    try {
                        JSONObject bookmark = new JSONObject();
                        bookmark.put("id", cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
                        bookmark.put("title", cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
                        bookmark.put("url", cursor.getString(cursor.getColumnIndexOrThrow(COL_URL)));
                        bookmarks.add(bookmark);
                    } catch (Exception e) {
                        // Skip malformed entry
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return bookmarks;
    }

    public void clearBookmarks() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_BOOKMARKS, null, null);
    }

    // ============ History ============

    public long addHistory(String title, String url) {
        SQLiteDatabase db = getWritableDatabase();

        // Don't save empty or data URLs
        if (url == null || url.isEmpty() || url.startsWith("data:") || url.startsWith("blob:")) {
            return -1;
        }

        // Don't save very long URLs (likely tracking params)
        if (url.length() > 2048) {
            url = url.substring(0, 2048);
        }

        ContentValues values = new ContentValues();
        values.put(COL_H_TITLE, title != null ? title : "");
        values.put(COL_H_URL, url);
        values.put(COL_H_VISITED, System.currentTimeMillis() / 1000);
        long result = db.insert(TABLE_HISTORY, null, values);

        // Prune old history if exceeding limit
        pruneHistory();

        return result;
    }

    /**
     * Remove oldest history entries when exceeding MAX_HISTORY_ITEMS
     */
    private void pruneHistory() {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.execSQL("DELETE FROM " + TABLE_HISTORY + " WHERE " + COL_ID + " IN " +
                    "(SELECT " + COL_ID + " FROM " + TABLE_HISTORY + " ORDER BY " + COL_H_VISITED + " DESC " +
                    "LIMIT -1 OFFSET " + MAX_HISTORY_ITEMS + ")");
        } catch (Exception e) {
            // Non-critical, ignore
        }
    }

    public List<JSONObject> getHistory(int limit) {
        List<JSONObject> history = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_HISTORY, null, null, null, null, null,
                    COL_H_VISITED + " DESC", String.valueOf(limit));

            if (cursor.moveToFirst()) {
                do {
                    try {
                        JSONObject item = new JSONObject();
                        item.put("id", cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
                        item.put("title", cursor.getString(cursor.getColumnIndexOrThrow(COL_H_TITLE)));
                        item.put("url", cursor.getString(cursor.getColumnIndexOrThrow(COL_H_URL)));
                        item.put("visited_at", cursor.getLong(cursor.getColumnIndexOrThrow(COL_H_VISITED)));
                        history.add(item);
                    } catch (Exception e) {
                        // Skip malformed entry
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return history;
    }

    public void deleteHistoryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_HISTORY, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void clearHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
    }

    public void clearAllData() {
        clearBookmarks();
        clearHistory();
    }
}
