package com.darksidebio.bjh3;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.darksidebio.bjh3.R;
//import com.tencent.mm.sdk.openapi.BaseReq;
//import com.tencent.mm.sdk.openapi.IWXAPI;
//import com.tencent.mm.sdk.openapi.SendMessageToWX;
//import com.tencent.mm.sdk.openapi.WXAPIFactory;
//import com.tencent.mm.sdk.openapi.WXMediaMessage;
//import com.tencent.mm.sdk.openapi.WXWebpageObject;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings({ "unused", "deprecation" })
public class MainActivity extends Activity {
	AlarmManager svc_alarm;
	ViewPager mViewPager;
	ActionBar mActionBar;
	TabListener mTabListener;
	LayoutInflater svc_inflater;
	SharedPreferences mPrefs;
	ListView mViewSongs;
//	ProgressBar mProgress;

	static Database mDatabase;
	public final HashMap<String, View> mhViews = new HashMap<String, View>();

	class Song {
		String pName, pColor, pGroup, pCompareString;
		NodeList pNodes;

		public Song(Node inputNode) {
			NamedNodeMap nnm = inputNode.getAttributes();
			pNodes = inputNode.getChildNodes();

			try {
				pGroup = nnm.getNamedItem("group").getNodeValue();
			} catch (Exception e) {
				pGroup = "none";
			}

			pName = nnm.getNamedItem("name").getNodeValue().toLowerCase();
			pCompareString = pGroup + pName;
		}
	}

	static class SongComparator implements Comparator<Song> {
		public int compare(Song a, Song b) {
			// return a.pCompareString.compareTo(b.pCompareString);
			return a.pName.compareTo(b.pName);
		}
	}

	private void attachCursorAdapter(String title, ListView lv) {
		Log.d("Z", "attachCursorAdapter(" + title + ",..)");

		boolean pDisplayURL = mPrefs.getBoolean("display_url_link", true);
		boolean pDisplayOLD = mPrefs.getBoolean("display_old_item", false);

		String[] fromFieldNames = new String[] { "title", "epochdate", "feed", (pDisplayURL ? "url" : "guid"), };
		int[] toViewIDs = new int[] { R.id.tvName, R.id.tvDate, R.id.tvFeed, R.id.tvURL, };

		String mQueryString;
		Cursor c;

		// String mQueryString =
		// "SELECT id as _id, feed, title, url, guid, strftime('%Y-%m-%d %H:%M:%S',epoch,'unixepoch') as epochdate FROM feeditems WHERE feed=? and active>=? ORDER BY epoch DESC, _id DESC";
		// Cursor c = mDatabase.getReadableDatabase().rawQuery(mQueryString, new
		// String[] { title, (pDisplayOLD ? "0" : "1") });
		// String mQueryString =
		// "SELECT id as _id, feed, title, url, guid, strftime('%Y-%m-%d %H:%M:%S',epoch,'unixepoch') as epochdate FROM feeditems WHERE active>=? GROUP BY guid ORDER BY epoch DESC, _id DESC";
		// Cursor c = mDatabase.getReadableDatabase().rawQuery(mQueryString, new
		// String[] { (pDisplayOLD ? "0" : "1") });

		// strftime('%Y-%m-%d %H:%M:%S',epoch,'unixepoch')
		if (title.equals("BJH3")) {
			// ALL
			mQueryString = "SELECT id as _id, group_concat(feed,', ') as feed, title, url, guid, strftime('%m/%d %H:%M',epoch,'unixepoch') as epochdate FROM feeditems WHERE active>=? GROUP BY guid ORDER BY epoch DESC, _id DESC";
			c = mDatabase.getReadableDatabase().rawQuery(mQueryString, new String[] { (pDisplayOLD ? "0" : "1") });
		} else {
			// SINGLE
			mQueryString = "SELECT id as _id, group_concat(feed,', ') as feed, title, url, guid, strftime('%m/%d %H:%M',epoch,'unixepoch') as epochdate FROM feeditems WHERE feed=? AND active>=? GROUP BY guid ORDER BY epoch DESC, _id DESC";
			c = mDatabase.getReadableDatabase().rawQuery(mQueryString, new String[] { title, (pDisplayOLD ? "0" : "1") });
		}

		lv.setAdapter(new SimpleCursorAdapter(this, R.layout.fragment_listitem, c, fromFieldNames, toViewIDs, 0) {
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);

				TextView tvFeed = (TextView) v.findViewById(R.id.tvFeed);

				Resources res = getResources();
				final Drawable drawable = res.getDrawable(R.drawable.bullet_square10);

				String[] feeds = tvFeed.getText().toString().split(", ");
				Arrays.sort(feeds);
				String feedSorted = "";
				for (int i = 0; i < feeds.length; i++) {
					if (i > 0)
						feedSorted = feedSorted.concat("+");
					feedSorted = feedSorted.concat(feeds[i]);

					if (feeds[i].equals("Boxer"))
						((GradientDrawable) drawable).setColor(0xFF7FA016);// green
					else if (feeds[i].equals("FMH"))
						((GradientDrawable) drawable).setColor(0xFF39A4C6);// blue
					else if (feeds[i].equals("Trash"))
						((GradientDrawable) drawable).setColor(0xFFAF96FF);// purple
					else if (feeds[i].equals("BJH3"))
						((GradientDrawable) drawable).setColor(0xFFFFB759);// orange
				}

				drawable.mutate();
				tvFeed.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
				tvFeed.setText(feedSorted);

				v.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View itemview) {
						TextView tvURL = (TextView) itemview.findViewById(R.id.tvURL);
						Intent i = new Intent(Intent.ACTION_VIEW);
						i.setData(Uri.parse(tvURL.getText().toString()));
						startActivity(i);
					}
				});

				v.setOnLongClickListener(new OnLongClickListener() {
					@Override
					public boolean onLongClick(View itemview) {
						TextView tvName = (TextView) itemview.findViewById(R.id.tvName);
						TextView tvURL = (TextView) itemview.findViewById(R.id.tvURL);
						String shareText = tvName.getText() + "\n" + tvURL.getText();

						ClipboardManager svc_clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						svc_clipboard.setText(shareText);

						Toast.makeText(MainActivity.this, "Successfully Copied", Toast.LENGTH_SHORT).show();

						// // Test SEND:begin
						// Uri shareURI = Uri.parse(tvURL.getText().toString());
						//
						// Intent intSend = new Intent(Intent.ACTION_SEND);
						// // Intent intSend = new Intent(Intent.ACTION_VIEW);
						//
						// intSend.setPackage("com.tencent.mm");// 直接打开微信
						// // intSend.putExtra(Intent.EXTRA_SUBJECT, "Share");
						// // intSend.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						//
						// // ComponentName componentName = new
						// // ComponentName("com.tencent.mm",
						// // "com.tencent.mm.ui.tools.ShareImgUI");
						// // intSend.setComponent(componentName);
						//
						// // intSend.setType("image/*");
						// intSend.setType("text/html");
						// // intSend.setType("text/plain");
						// String h =
						// "<html><body>Something <a href='http://baidu.com'>http://baidu.com</a> done</body></html>";
						//
						// intSend.setData(shareURI);
						// //intSend.putExtra(Intent.EXTRA_SUBJECT,
						// tvName.getText().toString());
						// //intSend.putExtra(Intent.EXTRA_TEXT,
						// tvURL.getText().toString());
						// //intSend.setData(h);
						// intSend.putExtra(Intent.EXTRA_SUBJECT, "subj");
						// intSend.putExtra(Intent.EXTRA_TEXT, h);
						// // intSend.putExtra("sms_body", shareText);
						// // intSend.putExtra(Intent.EXTRA_STREAM,
						// // getResources().( R.drawable.beijing_trans96beer));
						//
						// // intent.setType("image/*");//分享发送的数据类型
						// // intent.putExtra(Intent.EXTRA_SUBJECT,
						// "主题");//分享的主题
						// // intent.putExtra(Intent.EXTRA_TEXT, "分享内容");//分享的内容
						// // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						// //
						// intent.putExtra(Intent.EXTRA_STREAM,Uri.parse("file:////sdcard//ganyibei.png"));//分享的图片
						//
						// startActivity(Intent.createChooser(intSend,
						// "Share"));
						// // startActivity(intSend);
						// // Test SEND:end

						return false;
					}

				});

				return v;
			}
		});
	}

	public View inflateList(String title) {
		View v = svc_inflater.inflate(R.layout.fragment_list, null);
		v.setTag(1);
		ListView lv = (ListView) v.findViewById(R.id.flist);
		attachCursorAdapter(title, lv);
		return v;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("Z", "[MAIN] Create()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mViewPager = (ViewPager) findViewById(R.id.pager);
		svc_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// Backend
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mDatabase = new Database(this);

		PendingIntent mTimerIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, Alarm.class), 0);
		svc_alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
		svc_alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, 60000, mTimerIntent);

		MyLoadingBroadcastReceiver br = new MyLoadingBroadcastReceiver();
		IntentFilter f = new IntentFilter();
		f.addAction("STATUS_UPDATING");
		f.addAction("STATUS_UPDATED");
		f.addAction("STATUS_DATACHANGE");
		registerReceiver(br, f);

		// // WX
		// try {
		// String wx_api_key = "";
		// IWXAPI api = WXAPIFactory.createWXAPI(this, wx_api_key, true);
		// api.registerApp(wx_api_key);
		//
		// WXWebpageObject webpage = new WXWebpageObject();
		// webpage.webpageUrl = "http://www.wechat.com";
		//
		// WXMediaMessage msg = new WXMediaMessage(webpage);
		// msg.title = "Wechat homepage";
		// msg.description = "Welcome to Wechat!";
		//
		// SendMessageToWX.Req req = new SendMessageToWX.Req();
		// req.message = msg;
		// req.scene = SendMessageToWX.Req.WXSceneSession;
		//
		// api.sendReq(req);
		// } catch (Exception e) {
		// Log.d("Z", "WXAPI: " + e);
		// }

		// Views - init
		mViewSongs = (ListView) svc_inflater.inflate(R.layout.fragment_songlist, null);
		mViewSongs.setAdapter(new BaseAdapter() {
			ArrayList<Song> pList = null;
			HashMap<String, Integer> pGroupColors = new HashMap<String, Integer>();

			@Override
			public int getCount() {
				// Render Songs
				if (pList == null) {
					pList = new ArrayList<Song>();

					try {
						DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
						DocumentBuilder db = dbf.newDocumentBuilder();
						Document doc = db.parse(getAssets().open("songs.xml"));
						doc.getDocumentElement().normalize();
						NodeList n = doc.getElementsByTagName("song");
						for (int sIndex = 0; sIndex < n.getLength(); sIndex++) {
							pList.add(new Song(n.item(sIndex)));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					Collections.sort(pList, new SongComparator());
				}
				return pList.size();
			}

			@Override
			public Song getItem(int i) {
				return pList.get(i);
			}

			@Override
			public long getItemId(int i) {
				return i;
			}

			@SuppressLint("NewApi")
			@Override
			public View getView(int pos, View v, ViewGroup parent) {
				// if (v != null)
				// return v;

				Song mySong = this.getItem(pos);
				v = LayoutInflater.from(MainActivity.this).inflate(R.layout.fragment_songitem, null);

				TextView tvName = (TextView) v.findViewById(R.id.songName);
				tvName.setText(mySong.pName);

				// Lyrics
				LinearLayout lyricLayout = (LinearLayout) v.findViewById(R.id.songLyrics);
				lyricLayout.setVisibility(View.GONE);

				// // Group Color
				// Resources res = getResources();
				// final Drawable drawable =
				// res.getDrawable(R.drawable.bullet_square10);
				// try {
				// // existing color
				// ((GradientDrawable)
				// drawable).setColor(pGroupColors.get(mySong.pGroup));
				// } catch (Exception e) {
				// // new color
				// ((GradientDrawable)
				// drawable).setColor(pGroupColors.put(mySong.pGroup,
				// 0xFF00FFFF));
				// }
				// drawable.mutate();
				// tvName.setCompoundDrawablesWithIntrinsicBounds(drawable,
				// null, null, null);

				v.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						LinearLayout lyricLayout = (LinearLayout) v.findViewById(R.id.songLyrics);
						lyricLayout.setVisibility(lyricLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);

						try {
							ListView lvParent = (ListView) v.getParent();
							int vPos = lvParent.getPositionForView(v);
							if (vPos + 1 == lvParent.getCount())
								lvParent.setSelection(vPos);
						} catch (Exception e) {
						}
					}
				});

				Boolean lastWasInfo = true;
				for (int i = 0; i < mySong.pNodes.getLength(); i++) {
					Node n = mySong.pNodes.item(i);
					if (n.getNodeType() == Node.ELEMENT_NODE) {
						TextView tvNew = new TextView(MainActivity.this);
						tvNew.setTextAppearance(MainActivity.this, android.R.style.TextAppearance_Medium);
						tvNew.setText(n.getTextContent());
						tvNew.setPadding(20, 20, 0, 0);

						if (n.getNodeName().equals("info")) {
							tvNew.setTypeface(null, Typeface.ITALIC);
							tvNew.setTextAppearance(MainActivity.this, android.R.style.TextAppearance_Small);
							tvNew.setPadding(10, 20, 0, 0);
							if (lastWasInfo)
								tvNew.setPadding(10, 0, 0, 0);
							lastWasInfo = true;
						} else {
							lastWasInfo = false;
						}

						lyricLayout.addView(tvNew);
					}
				}

				return v;
			}

		});

		// Views
		mhViews.put("BJH3", inflateList("BJH3"));
		mhViews.put("FMH", inflateList("FMH"));
		mhViews.put("Boxer", inflateList("Boxer"));
		mhViews.put("Trash", inflateList("Trash"));
		mhViews.put("Songs", mViewSongs);
		// mhViews.put("Map", inflater.inflate(R.layout.fragment_map, null));

		// Handlers
		mTabListener = new TabListener() {
			@Override
			public void onTabSelected(Tab t, FragmentTransaction f) {
				mViewPager.setCurrentItem(t.getPosition());
			}

			@Override
			public void onTabUnselected(Tab t, FragmentTransaction f) {
			}

			@Override
			public void onTabReselected(Tab t, FragmentTransaction f) {
			}
		};

		mViewPager.setAdapter(new PagerAdapter() {
			public Object instantiateItem(ViewGroup cont, int pos) {
				View layout = (View) mhViews.get(mActionBar.getTabAt(pos).getText());
				cont.addView(layout);
				return layout;
			}

			public void destroyItem(ViewGroup cont, int pos, Object obj) {
				cont.removeView((View) obj);
			}

			@Override
			public int getCount() {
				return mhViews.size();
			}

			@Override
			public boolean isViewFromObject(View v, Object obj) {
				return v == obj;
			}
		});

		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				try {
					getActionBar().setSelectedNavigationItem(position);
				} catch (Exception e) {
				}
			}
		});

		// ActionBar
		mActionBar = getActionBar();
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		mActionBar.setTitle("Beijing Hash House Harriers");
		mActionBar.removeAllTabs();
		mActionBar.addTab(mActionBar.newTab().setTabListener(mTabListener).setText("BJH3"));
		mActionBar.addTab(mActionBar.newTab().setTabListener(mTabListener).setText("FMH"));
		mActionBar.addTab(mActionBar.newTab().setTabListener(mTabListener).setText("Boxer"));
		mActionBar.addTab(mActionBar.newTab().setTabListener(mTabListener).setText("Trash"));
		mActionBar.addTab(mActionBar.newTab().setTabListener(mTabListener).setText("Songs"));
		// mActionBar.addTab(mActionBar.newTab().setTabListener(mTabListener).setText("Map"));

		// ProgressBar
//		mProgress = (ProgressBar) findViewById(R.id.progressBar1);
	}

	public long getDexCRC() {
		ZipFile zf = null;
		long ret = -1;

		try {
			zf = new ZipFile(getPackageManager().getApplicationInfo(getPackageName(), 0).sourceDir);
			ZipEntry ze = zf.getEntry("classes.dex");
			ret = ze.getCrc();
		} catch (Exception e) {
		}

		try {
			zf.close();
		} catch (Exception e) {
		}

		return ret;
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_settings:
			startActivityForResult(new Intent(this, PrefsActivity.class), 0);
			return true;
		case R.id.action_update:
			// Reset Times
			ContentValues v = new ContentValues();
			v.put("epoch", 1);
			mDatabase.getWritableDatabase().update("feedtimes", v, null, null);
			// Update
			startService(new Intent(this, Downloader.class));
			return true;
		case R.id.action_update_app:
			new UpdateTask().execute();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	class MyLoadingBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context c, Intent i) {
			Log.d("Z", "MyLoadingBroadcastReceiver.onReceive: " + i.getAction());
			if (i.getAction().equalsIgnoreCase("STATUS_UPDATING")) {
				if (mActionBar.getSubtitle() != null && mActionBar.getSubtitle().length() > 0) {
					mActionBar.setSubtitle(mActionBar.getSubtitle() + ".");
//					mProgress.setProgress(mProgress.getProgress()+20);
//					mProgress.setVisibility(View.VISIBLE);
				} else {
					mActionBar.setSubtitle("Updating..");
//					mProgress.setProgress(20);
//					mProgress.setVisibility(View.VISIBLE);
				}
			} else if (i.getAction().equalsIgnoreCase("STATUS_UPDATED")) {
				mActionBar.setSubtitle(null);
//				mProgress.setVisibility(View.GONE);
			} else if (i.getAction().equalsIgnoreCase("STATUS_DATACHANGE")) {
				for (String key : mhViews.keySet()) {
					try {
						View v = mhViews.get(key);
						if (v != null && v.getTag() != null && v.getTag().equals(1)) {
							ListView lv = (ListView) v.findViewById(R.id.flist);
							attachCursorAdapter(key, lv);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private class UpdateTask extends AsyncTask<Void, Void, String> {
		private ProgressDialog pDialog = null;

		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = ProgressDialog.show(MainActivity.this, "Update", "Checking for updates..");
			pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			pDialog.setCancelable(true);
			pDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface d) {
					UpdateTask.this.cancel(true);
				}
			});
			pDialog.show();
		}

		@Override
		protected String doInBackground(Void... none) {
			try {
				URL url = new URL("http://www.darksidebio.com/android/bjh3/version.cgi");
				URLConnection con = url.openConnection();
				con.setConnectTimeout(30000);
				con.setReadTimeout(30000);

				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(new InputSource(con.getInputStream()));
				doc.getDocumentElement().normalize();

				String crc_rem = doc.getDocumentElement().getAttribute("crc");
				String crc_loc = Long.toString(getDexCRC());
				return (crc_loc.equals(crc_rem) ? "Already using latest version." : "An update is available.");
			} catch (Exception e) {
				e.printStackTrace();
				return e.getMessage();
			}
		}

		protected void onPostExecute(final String sResponse) {
			super.onPostExecute(sResponse);

			if (pDialog.isShowing())
				pDialog.dismiss();

			AlertDialog pAlert = new AlertDialog.Builder(MainActivity.this).create();
			// pAlert.setIcon(android.R.drawable.checkbox_on_background);
			pAlert.setTitle("Update");
			pAlert.setCancelable(true);

			pAlert.setButton("Close", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
				}
			});

			pAlert.setButton2("Download", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					Request req = new DownloadManager.Request(Uri.parse("http://www.darksidebio.com/android/bjh3/download.cgi"));
					req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
					req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "BJH3.apk");
					req.setVisibleInDownloadsUi(true);

					DownloadManager svc_download = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
					svc_download.enqueue(req);
				}
			});

			pAlert.setMessage(sResponse);
			pAlert.show();
		}
	}
}
