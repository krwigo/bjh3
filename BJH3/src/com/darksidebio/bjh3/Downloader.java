package com.darksidebio.bjh3;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class Downloader extends IntentService {
	private static boolean firstRun = true;
	SharedPreferences mSharedPreferences = null;
	Database mDatabase = null;
	SQLiteDatabase mSQL = null;
	
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
	
	int getNewItems(String feedtitle, String feedurl) {
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
		int tRate = getConnectionRate();

		Log.d("Z", "-[tRate]: "+tRate);
		
		//CHECK
		//> if "never" or not connected
		if (tRate <= 0)
			return 0;
		//> if not first run, past isnt in the future, and enough time has passed
		if (!firstRun && tPast < tNow && tPast+tRate > tNow)
			return 0;

		Log.d("Z", "Updating "+feedtitle);
		postStatusChange("STATUS_UPDATING");

		//UPDATE LAST RUN TIME
		ContentValues updateValues = new ContentValues();
		updateValues.put("epoch", System.currentTimeMillis()/1000);
		mSQL.update("feedtimes", updateValues, "feed=?", new String[]{feedtitle});

		Log.d("Z", "[Updating] 01");
		try {
			Log.d("Z", "[Updating] 02");
			URL url = new URL(feedurl);
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			RSSHandler rh = new RSSHandler();
			xr.setContentHandler(rh);
			Log.d("Z", "[Updating] 03");
			URLConnection con = url.openConnection();
			con.setConnectTimeout(30000);
			con.setReadTimeout(30000);
			Log.d("Z", "[Updating] 03A");
			InputStream in = con.getInputStream();
			Log.d("Z", "[Updating] 03B");
//			InputStream in = url.openStream(); //Doesn't accept timeouts
			InputSource is = new InputSource(in);
			Log.d("Z", "[Updating] 03C");
			xr.parse(is);
			Log.d("Z", "[Updating] 03D");

			for (RSSItem item : rh.items) {
				ContentValues v = new ContentValues();
				v.put("feed", feedtitle);
				v.put("title", item.mTitle);
				v.put("url", item.mURL);
				v.put("guid", item.mGUID);
				v.put("epoch", item.mDate.getTime()/1000);
				
				try {
					mSQL.insertOrThrow("feeditems", null, v);
					newItemCount += 1;
				} catch (SQLiteConstraintException e) {
				}
			}
			Log.d("Z", "[Updating] 08");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Log.d("Z", "[Updating] 09");
		return newItemCount;
	}
	
	boolean isWifiConnected() {
		try {
			ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			return networkInfo.isConnected();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	boolean isDataConnected() {
		try {
			ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo networkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			return networkInfo.isConnected();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	int getConnectionRate() {
		if (isWifiConnected()) {
			try {
				return Integer.parseInt(mSharedPreferences.getString("update_rate_wifi", "3600"));
			} catch (Exception e) {
				return 3600;
			}
		} else if (isDataConnected()) {
			try {
				return Integer.parseInt(mSharedPreferences.getString("update_rate_cell", "0"));
			} catch (Exception e) {
				return 0;
			}
		}
		return 0;
	}
	
	void postStatusChange(String _action) {
		Log.d("Z", "postSatusChange(): "+_action);
		sendBroadcast(new Intent().setAction(_action));
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		int newItemTotal = 0;
		postStatusChange("STATUS_UPDATED");

		if (intent == null)
			return;
		
		Log.d("Z", "Downloader");
		
		//PREFS
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean pNotifications = mSharedPreferences.getBoolean("allow_notifications", true);
		
		//DB
		mDatabase = new Database(getApplicationContext());
		mSQL = mDatabase.getWritableDatabase();
		
		HashMap<String,String> mTabFeeds = new HashMap<String,String>();
		mTabFeeds.put("BJH3",  "http://www.hash.cn/feed/");
		mTabFeeds.put("Boxer", "http://www.hash.cn/category/boxerh3/feed/");
		mTabFeeds.put("FMH",   "http://www.hash.cn/category/fullmoonh3/feed/");
		mTabFeeds.put("Trash", "http://www.hash.cn/category/hashtrash/feed/");
		
		//Download
		for (Entry<String,String> entry : mTabFeeds.entrySet()) {
			Log.d("Z", "Launching.. "+entry.getKey());
			try {
				newItemTotal += this.getNewItems(entry.getKey(), entry.getValue());
			} catch (Exception e) {
				e.printStackTrace();
			}
			Log.d("Z", "... returned new value: "+newItemTotal);
		}
		
		//UI Finished Updating
		postStatusChange("STATUS_UPDATED");
		
		if (newItemTotal > 0) {
			//Update
			postStatusChange("SOME_ACTION");
			
			if (pNotifications) {
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
		
		//CLEANUP
		firstRun = false;
		mSQL.close();
		mDatabase.close();
	}

	private class RSSItem {
		String mTitle, mURL, mGUID;
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
			} else if (qName.equalsIgnoreCase("guid")) {
				curItem.mGUID = tmpValue.toString();
			} else if (qName.equalsIgnoreCase("pubdate")) {
				try {
					DateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.getDefault());
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
