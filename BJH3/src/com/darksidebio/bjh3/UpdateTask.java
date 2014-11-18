package com.darksidebio.bjh3;

import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.util.Log;

class UpdateTask extends AsyncTask<Void, Void, String> {
	private ProgressDialog pDialog = null;
	private Context pContext = null;
	private Boolean pBeQuiet = false;
	private String pCRC = "";
	private String pAndroidID = "";
	private static String STR_UPDATE_AVAILABLE = "An update is available.";
	private static String STR_UPDATE_USINGLATEST = "Already using latest version.";
	private static String URL_CHECK = "http://www.darksidebio.com/android/bjh3/version.cgi?";
	private static String URL_DOWNLOAD = "http://www.darksidebio.com/android/bjh3/download.cgi?";

	UpdateTask(Context cContext, Boolean bBeQuiet, String sCRC) {
		Log.d("Z", "UpdateTask(): Started; quiet:" + bBeQuiet + " crc:" + sCRC);
		pContext = cContext;
		pBeQuiet = bBeQuiet;
		pCRC = sCRC;
		pAndroidID = Secure.getString(pContext.getContentResolver(), Secure.ANDROID_ID);
	}

	protected void onPreExecute() {
		super.onPreExecute();
		pDialog = new ProgressDialog(pContext);
		if (pBeQuiet)
			return;
		pDialog.setTitle("Update");
		pDialog.setMessage("Checking for updates..");
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
			if (pCRC.length() <= 0)
				return "Manually download?";

			URL url = new URL(URL_CHECK + "?crc=" + pCRC + "&id=" + pAndroidID);
			URLConnection con = url.openConnection();
			con.setConnectTimeout(30000);
			con.setReadTimeout(30000);

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(con.getInputStream()));
			doc.getDocumentElement().normalize();

			String crc_rem = doc.getDocumentElement().getAttribute("crc");
			String crc_loc = pCRC;
			return (crc_loc.equals(crc_rem) ? STR_UPDATE_USINGLATEST : STR_UPDATE_AVAILABLE);
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}
	}

	@SuppressWarnings("deprecation")
	protected void onPostExecute(final String sResponse) {
		super.onPostExecute(sResponse);

		Log.d("Z", "UpdateTask(): onPostExecute; response:" + sResponse);

		if (pDialog.isShowing())
			pDialog.dismiss();
		if (pBeQuiet && !sResponse.equals(STR_UPDATE_AVAILABLE))
			return;

		AlertDialog pAlert = new AlertDialog.Builder(pContext).create();
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
				Request req = new DownloadManager.Request(Uri.parse(URL_DOWNLOAD));
				req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
				req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "BJH3.apk");
				req.setVisibleInDownloadsUi(true);

				DownloadManager svc_download = (DownloadManager) pContext.getSystemService(Context.DOWNLOAD_SERVICE);
				svc_download.enqueue(req);
			}
		});

		pAlert.setMessage(sResponse);
		pAlert.show();
	}
}
