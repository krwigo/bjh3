package com.darksidebio.bjh3;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {
	public Database(Context context) {
		super(context, "feeditems", null, 25); /* VER */
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("Z", "UPDATING DATABASE");
		
		db.execSQL("DROP TABLE IF EXISTS feeditems");
		db.execSQL("CREATE TABLE IF NOT EXISTS feeditems (id INTEGER PRIMARY KEY AUTOINCREMENT, epoch INTEGER default 0, feed TEXT, title TEXT, url TEXT, UNIQUE(feed,url))");
		
		db.execSQL("DROP TABLE IF EXISTS feedtimes");
		db.execSQL("CREATE TABLE IF NOT EXISTS feedtimes (id INTEGER PRIMARY KEY AUTOINCREMENT, epoch INTEGER default 0, feed TEXT, UNIQUE(feed))");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		this.onCreate(db);
	}
}
