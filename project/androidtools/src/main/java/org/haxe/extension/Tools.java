package org.haxe.extension;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.content.ContentResolver;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import androidx.core.content.FileProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.haxe.extension.Extension;
import org.haxe.lime.HaxeObject;

/* 
	You can use the Android Extension class in order to hook
	into the Android activity lifecycle. This is not required
	for standard Java code, this is designed for when you need
	deeper integration.

	You can access additional references from the Extension class,
	depending on your needs:

	- Extension.assetManager (android.content.res.AssetManager)
	- Extension.callbackHandler (android.os.Handler)
	- Extension.mainActivity (android.app.Activity)
	- Extension.mainContext (android.content.Context)
	- Extension.mainView (android.view.View)

	You can also make references to static or instance methods
	and properties on Java classes. These classes can be included 
	as single files using <java path="to/File.java" /> within your
	project, or use the full Android Library Project format (such
	as this example) in order to include your own AndroidManifest
	data, additional dependencies, etc.

	These are also optional, though this example shows a static
	function for performing a single task, like returning a value
	back to Haxe from Java.
*/
public class Tools extends Extension
{
	public static final String LOG_TAG = "Tools";

	public static HaxeObject cbObject;

	public static Gson gson;
	public static final int REQUEST_CODE_OPEN_DOCUMENT = 1001;
	public static final int REQUEST_CODE_CREATE_DOCUMENT = 1002;

	public static void initCallBack(final HaxeObject cbObject)
	{
		Tools.cbObject = cbObject;
	}

	public static String[] getGrantedPermissions()
	{
		List<String> granted = new ArrayList<String>();

		try
		{
			// [Android 12+] Use PackageManager.GET_PERMISSIONS; requestedPermissionsFlags is deprecated in API 28+
			PackageInfo info = mainContext.getPackageManager().getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);

			if (info != null && info.requestedPermissions != null)
			{
				int len = info.requestedPermissions.length;
				for (int i = 0; i < len; i++)
				{
					// Use PackageInfo.requestedPermissionsFlags if available (deprecated in API 28 but still works)
					int flags = 0;
					if (Build.VERSION.SDK_INT < 28)
					{
						if (info.requestedPermissionsFlags != null && i < info.requestedPermissionsFlags.length)
							flags = info.requestedPermissionsFlags[i];
					}
					else
					{
						// On API 28+, check via checkSelfPermission as fallback
						flags = (mainContext.checkSelfPermission(info.requestedPermissions[i]) == PackageManager.PERMISSION_GRANTED)
							? 1 : 0;
					}
					if ((flags & 1) != 0)
						granted.add(info.requestedPermissions[i]);
				}
			}
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, e.toString());
		}

		return granted.toArray(new String[granted.size()]);
	}

	public static void makeToastText(final String message, final int duration, final int gravity, final int xOffset, final int yOffset)
	{
		if (mainActivity == null)
		{
			Log.e(LOG_TAG, "makeToastText: mainActivity is null");
			return;
		}

		mainActivity.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Context ctx = (mainContext != null) ? mainContext : mainActivity;
					Toast toast = Toast.makeText(ctx, message, duration);

					if (gravity >= 0)
						toast.setGravity(gravity, xOffset, yOffset);

					toast.show();
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, e.toString());
				}
			}
		});
	}

	public static void showAlertDialog(final String title, final String message, final String positiveLabel, final HaxeObject positiveObject, final String negativeLabel, final HaxeObject negativeObject)
	{
		if (mainActivity == null)
		{
			Log.e(LOG_TAG, "showAlertDialog: mainActivity is null");
			return;
		}

		mainActivity.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					// [Android 12+] Use default AlertDialog theme instead of deprecated Theme_Material_Dialog_Alert
					AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);

					if (title != null)
						builder.setTitle(title);

					TextView messageView = new TextView(mainActivity);
					messageView.setPadding(20, 20, 20, 20);
					messageView.setText(message);

					ScrollView scrollView = new ScrollView(mainActivity);
					scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 300));
					scrollView.addView(messageView);

					builder.setView(scrollView);

					if (positiveLabel != null)
					{
						builder.setPositiveButton(positiveLabel, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								dialog.dismiss();

								if (positiveObject != null)
									positiveObject.call("onClick", new Object[] {});
							}
						});
					}
				
					if (negativeLabel != null)
					{
						builder.setNegativeButton(negativeLabel, new DialogInterface.OnClickListener()
						{
							@Override
							public void onClick(DialogInterface dialog, int which)
							{
								dialog.dismiss();

								if (negativeObject != null)
									negativeObject.call("onClick", new Object[] {});
							}
						});
					}

					builder.setCancelable(false);
					builder.create().show();
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, e.toString());
				}
			}
		});
	}

		/**
		 * 显示原生对话框（Material Design 风格），支持正/负/中立按钮和 cancelable 标志。
		 */
		public static void showNativeAlertDialog(final String title, final String message, final String positiveLabel, final HaxeObject positiveObject, final String negativeLabel, final HaxeObject negativeObject, final String neutralLabel, final HaxeObject neutralObject, final boolean cancelable)
		{
			if (mainActivity == null)
			{
				Log.e(LOG_TAG, "showNativeAlertDialog: mainActivity is null");
				return;
			}

			mainActivity.runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						ContextThemeWrapper themedContext = new ContextThemeWrapper(mainActivity, com.google.android.material.R.style.Theme_MaterialComponents_DayNight_Dialog);
						MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(themedContext);

						if (title != null)
							builder.setTitle(title);

						if (message != null)
							builder.setMessage(message);

						if (positiveLabel != null)
						{
							builder.setPositiveButton(positiveLabel, new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
									if (positiveObject != null)
										positiveObject.call("onClick", new Object[] {});
								}
							});
						}

						if (negativeLabel != null)
						{
							builder.setNegativeButton(negativeLabel, new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
									if (negativeObject != null)
										negativeObject.call("onClick", new Object[] {});
								}
							});
						}

						if (neutralLabel != null)
						{
							builder.setNeutralButton(neutralLabel, new DialogInterface.OnClickListener()
							{
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									dialog.dismiss();
									if (neutralObject != null)
										neutralObject.call("onClick", new Object[] {});
								}
							});
						}

						builder.setCancelable(cancelable);
						builder.show();
					}
					catch (Exception e)
					{
						Log.e(LOG_TAG, e.toString());
					}
				}
			});
		}

	public static boolean installPackage(final String path)
	{
		if (path == null)
			return false;
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "installPackage: mainContext is null");
			return false;
		}

		try
		{
			boolean retVal = true;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
				retVal = mainContext.getPackageManager().canRequestPackageInstalls();

			File file = new File(path);

			if (!file.exists())
			{
				Log.e(LOG_TAG, "Attempted to install a application package from " + file.getAbsolutePath() + " but the file dosen't exist.");
				return retVal;
			}

			Uri contentUri = null;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			{
				try
				{
					contentUri = FileProvider.getUriForFile(mainContext, packageName + ".provider", file);
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, "FileProvider.getUriForFile failed: " + e.toString());
					return false;
				}
			}
			else
			{
				contentUri = Uri.fromFile(file);
			}

			Intent intent;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
			{
				intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
				intent.setData(contentUri);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
			}
			else
			{
				intent = new Intent(Intent.ACTION_VIEW);
				intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			}

			mainContext.startActivity(intent);

			return retVal;
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, e.toString());
			return false;
		}
	}

	public static void enableAppSecure()
	{
		if (mainActivity == null)
		{
			Log.e(LOG_TAG, "enableAppSecure: mainActivity is null");
			return;
		}

		mainActivity.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					mainActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, e.toString());
				}
			}
		});
	}

	public static void disableAppSecure()
	{
		if (mainActivity == null)
		{
			Log.e(LOG_TAG, "disableAppSecure: mainActivity is null");
			return;
		}

		mainActivity.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					mainActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, e.toString());
				}
			}
		});
	}

	public static void launchPackage(final String packageName, final int requestCode)
	{
		try
		{
			if (mainActivity == null)
			{
				Log.e(LOG_TAG, "launchPackage: mainActivity is null");
				return;
			}
			Intent launchIntent = mainActivity.getPackageManager().getLaunchIntentForPackage(packageName);
			if (launchIntent == null)
			{
				Log.e(LOG_TAG, "launchPackage: no launch intent for package: " + packageName);
				return;
			}
			mainActivity.startActivityForResult(launchIntent, requestCode);
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, e.toString());
		}
	}

	public static void requestSetting(final String setting, final int requestCode)
	{
		try
		{
			if (mainActivity == null)
			{
				Log.e(LOG_TAG, "requestSetting: mainActivity is null");
				return;
			}
			Intent intent = new Intent(setting);
			intent.setData(Uri.fromParts("package", packageName, null));
			mainActivity.startActivityForResult(intent, requestCode);
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, e.toString());
		}
	}

	public static boolean isRooted()
	{
		Process execute = null;
		try
		{
			execute = Runtime.getRuntime().exec(new String[] { "su", "-c", "id" });

			final Process p = execute;
			// Watchdog: never let "su" block the game forever (some ROMs show a
			// superuser prompt that nobody answers, or "su" waits for input).
			Thread watchdog = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						Thread.sleep(2000);
						p.destroy();
					}
					catch (Exception ignored) {}
				}
			});
			watchdog.setDaemon(true);
			watchdog.start();

			BufferedReader reader = new BufferedReader(new InputStreamReader(execute.getInputStream()));
			String line = reader.readLine();

			if (line != null && line.contains("uid=0"))
				return true;
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, e.toString());
		}
		finally
		{
			if (execute != null)
			{
				try { execute.destroy(); } catch (Exception ignored) {}
			}
		}

		return false;
	}

	public static boolean isDolbyAtmos()
	{
		try
		{
			MediaFormat format = new MediaFormat();

			format.setString(MediaFormat.KEY_MIME, "audio/eac3-joc"); // or "audio/ac4"

			// [API 29+] MediaCodecList.ALL_CODECS is deprecated, use REGULAR_CODECS
			// Note: use raw int 29 because compileSdk is 28 (Build.VERSION_CODES.Q = 29)
			int codecFlags = (Build.VERSION.SDK_INT >= 29)
				? MediaCodecList.REGULAR_CODECS
				: MediaCodecList.ALL_CODECS;

			MediaCodecList codecList = new MediaCodecList(codecFlags);

			if (codecList.findDecoderForFormat(format) != null)
				return true;
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, e.toString());
		}

		return false;
	}

	public static void showNotification(final String title, final String message, final String channelID, final String channelName, final int ID)
	{
		if (mainActivity == null)
		{
			Log.e(LOG_TAG, "showNotification: mainActivity is null");
			return;
		}

		mainActivity.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					if (mainContext == null)
					{
						Log.e(LOG_TAG, "showNotification: mainContext is null");
						return;
					}
					NotificationManager notificationManager = (NotificationManager) mainContext.getSystemService(Context.NOTIFICATION_SERVICE);

					String finalChannelID = (channelID != null && channelID.length() > 0) ? channelID : (packageName + ".default_channel");
					String finalChannelName = (channelName != null && channelName.length() > 0) ? channelName : "Notifications";

					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
					{
						try
						{
							NotificationChannel channel = new NotificationChannel(finalChannelID, finalChannelName, NotificationManager.IMPORTANCE_DEFAULT);
							notificationManager.createNotificationChannel(channel);
						}
						catch (Exception e)
						{
							Log.e(LOG_TAG, "createNotificationChannel failed: " + e.toString());
						}
					}

					Notification.Builder builder;
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
						builder = new Notification.Builder(mainContext, finalChannelID);
					else
						builder = new Notification.Builder(mainContext);

					builder.setAutoCancel(true);
					builder.setContentTitle(title);
					builder.setContentText(message);
					builder.setDefaults(Notification.DEFAULT_ALL);
					builder.setSmallIcon(android.R.drawable.ic_dialog_info);
					builder.setWhen(System.currentTimeMillis());
					notificationManager.notify(ID, builder.build());
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, e.toString());
				}
			}
		});
	}

	public static File getFilesDir()
	{
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "getFilesDir: mainContext is null");
			return null;
		}
		return mainContext.getFilesDir();
	}

	public static File getExternalFilesDir(final String type)
	{
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "getExternalFilesDir: mainContext is null");
			return null;
		}
		return mainContext.getExternalFilesDir(type);
	}

	public static File getCacheDir()
	{
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "getCacheDir: mainContext is null");
			return null;
		}
		return mainContext.getCacheDir();
	}

	public static File getCodeCacheDir()
	{
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "getCodeCacheDir: mainContext is null");
			return null;
		}
		return mainContext.getCodeCacheDir();
	}

	public static File getNoBackupFilesDir()
	{
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "getNoBackupFilesDir: mainContext is null");
			return null;
		}
		return mainContext.getNoBackupFilesDir();
	}

	public static File getExternalCacheDir()
	{
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "getExternalCacheDir: mainContext is null");
			return null;
		}
		return mainContext.getExternalCacheDir();
	}

	public static File getObbDir()
	{
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "getObbDir: mainContext is null");
			return null;
		}
		return mainContext.getObbDir();
	}

	public static BatteryManager getBatteryManager()
	{
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "getBatteryManager: mainContext is null");
			return null;
		}
		return (BatteryManager) mainContext.getSystemService(Context.BATTERY_SERVICE);
	}

	// --- Storage Access Framework helpers ---

	/**
	 * 打开系统文件选择器（SAF），选择已有文档。
	 * mimeTypesCsv 例如: "image/*,application/pdf"，为空则使用 ""。
	 */
	public static void openDocument(final String mimeTypesCsv, final int requestCode)
	{
		if (mainActivity == null)
		{
			Log.e(LOG_TAG, "openDocument: mainActivity is null");
			return;
		}

		mainActivity.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
					intent.addCategory(Intent.CATEGORY_OPENABLE);
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

					if (mimeTypesCsv == null || mimeTypesCsv.length() == 0)
					{
						intent.setType("*/*");
					}
					else
					{
						String[] types = mimeTypesCsv.split(",");
						if (types.length == 1)
							intent.setType(types[0].trim());
						else
						{
							intent.setType("*/*");
							intent.putExtra(Intent.EXTRA_MIME_TYPES, types);
						}
					}

					mainActivity.startActivityForResult(intent, requestCode);
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, e.toString());
				}
			}
		});
	}

	/**
	 * 调用系统“另存为”对话（SAF），创建/保存新文档。
	 * mimeType 可为 null 使用 ""，suggestedFileName 可为 null。
	 */
	public static void createDocument(final String mimeType, final String suggestedFileName, final int requestCode)
	{
		if (mainActivity == null)
		{
			Log.e(LOG_TAG, "createDocument: mainActivity is null");
			return;
		}

		mainActivity.runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
					intent.addCategory(Intent.CATEGORY_OPENABLE);
					intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
					intent.setType((mimeType != null && mimeType.length() > 0) ? mimeType : "*/*");
					if (suggestedFileName != null)
						intent.putExtra(Intent.EXTRA_TITLE, suggestedFileName);
					mainActivity.startActivityForResult(intent, requestCode);
				}
				catch (Exception e)
				{
					Log.e(LOG_TAG, e.toString());
				}
			}
		});
	}

	/**
	 * 从 content:// URI 读取文本内容（UTF-8）。失败返回 null。
	 */
	public static String readTextFromUri(final String uriString)
	{
		if (uriString == null) return null;
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "readTextFromUri: mainContext is null");
			return null;
		}

		Uri uri = Uri.parse(uriString);
		try (InputStream is = mainContext.getContentResolver().openInputStream(uri);
			 ByteArrayOutputStream baos = new ByteArrayOutputStream())
		{
			if (is == null) return null;
			byte[] buffer = new byte[4096];
			int read;
			while ((read = is.read(buffer)) != -1)
				baos.write(buffer, 0, read);
			return new String(baos.toByteArray(), StandardCharsets.UTF_8);
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, e.toString());
			return null;
		}
	}

	/**
	 * Copies a content:// URI (or any readable URI) into a local file.
	 * Returns the destination path on success, null on failure.
	 *
	 * Needed because Haxe's sys.io.File cannot open content:// URIs, so
	 * chart-converter code (File.read / File.getContent / zip Reader) must
	 * first copy the picked file into a plain filesystem path.
	 */
	public static String copyUriToFile(final String uriString, final String destPath)
	{
		if (uriString == null || destPath == null) return null;
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "copyUriToFile: mainContext is null");
			return null;
		}

		InputStream in = null;
		FileOutputStream out = null;
		try
		{
			Uri uri = Uri.parse(uriString);

			// content:// URIs rarely carry the extension, but the converter
			// detects .osz/.mcz/.osu/.mc by name - recover the original file
			// name from the ContentResolver and append it to the temp path.
			String finalDest = destPath;
			if (finalDest.indexOf('.') < 0)
			{
				String displayName = queryDisplayName(uri);
				if (displayName != null && displayName.length() > 0)
				{
					int slash = displayName.lastIndexOf('/');
					if (slash >= 0) displayName = displayName.substring(slash + 1);
					if (displayName.indexOf('.') >= 0)
						finalDest = destPath + "_" + displayName;
				}
			}

			// Some providers don't expose DISPLAY_NAME. Detect the file type
			// by content instead: a zip archive starts with the "PK" magic
			// bytes, so make sure chart packages (.osz/.mcz) keep a proper
			// extension - otherwise the caller treats them as text and JSON
			// parsing fails with "Invalid char 80" (0x50 = 'P').
			String finalLower = finalDest.toLowerCase();
			boolean hasChartExt = finalLower.endsWith(".osz") || finalLower.endsWith(".mcz")
				|| finalLower.endsWith(".osu") || finalLower.endsWith(".mc");
			if (!hasChartExt)
			{
				try
				{
					java.io.DataInputStream probe = new java.io.DataInputStream(
						mainContext.getContentResolver().openInputStream(uri));
					byte[] magic = new byte[4];
					int got = probe.read(magic);
					probe.close();
					if (got >= 2 && magic[0] == 'P' && magic[1] == 'K')
						finalDest = destPath + ".osz";
				}
				catch (Exception ignored) {}
			}

			in = mainContext.getContentResolver().openInputStream(uri);
			if (in == null)
			{
				Log.e(LOG_TAG, "copyUriToFile: cannot open " + uriString);
				return null;
			}

			File dest = new File(finalDest);
			File parent = dest.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs())
			{
				Log.e(LOG_TAG, "copyUriToFile: cannot create " + parent.getAbsolutePath());
				return null;
			}

			out = new FileOutputStream(dest);
			byte[] buffer = new byte[64 * 1024];
			int read;
			while ((read = in.read(buffer)) != -1)
				out.write(buffer, 0, read);
			out.flush();
			out.close();
			out = null;
			return dest.getAbsolutePath();
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, "copyUriToFile: " + e.toString());
			return null;
		}
		finally
		{
			try { if (in != null) in.close(); } catch (Exception ignored) {}
			try { if (out != null) out.close(); } catch (Exception ignored) {}
		}
	}

	/** Best-effort query of a content URI's original display name. */
	private static String queryDisplayName(Uri uri)
	{
		try
		{
			if (mainContext == null) return null;
			android.database.Cursor cursor = mainContext.getContentResolver().query(
				uri, new String[] { android.provider.OpenableColumns.DISPLAY_NAME }, null, null, null);
			if (cursor != null)
			{
				try
				{
					if (cursor.moveToFirst())
					{
						int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
						if (idx >= 0)
							return cursor.getString(idx);
					}
				}
				finally
				{
					cursor.close();
				}
			}
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, "queryDisplayName: " + e.toString());
		}
		return null;
	}

	/**
	 * 将文本（UTF-8）写入 content:// URI，覆盖已有内容。返回是否成功。
	 */
	public static boolean writeTextToUri(final String uriString, final String text)
	{
		if (uriString == null) return false;
		if (mainContext == null)
		{
			Log.e(LOG_TAG, "writeTextToUri: mainContext is null");
			return false;
		}
		Uri uri = Uri.parse(uriString);
		try (OutputStream os = mainContext.getContentResolver().openOutputStream(uri))
		{
			if (os == null) return false;
			byte[] bytes = (text != null) ? text.getBytes(StandardCharsets.UTF_8) : new byte[0];
			os.write(bytes);
			os.flush();
			return true;
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, e.toString());
			return false;
		}
	}

	/**
	 * 将持久化 URI 权限请求应用到指定 URI（如果系统允许）。
	 */
	public static void persistUriPermission(final String uriString)
	{
		if (uriString == null) return;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
		{
			try
			{
				Uri uri = Uri.parse(uriString);
				if (mainContext == null)
				{
					Log.e(LOG_TAG, "persistUriPermission: mainContext is null");
					return;
				}
				final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
				mainContext.getContentResolver().takePersistableUriPermission(uri, takeFlags);
			}
			catch (Exception e)
			{
				Log.e(LOG_TAG, e.toString());
			}
		}
	}

	// -------------------------------------------------------------------------
	// Native asset extraction engine
	//
	// The old approach copied every bundled asset from Haxe, one file at a time,
	// reading each file fully into memory (OOM on low-end devices) and writing
	// non-atomically (a kill mid-copy left corrupted files that were never
	// repaired). This engine streams from the APK AssetManager straight to disk
	// with a bounded buffer, writes atomically (.tmp + rename), resumes cleanly
	// after crashes, never overwrites user mods and reports progress back to Haxe.
	// -------------------------------------------------------------------------

	public static final String EXTRACT_TMP_SUFFIX = ".tmp_extract";

	private static ZipFile apkZip;

	/**
	 * Counts bundled assets (under the given APK asset roots, e.g. "assets,mods")
	 * that still need to be copied to {@code destRoot}. Synchronous.
	 */
	public static int countMissingAssets(final String rootsCsv, final String destRoot)
	{
		if (mainContext == null || assetManager == null || destRoot == null)
		{
			Log.e(LOG_TAG, "countMissingAssets: context/assetManager/destRoot is null");
			return 0;
		}

		final String[] roots = parseAssetRoots(rootsCsv);
		final File destDir = new File(destRoot);
		final int[] missing = new int[] { 0 };

		try
		{
			walkApkAssets(new AssetFileConsumer()
			{
				@Override
				public void accept(String destRel, String apkAssetPath)
				{
					if (!matchesAssetRoots(destRel, roots))
						return;
					if (needsExtraction(destRel, apkAssetPath, new File(destDir, destRel)))
						missing[0]++;
				}
			});
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, "countMissingAssets: " + e.toString());
		}

		return missing[0];
	}

	/**
	 * Extracts bundled assets from the APK to {@code destRoot} on a background
	 * thread. Progress and completion are reported through {@code listener} on
	 * the main thread.
	 */
	public static void extractAssets(final String rootsCsv, final String destRoot, final HaxeObject listener)
	{
		if (mainContext == null || assetManager == null || destRoot == null)
		{
			postExtractComplete(listener, buildFailureOnlyJson("Android context or AssetManager is not available."));
			return;
		}

		final String[] roots = parseAssetRoots(rootsCsv);
		final File destDir = new File(destRoot);

		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				Thread.currentThread().setName("asset-extractor");

				final List<String> failures = new ArrayList<String>();
				int extracted = 0;
				final int[] skippedCount = new int[] { 0 };
				int total = 0;

				try
				{
					final List<String> missing = new ArrayList<String>();
					walkApkAssets(new AssetFileConsumer()
					{
						@Override
						public void accept(String destRel, String apkAssetPath)
						{
							if (!matchesAssetRoots(destRel, roots))
								return;
							if (needsExtraction(destRel, apkAssetPath, new File(destDir, destRel)))
								missing.add(destRel);
							else
								skippedCount[0]++;
						}
					});

					total = missing.size();
					int done = 0;
					for (final String destRel : missing)
					{
						done++;
						try
						{
							copyAssetStream(destRel, new File(destDir, destRel));
							extracted++;
						}
						catch (Exception e)
						{
							failures.add(destRel + "\t" + String.valueOf(e.getMessage()));
						}

						if (listener != null && (done == total || done % 20 == 0))
							postExtractProgress(listener, destRel, done, total);
					}
				}
				catch (Throwable t)
				{
					failures.add("__overall__\t" + String.valueOf(t));
				}

				postExtractComplete(listener, buildResultJson(total, extracted, skippedCount[0], failures));
			}
		}).start();
	}

	/**
	 * @return true if the directory exists and a probe file could be written and
	 *         removed. Used to verify a storage location is really usable before
	 *         committing to it.
	 */
	public static boolean isDirectoryWritable(final String dirPath)
	{
		if (dirPath == null || dirPath.length() == 0)
			return false;

		File dir = new File(dirPath);
		File probe = null;
		try
		{
			if (!dir.exists() && !dir.mkdirs())
				return false;
			if (!dir.isDirectory())
				return false;

			probe = File.createTempFile(".write_probe_", ".tmp", dir);
			FileOutputStream fos = new FileOutputStream(probe);
			try
			{
				fos.write('x');
				fos.flush();
			}
			finally
			{
				try { fos.close(); } catch (Exception ignored) {}
			}
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
		finally
		{
			if (probe != null)
				try { probe.delete(); } catch (Exception ignored) {}
		}
	}

	private interface AssetFileConsumer
	{
		void accept(String destRel, String apkAssetPath) throws Exception;
	}

	private static String[] parseAssetRoots(String csv)
	{
		if (csv == null)
			return new String[0];

		List<String> roots = new ArrayList<String>();
		for (String part : csv.split(","))
		{
			String p = part.trim();
			while (p.startsWith("/")) p = p.substring(1);
			while (p.endsWith("/")) p = p.substring(0, p.length() - 1);
			if (p.length() > 0 && !roots.contains(p))
				roots.add(p);
		}
		return roots.toArray(new String[roots.size()]);
	}

	/**
	 * Enumerate every OpenFL asset in the APK. Android stores all OpenFL assets
	 * under the zip's "assets/" directory. AssetManager paths are relative to
	 * that directory, so an OpenFL path like "assets/images/x.png" is also the
	 * AssetManager path, and the zip entry is "assets/assets/images/x.png".
	 */
	private static void walkApkAssets(AssetFileConsumer consumer) throws Exception
	{
		walkAssets("", consumer);
	}

	private static void walkAssets(String path, AssetFileConsumer consumer) throws Exception
	{
		// Iterative DFS instead of recursion: asset trees in big modded APKs
		// can be deep enough to overflow the stack on some Android versions.
		java.util.ArrayDeque<String> stack = new java.util.ArrayDeque<String>();
		stack.push(path);

		while (!stack.isEmpty())
		{
			String current = stack.pop();
			String[] entries;
			try
			{
				entries = assetManager.list(current);
			}
			catch (Exception e)
			{
				continue; // unreadable directory - skip instead of crashing
			}
			if (entries == null)
				continue;

			for (String entry : entries)
			{
				String child = current.length() == 0 ? entry : current + "/" + entry;

				// Embedded assets are loaded from the binary/cache directly and
				// must NOT be extracted to the filesystem.
				if (child.equals("assets/embed") || child.startsWith("assets/embed/"))
					continue;

				if (isAssetDirectory(child))
				{
					// Directories carrying an ignore.txt marker are skipped.
					if (assetExists(child + "/ignore.txt"))
						continue;
					stack.push(child);
				}
				else if (assetExists(child))
				{
					String destRel = child;
					if (destRel.startsWith("flixel/") || destRel.startsWith("manifest/"))
						continue;
					consumer.accept(destRel, child);
				}
			}
		}
	}

	private static boolean matchesAssetRoots(String destRel, String[] roots)
	{
		for (String root : roots)
		{
			if (destRel.equals(root) || destRel.startsWith(root + "/"))
				return true;
		}
		return false;
	}

	private static boolean isAssetDirectory(String path)
	{
		try
		{
			String[] entries = assetManager.list(path);
			return entries != null && entries.length > 0;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private static boolean assetExists(String path)
	{
		InputStream is = null;
		try
		{
			is = assetManager.open(path);
			return is != null;
		}
		catch (IOException e)
		{
			return false;
		}
		finally
		{
			if (is != null)
				try { is.close(); } catch (Exception ignored) {}
		}
	}

	/**
	 * Decide whether an asset must be (re)extracted:
	 * - missing on disk -> yes
	 * - under mods/ -> never overwrite existing user content
	 * - under assets/ -> re-extract when size differs from the APK entry, so
	 *   updated builds replace stale files while identical files are skipped.
	 */
	private static boolean needsExtraction(String destRel, String apkAssetPath, File dest)
	{
		if (!dest.exists())
			return true;
		if (destRel.startsWith("mods/"))
			return false;

		long expected = apkEntrySize(apkAssetPath);
		if (expected < 0)
			return false; // cannot verify -> keep the existing file
		return dest.length() != expected;
	}

	private static ZipFile getApkZip()
	{
		if (apkZip == null)
		{
			try
			{
				apkZip = new ZipFile(mainContext.getApplicationInfo().sourceDir);
			}
			catch (Exception e)
			{
				Log.e(LOG_TAG, "getApkZip: " + e.toString());
			}
		}
		return apkZip;
	}

	private static long apkEntrySize(String assetManagerPath)
	{
		try
		{
			ZipFile zip = getApkZip();
			if (zip == null)
				return -1;
			// AssetManager paths are relative to the zip's "assets/" directory.
			ZipEntry entry = zip.getEntry("assets/" + assetManagerPath);
			return entry == null ? -1 : entry.getSize();
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	/** Stream an asset to disk with a bounded buffer + atomic rename. */
	private static void copyAssetStream(String assetPath, File dest) throws Exception
	{
		File parent = dest.getParentFile();
		if (parent != null && !parent.exists() && !parent.mkdirs())
			throw new IOException("Cannot create directory: " + parent.getAbsolutePath());

		File tmp = new File(dest.getAbsolutePath() + EXTRACT_TMP_SUFFIX);
		if (tmp.exists())
			try { tmp.delete(); } catch (Exception ignored) {}

		InputStream in = null;
		FileOutputStream out = null;
		try
		{
			in = assetManager.open(assetPath);
			out = new FileOutputStream(tmp);
			byte[] buffer = new byte[64 * 1024];
			int read;
			while ((read = in.read(buffer)) != -1)
				out.write(buffer, 0, read);
			out.flush();
			out.close();
			out = null;

			if (!tmp.renameTo(dest))
			{
				// Some filesystems refuse rename over an existing target.
				if (dest.exists())
					dest.delete();
				if (!tmp.renameTo(dest))
					throw new IOException("Cannot finalize " + dest.getAbsolutePath());
			}
		}
		finally
		{
			try { if (in != null) in.close(); } catch (Exception ignored) {}
			try { if (out != null) out.close(); } catch (Exception ignored) {}
			if (tmp.exists())
				try { tmp.delete(); } catch (Exception ignored) {}
		}
	}

	private static void postExtractProgress(final HaxeObject listener, final String file, final int done, final int total)
	{
		postToMainThread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					listener.call("onProgress", new Object[] { file == null ? "" : file, done, total });
				}
				catch (Throwable t)
				{
					Log.e(LOG_TAG, "onProgress callback failed: " + t.toString());
				}
			}
		});
	}

	private static void postExtractComplete(final HaxeObject listener, final String json)
	{
		postToMainThread(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					listener.call("onComplete", new Object[] { json });
				}
				catch (Throwable t)
				{
					Log.e(LOG_TAG, "onComplete callback failed: " + t.toString());
				}
			}
		});
	}

	private static void postToMainThread(Runnable r)
	{
		if (callbackHandler != null)
			callbackHandler.post(r);
		else if (mainActivity != null)
			mainActivity.runOnUiThread(r);
		else
			r.run();
	}

	private static String buildResultJson(int total, int extracted, int skipped, List<String> failures)
	{
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("total", total);
		result.put("extracted", extracted);
		result.put("skipped", skipped);
		result.put("failed", failures.size());

		List<Map<String, Object>> failureList = new ArrayList<Map<String, Object>>();
		for (String f : failures)
		{
			Map<String, Object> item = new LinkedHashMap<String, Object>();
			int tab = f.indexOf('\t');
			if (tab >= 0)
			{
				item.put("file", f.substring(0, tab));
				item.put("error", f.substring(tab + 1));
			}
			else
			{
				item.put("file", f);
				item.put("error", "");
			}
			failureList.add(item);
		}
		result.put("failures", failureList);

		if (gson == null)
			gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
		return gson.toJson(result);
	}

	private static String buildFailureOnlyJson(String message)
	{
		List<String> failures = new ArrayList<String>();
		failures.add("__overall__\t" + message);
		return buildResultJson(0, 0, 0, failures);
	}


	/**
	 * Called when an activity you launched exits, giving you the requestCode 
	 * you started it with, the resultCode it returned, and any additional data 
	 * from it.
	 */
	@Override
	public boolean onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (cbObject != null)
		{
			HashMap<String, Object> content = new HashMap<String, Object>();

			content.put("requestCode", requestCode);
			content.put("resultCode", resultCode);

			if (data != null && data.getData() != null)
			{
				Uri uri = data.getData();
				content.put("uri", uri.toString());

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				{
					try
					{
						final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
						if (mainContext != null)
						{
							mainContext.getContentResolver().takePersistableUriPermission(uri, takeFlags);
						}
						else
						{
							Log.e(LOG_TAG, "onActivityResult: mainContext is null, cannot take persistable permission");
						}
					}
					catch (Exception e)
					{
						Log.e(LOG_TAG, e.toString());
					}
				}
			}

			if (gson == null)
				gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

			cbObject.call("onActivityResult", new Object[] {
				gson.toJson(content)
			});
		}

		return true;
	}

	/**
	 * Callback for the result from requesting permissions.
	 */
	@Override
	public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
	{
		if (cbObject != null)
		{
			HashMap<String, Object> content = new HashMap<String, Object>();

			content.put("requestCode", requestCode);
			content.put("permissions", permissions);
			content.put("grantResults", grantResults);

			if (gson == null)
				gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

			cbObject.call("onRequestPermissionsResult", new Object[] {
				gson.toJson(content)
			});
		}

		return true;
	}
}
