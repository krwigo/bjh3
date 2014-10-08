package com.darksidebio.bjh3;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
import android.database.sqlite.SQLiteConstraintException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class Downloader extends IntentService {
	private static boolean hasRun = false;

	public Downloader() {
		super(null);
	}

	int getNewItems(String feedtitle, String feedurl) {
		int newItemCount = 0;
		Database mDatabase = null;
		
		try {
			URL url = new URL(feedurl);
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();
			RSSHandler rh = new RSSHandler();
			xr.setContentHandler(rh);

			xr.parse(new InputSource(url.openStream()));

			for (RSSItem item : rh.items) {
				Log.d("Z", "RSSITEM; "+item.mTitle+" (Feed:"+feedtitle+")");
				
				if (mDatabase == null)
					mDatabase = new Database(getApplicationContext());
				
				ContentValues v = new ContentValues();
				v.put("feed", feedtitle);
				v.put("title", item.mTitle);
				v.put("url", item.mURL);
				
				try {
					mDatabase.insert(v);
					newItemCount += 1;
				} catch (SQLiteConstraintException e) {
					//Dupe
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

		if (mDatabase != null)
			mDatabase.close();
		
		return newItemCount;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		int newItemTotal = 0;
		
		if (hasRun)
			return;
		if (intent == null)
			return;
		// if (wifiOnly && isConnected)
		// return;

		Log.d("Z", "Downloader");
		hasRun = true;
		
		//Download
		newItemTotal += this.getNewItems("BJH3",  "http://www.hash.cn/feed/");
		newItemTotal += this.getNewItems("Boxer", "http://www.hash.cn/category/boxerh3/feed/");
		newItemTotal += this.getNewItems("FMH",   "http://www.hash.cn/category/fullmoonh3/feed/");
		newItemTotal += this.getNewItems("Trash", "http://www.hash.cn/category/hashtrash/feed/");

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
				//.setContentText("Content Updated")
				.build();
			notificationManager.notify(0, noti);
		}
	}

	private class RSSItem {
		String mTitle;
		String mURL;
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
