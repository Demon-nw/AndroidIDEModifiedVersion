package com.itsaky.androidide.editor.language.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TranslationCompletion extends SQLiteOpenHelper {

	private static TranslationCompletion translationCompletion;
	private OkHttpClient.Builder cli;
	private OkHttpClient client;

	private TranslationCompletion(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
		super(context, name, factory, version);
		if (!tableIsExist(getWritableDatabase(), "TranslationContent")) {
			createDataTable(getWritableDatabase(), "TranslationContent", "Original text,Content text");
		}
		cli = new OkHttpClient.Builder();
		client = cli.proxySelector(new ProxySelector() {
			@Override
			public List<Proxy> select(URI uri) {
				return Collections.singletonList(Proxy.NO_PROXY);
			}

			@Override
			public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {

			}
		}).build();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	public static TranslationCompletion getInstance(Context context) {
		if (translationCompletion == null) {
			translationCompletion = new TranslationCompletion(context, "Translation", null, 1);
		}
		return translationCompletion;
	}

	public String query(String original) {
		SQLiteDatabase db = getWritableDatabase();
		String[] columns = { "Original", "Content" };
		String selection = "Original" + " = ?";
		String[] selectionArgs = { original };
		Cursor cursor = db.query("TranslationContent", columns, selection, selectionArgs, null, null, null);
		if (cursor.getCount() > 0) {
			String data = "翻译失败";
			while (cursor.moveToNext()) {
				data = cursor.getString(cursor.getColumnIndex("Content"));
			}
			return data;
		} else {
			String str = original;
			if (str.contains("(")) {
				str = str.split("\\(")[0];
			}
			Request request = new Request.Builder().url(getUrl(str.replaceAll("(?<=[a-z])([A-Z])", " $1"))).get()
					.build();
			Call call = client.newCall(request);
			call.enqueue(new Callback() {

				@Override
				public void onFailure(Call p1, IOException p2) {
				}

				@Override
				public void onResponse(Call p1, Response p2) throws IOException {
					Matcher mr = Pattern.compile("(?<=\\Q<p class=\"sentence-text\">\\E).*?(?=\\Q</p>\\E)", 40)
							.matcher(p2.body().string());
					List<String> list = new ArrayList<>();
					while (mr.find()) {
						list.add(mr.group());
					}
					insert(original, list.get(1));
				}
			});
			return "刷新列表查看翻译";
		}
	}

	private void insert(String original, String content) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put("Original", original);
		values.put("Content", content);
		db.insert("TranslationContent", null, values);
	}

	private void createDataTable(SQLiteDatabase db, String tableName, String field) {
		String sql = "create table %s ( id integer primary key autoincrement,%s)";
		db.execSQL(String.format(sql, tableName, field));
	}

	private boolean tableIsExist(SQLiteDatabase db, String tableName) {
		boolean result = false;
		if (tableName == null) {
			return false;
		}
		Cursor cursor = null;
		try {
			String sql = "select count(*) as c from Sqlite_master  where type ='table' and name ='" + tableName.trim()
					+ "' ";
			cursor = db.rawQuery(sql, null);
			if (cursor.moveToNext()) {
				int count = cursor.getInt(0);
				if (count > 0) {
					result = true;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static String getUrl(String text) {
		return String.format("https://dictweb.translator.qq.com/eduTranslate?sentence=%s&channel=tencent", text);
	}
}