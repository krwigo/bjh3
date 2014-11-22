package com.darksidebio.bjh3;

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

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import android.widget.Toast;

@SuppressWarnings("deprecation")
public class MainActivity extends Activity {
	ProgressBar mProgress;
	BroadcastReceiver mBroadcastReceiver;
	TabHost mViewHost;
	ViewPager mViewPager;
	AlarmManager svc_alarm;
	LayoutInflater svc_inflater;
	SharedPreferences mPrefs;
	Resources mResources;
	static Database mDatabase;
	ArrayList<View> mTabList = new ArrayList<View>();
	ArrayList<Song> mSongListSource = new ArrayList<Song>();

	boolean mSongAdapterFirstRun = true;
	// HashMap<Integer, View> mSongViewMap = new HashMap<Integer, View>();
	HashMap<String, Integer> mSongGroupColorMap = new HashMap<String, Integer>();
	ArrayList<Integer> mSongGroupColors = new ArrayList<Integer>(Arrays.asList(0xFFDF0101, 0xFFDF3A01, 0xFFDBA901, 0xFFD7DF01, 0xFFA5DF00, 0xFF3ADF00, 0xFF01DFA5, 0xFF01A9DB, 0xFF013ADF, 0xFFA901DB, 0xFFDF01A5, 0xFF424242));

	class Song {
		int pColor;
		NodeList pNodes;
		String pName, pGroup;
		boolean isExpanded = false;

		public Song(Node inputNode) {
			NamedNodeMap nnm = inputNode.getAttributes();
			pNodes = inputNode.getChildNodes();
			pName = nnm.getNamedItem("name").getNodeValue().toLowerCase(Locale.ENGLISH);
			pGroup = nnm.getNamedItem("group").getNodeValue().toLowerCase(Locale.ENGLISH);
		}
	}

	static class SongComparator implements Comparator<Song> {
		public int compare(Song a, Song b) {
			// return a.pCompareString.compareTo(b.pCompareString);
			// return a.pName.compareTo(b.pName);
			// return a.pColor - b.pColor;
			return (a.pColor + a.pName).compareTo(b.pColor + b.pName);
		}
	}

	class SongViewHolder {
		private LinearLayout lyricLayout;
		private TextView tvName;
		private Drawable drawable;
	}

	private void attachCursorAdapter(String title, ListView lv) {
		Log.d("Z", "attachCursorAdapter(" + title + ",..)");

		boolean pDisplayURL = mPrefs.getBoolean("display_url_link", true);
		boolean pDisplayOLD = mPrefs.getBoolean("display_old_item", false);

		String[] fromFieldNames = new String[] { "title", "epochdate", "feed", (pDisplayURL ? "url" : "guid"), };
		int[] toViewIDs = new int[] { R.id.tvName, R.id.tvDate, R.id.tvFeed, R.id.tvURL, };

		String mQueryString;
		Cursor c;

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
				TextView tvDate = (TextView) v.findViewById(R.id.tvDate);

				Drawable drawable = mResources.getDrawable(R.drawable.bullet_square10);

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
						((GradientDrawable) drawable).setColor(0xFFFF6600);// orange
				}

				drawable.mutate();
				tvDate.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
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
						return false;
					}
				});

				return v;
			}
		});
	}

	public View inflateList(String title) {
		View v = svc_inflater.inflate(R.layout.fragment_list, null);
		v.setTag(title);
		ListView lv = (ListView) v.findViewById(R.id.flist);
		attachCursorAdapter(title, lv);
		return v;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d("Z", "[MAIN] Create()");
		super.onCreate(savedInstanceState);
		setTitle(getResources().getString(R.string.app_name_long));
		setContentView(R.layout.activity_main);

		mViewHost = (TabHost) findViewById(R.id.myhost);
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mProgress = (ProgressBar) findViewById(R.id.progressBar1);

		svc_inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		svc_alarm = (AlarmManager) getSystemService(ALARM_SERVICE);

		mResources = getResources();
		mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		mDatabase = new Database(this);

		// Timer
		PendingIntent mTimerIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, Alarm.class), 0);
		svc_alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0, 60000, mTimerIntent);

		// Messages
		mBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context c, Intent i) {
				Log.d("Z", "mBroadcastReceiver.onReceive(): " + i.getAction());
				try {
					if (i.getAction().equalsIgnoreCase("STATUS_UPDATING")) {
						// Updating..
						if (mProgress.getVisibility() == ProgressBar.VISIBLE) {
							mProgress.setProgress(mProgress.getProgress() + 1);
							mProgress.setVisibility(View.VISIBLE);
						} else {
							mProgress.setProgress(1);
							mProgress.setVisibility(View.VISIBLE);
						}
					} else if (i.getAction().equalsIgnoreCase("STATUS_UPDATED")) {
						// Updated
						mProgress.setVisibility(View.GONE);
					} else if (i.getAction().contains("DOWNLOAD_COMPLETE")) {
						// Download Completed
					} else if (i.getAction().equalsIgnoreCase("STATUS_DATACHANGE")) {
						// Data changed, Update adapters
						for (View v : mTabList) {
							try {
								if (v != null && v.getTag() != null && v.getTag().equals(1)) {
									ListView lv = (ListView) v.findViewById(R.id.flist);
									if (lv != null) {
										attachCursorAdapter((String) v.getTag(), lv);
									}
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		IntentFilter f = new IntentFilter();
		f.addAction("STATUS_UPDATING");
		f.addAction("STATUS_UPDATED");
		f.addAction("STATUS_DATACHANGE");
		// f.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
		registerReceiver(mBroadcastReceiver, f);

		// Songs
		ListView viewSongs = (ListView) svc_inflater.inflate(R.layout.fragment_songlist, null);
		viewSongs.setTag("Songs");

		try {
			mSongListSource.clear();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(getAssets().open("songs.xml"));
			doc.getDocumentElement().normalize();
			NodeList n = doc.getElementsByTagName("song");
			for (int sIndex = 0; sIndex < n.getLength(); sIndex++) {
				Song s = new Song(n.item(sIndex));
				try {
					s.pColor = mSongGroupColorMap.get(s.pGroup);
				} catch (NullPointerException e) {
					mSongGroupColorMap.put(s.pGroup, s.pColor = mSongGroupColors.remove(0));
				}
				mSongListSource.add(s);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Collections.sort(mSongListSource, new SongComparator());
		}

		viewSongs.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Song item = (Song) parent.getItemAtPosition(position);
				item.isExpanded = !item.isExpanded;

				SongViewHolder holder = (SongViewHolder) view.getTag();
				holder.lyricLayout.setVisibility(item.isExpanded ? View.VISIBLE : View.GONE);

				if (position + 1 == parent.getCount())
					parent.setSelection(position);
			}
		});
		
		viewSongs.setAdapter(new ArrayAdapter<Song>(this, 0, 0, mSongListSource) {
			public View getView(int pos, View v, ViewGroup parent) {
				Song item = getItem(pos);
				SongViewHolder holder;

				if (v == null) {
					v = LayoutInflater.from(this.getContext()).inflate(R.layout.fragment_songitem, parent, false);
					holder = new SongViewHolder();
					holder.lyricLayout = (LinearLayout) v.findViewById(R.id.songLyrics);
					holder.tvName = (TextView) v.findViewById(R.id.songName);
					holder.drawable = mResources.getDrawable(R.drawable.bullet_square10);
					v.setTag(holder);
				} else {
					holder = (SongViewHolder) v.getTag();
				}

				if (item != null) {
					// Song Name
					holder.tvName.setText(item.pName);
					// Song Bullet
					holder.drawable.mutate();
					((GradientDrawable) holder.drawable).setColor(item.pColor);
					holder.tvName.setCompoundDrawablesWithIntrinsicBounds(holder.drawable, null, null, null);
					// Song Lyrics
					holder.lyricLayout.setVisibility(item.isExpanded ? View.VISIBLE : View.GONE);
					holder.lyricLayout.removeAllViews();
					Boolean lastWasInfo = true;
					for (int i = 0; i < item.pNodes.getLength(); i++) {
						Node n = item.pNodes.item(i);
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
							holder.lyricLayout.addView(tvNew);
						}
					}
				}
				
				return v;
			}
		});

		// TabHost
		mViewHost.setup();
		mViewHost.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabname) {
				try {
					mViewPager.setCurrentItem(mViewHost.getCurrentTab());
				} catch (Exception e) {
					Log.d("Z", "mViewPager.setCurrentItem() failed");
				}
			}
		});

		// Add Tabs
		// ,getResources().getDrawable(R.drawable.beijing_trans96beer)
		mViewHost.addTab(mViewHost.newTabSpec("BJH3").setContent(R.id.blanktab).setIndicator("BJH3"));
		mTabList.add(inflateList("BJH3"));

		mViewHost.addTab(mViewHost.newTabSpec("FMH").setContent(R.id.blanktab).setIndicator("FMH"));
		mTabList.add(inflateList("FMH"));

		mViewHost.addTab(mViewHost.newTabSpec("Boxer").setContent(R.id.blanktab).setIndicator("Boxer"));
		mTabList.add(inflateList("Boxer"));

		mViewHost.addTab(mViewHost.newTabSpec("Trash").setContent(R.id.blanktab).setIndicator("Trash"));
		mTabList.add(inflateList("Trash"));

		mViewHost.addTab(mViewHost.newTabSpec("Songs").setContent(R.id.blanktab).setIndicator("Songs"));
		mTabList.add(viewSongs);

		// ViewPager
		mViewPager.setAdapter(new PagerAdapter() {
			public Object instantiateItem(ViewGroup cont, int pos) {
				View layout = mTabList.get(pos);
				cont.addView(layout);
				return layout;
			}

			public void destroyItem(ViewGroup cont, int pos, Object obj) {
				cont.removeView((View) obj);
			}

			@Override
			public int getCount() {
				return mTabList.size();
			}

			@Override
			public boolean isViewFromObject(View v, Object obj) {
				return (v == obj);
			}
		});

		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				try {
					mViewHost.setCurrentTab(position);
				} catch (Exception e) {
					Log.d("Z", "mViewHost.setCurrentTab() failed");
				}
			}
		});

		// Update on launch
		try {
			if (mPrefs.getBoolean("allow_autoupdate", true)) {
				long updater_lastrun = mPrefs.getLong("INTERNAL_UPDATER_LASTRUN", 0);
				long updater_now = System.currentTimeMillis() / 1000;
				if (updater_lastrun > updater_now || updater_lastrun + (24 * 3600) < updater_now) {
					// Launch updater every 24 hours
					new UpdateTask(this, true, Long.toString(getDexCRC())).execute();
					Editor prefEdit = mPrefs.edit();
					// Mark now as last run
					prefEdit.putLong("INTERNAL_UPDATER_LASTRUN", System.currentTimeMillis() / 1000);
					prefEdit.commit();
				}
			}
		} catch (Exception e) {
			Log.d("Z", "Updater; Exception during daily App Update check: " + e.toString());
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mBroadcastReceiver);
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
			// Open Settings
			startActivityForResult(new Intent(this, PrefsActivity.class), 0);
			return true;
		case R.id.action_website:
			// Open Website
			Intent web = new Intent(Intent.ACTION_VIEW);
			web.setData(Uri.parse("http://www.hash.cn"));
			startActivity(web);
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
			// Updater
			new UpdateTask(this, false, Long.toString(getDexCRC())).execute();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

}
