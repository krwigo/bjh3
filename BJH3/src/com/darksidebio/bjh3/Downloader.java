package com.darksidebio.bjh3;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class Downloader extends IntentService {
	private static boolean firstRun = true;
	
	public Downloader() {
		super(null);
		Log.d("Z", "Downloader() Init");
	}

	void debugPrint(String name, SQLiteDatabase mSQL) {
		Log.d("Z", "[debugPrint] BEGIN; "+name);
		Cursor c = mSQL.rawQuery("SELECT feed,epoch FROM feedtimes", null);
		Log.d("Z", "[debugPrint] count:"+c.getCount());
		c.moveToFirst();
		while (c.isAfterLast() == false) {
			Log.d("Z", "[debugPrint]:  "+c.getString(0)+", "+c.getString(1));
			c.moveToNext();
		}
		c.close();
	}
	
	int getNewItems(String feedtitle, String feedurl, SQLiteDatabase mSQL) {
		int newItemCount = 0;
		Log.d("Z", "getNewItems() "+feedtitle);

		//INSERT FEED
		try {
			ContentValues insertValues = new ContentValues();
			insertValues.put("feed", feedtitle);
			mSQL.insertOrThrow("feedtimes", null, insertValues);
		} catch (SQLiteConstraintException e) {
		}
		
		//GET FEED LAST RUN TIME
		Cursor cSelect = mSQL.rawQuery("SELECT epoch FROM feedtimes WHERE (feed=?) LIMIT 1", new String[]{feedtitle});
		if (cSelect.getCount() != 1) return 0;
		cSelect.moveToFirst();
		long tPast = cSelect.getInt(0);
		long tNow = System.currentTimeMillis()/1000;

		//CHECK
		if (!firstRun && tPast < tNow && tPast+600 > tNow)
			return 0;
		
		Log.d("Z", "Updating "+feedtitle);
		
		//UPDATE LAST RUN TIME
		ContentValues updateValues = new ContentValues();
		updateValues.put("epoch", System.currentTimeMillis()/1000);
		mSQL.update("feedtimes", updateValues, "feed=?", new String[]{feedtitle});

		try {
			URL url = new URL(feedurl);
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			RSSHandler rh = new RSSHandler();
			xr.setContentHandler(rh);
			xr.parse(new InputSource(url.openStream()));

			for (RSSItem item : rh.items) {
				ContentValues v = new ContentValues();
				v.put("feed", feedtitle);
				v.put("title", item.mTitle);
				v.put("url", item.mURL);
				v.put("epoch", item.mDate.getTime()/1000);
				
				try {
					mSQL.insertOrThrow("feeditems", null, v);
					newItemCount += 1;
				} catch (SQLiteConstraintException e) {
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return newItemCount;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		int newItemTotal = 0;

		if (intent == null)
			return;
		
		// if (wifiOnly && isConnected)
		// return;

		Log.d("Z", "Downloader");
		
		Database mDatabase = new Database(getApplicationContext());
		SQLiteDatabase mSQL = mDatabase.getWritableDatabase();
		
		HashMap<String,String> mTabFeeds = new HashMap<String,String>();
		mTabFeeds.put("BJH3",  "http://www.hash.cn/feed/");
		mTabFeeds.put("Boxer", "http://www.hash.cn/category/boxerh3/feed/");
		mTabFeeds.put("FMH",   "http://www.hash.cn/category/fullmoonh3/feed/");
		mTabFeeds.put("Trash", "http://www.hash.cn/category/hashtrash/feed/");
		
		//Download
		for (Entry<String,String> entry : mTabFeeds.entrySet()) {
			newItemTotal += this.getNewItems(entry.getKey(), entry.getValue(), mSQL);
		}
		
		firstRun = false;
		mSQL.close();

		if (newItemTotal > 0) {
			//Update
			Intent b = new Intent();
			b.setAction("SOME_ACTION");
			sendBroadcast(b);
			
			//Notify
			PendingIntent pIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
			NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
			Notification noti = new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.beijing_trans96)
				.setContentIntent(pIntent)
				.setAutoCancel(true)
				.setContentTitle("Beijing Hash House Harriers")
				.setContentText(newItemTotal + (newItemTotal > 1 ? " new posts" : " new post"))
				.build();
			notificationManager.notify(0, noti);
		}
	}

	private class RSSItem {
		String mTitle;
		String mURL;
		Date mDate;

		public RSSItem() {
			mDate = new Date(0);
		}
	}

	private class RSSHandler extends DefaultHandler {
		StringBuilder tmpValue = new StringBuilder();
		boolean isContent = false;
		List<RSSItem> items = new ArrayList<RSSItem>();
		RSSItem curItem = null;

		@Override
		public void startDocument() throws SAXException {
			items.clear();
		}

		@Override
		public void endDocument() throws SAXException {
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equalsIgnoreCase("item"))
				curItem = new RSSItem();
			tmpValue.setLength(0);
			isContent = true;
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (curItem == null) {
				// Log.d("RSSItems", "curItem is null (endElement)");
			} else if (qName.equalsIgnoreCase("item")) {
				items.add(curItem);
			} else if (qName.equalsIgnoreCase("title")) {
				curItem.mTitle = tmpValue.toString();
			} else if (qName.equalsIgnoreCase("link")) {
				curItem.mURL = tmpValue.toString();
			} else if (qName.equalsIgnoreCase("pubdate")) {
				try {
					DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
					curItem.mDate = formatter.parse(tmpValue.toString());
				} catch (ParseException e) {
					curItem.mDate = new Date(System.currentTimeMillis());
					e.printStackTrace();
				}
			}
			tmpValue.setLength(0);
			isContent = false;
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (isContent)
				tmpValue.append(ch, start, length);
		}
	}
}
