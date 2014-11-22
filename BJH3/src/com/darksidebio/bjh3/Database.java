package com.darksidebio.bjh3;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {
	public Database(Context context) {
		super(context, "feeditems", null, 32); /* VER */
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("Z", "UPDATING DATABASE");

		db.execSQL("DROP TABLE IF EXISTS feeditems");
		db.execSQL("CREATE TABLE IF NOT EXISTS feeditems (id INTEGER PRIMARY KEY AUTOINCREMENT, epoch INTEGER default 0, feed TEXT, title TEXT, url TEXT, guid TEXT, active INTEGER default 0, UNIQUE(feed,url))");

		db.execSQL("DROP TABLE IF EXISTS feedtimes");
		db.execSQL("CREATE TABLE IF NOT EXISTS feedtimes (id INTEGER PRIMARY KEY AUTOINCREMENT, epoch INTEGER default 0, feed TEXT, UNIQUE(feed))");

		try {
			ContentValues v = new ContentValues();
			v.put("feed", "BJH3");
			v.put("title", "First Run - Check Connection Settings!");
			v.put("url", "http://wiki.darksidebio.com/index.php/BJH3_(Android)");
			v.put("guid", "http://wiki.darksidebio.com/index.php/BJH3_(Android)");
			v.put("epoch", 0);
			v.put("active", 1);

			db.insertOrThrow("feeditems", null, v);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		this.onCreate(db);
	}
}
