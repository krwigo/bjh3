package com.darksidebio.bjh3;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {
	public Database(Context context) {
		super(context, "feeditems", null, 19); /* VER */
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("Z", "UPDATING DATABASE");
		db.execSQL("DROP TABLE IF EXISTS feeditems");
		db.execSQL("CREATE TABLE IF NOT EXISTS feeditems (id INTEGER PRIMARY KEY AUTOINCREMENT, epoch INTEGER default 0 NOT NULL, viewed INTEGER default 0 NOT NULL, feed TEXT, title TEXT, url TEXT, UNIQUE (feed,url))");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		this.onCreate(db);
	}

	public void insert(ContentValues v) {
		// ContentValues values = new ContentValues();
		// values.put("itemtitle", i.itemtitle);
		// values.put("itemurl", i.itemurl);
		// values.put("feedtitle", i.feedtitle);

		SQLiteDatabase db = this.getWritableDatabase();
		//logcat warning:
		//db.insert("feeditems", null, v);
		//ignore:
		db.insertWithOnConflict("feeditems", null, v, 0);
		db.close();
	}
}

/*
 * public List<String> getAll() { List<String> items = new
 * LinkedList<FeedItem>(); SQLiteDatabase db = this.getWritableDatabase();
 * Cursor cursor = db.rawQuery(
 * "SELECT id, isread, feedid, feedtitle, itemtitle, itemurl FROM feeditems"
 * ,null); if (cursor.moveToFirst()) { do { item.id =
 * Integer.parseInt(cursor.getString(0)); item.isread =
 * Integer.parseInt(cursor.getString(1)); item.itemtitle = cursor.getString(4);
 * item.itemurl = cursor.getString(5); items.add(item); } while
 * (cursor.moveToNext()); } return items; }
 */
