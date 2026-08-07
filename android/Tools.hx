package android;

#if (!android && !native)
#error 'extension-androidtools is not supported on your current platform'
#end
import android.Permissions;
import haxe.io.Path;
import lime.app.Event;
import lime.system.JNI;
import lime.utils.Log;

class Tools
{
	/**
	 * Prompt the user to install a specific APK file.
	 */
	public static function installPackage(path:String):Void
	{
		if (!installPackage_jni(path))
			Log.warn('"REQUEST_INSTALL_PACKAGES" permission and "Install apps from external sources" setting must be granted to this app in order to install a ${Path.extension(path).toUpperCase()} file.');
	}

	/**
	 * Adds the security flag to application's window.
	 */
	public static inline function enableAppSecure():Void
	{
		enableAppSecure_jni();
	}

	/**
	 * Clears the security flag from application's window.
	 */
	public static inline function disableAppSecure():Void
	{
		disableAppSecure_jni();
	}

	/**
	 * Launches a app by the `packageName`.
	 */
	public static inline function launchPackage(packageName:String, requestCode:Int = 1):Void
	{
		launchPackage_jni(packageName, requestCode);
	}

	/**
	 * Launches a app by the `packageName`.
	 */
	public static function showAlertDialog(title:String, message:String, ?positiveButton:ButtonData, ?negativeButton:ButtonData):Void
	{
		if (positiveButton == null)
			positiveButton = {name: null, func: null};

		if (negativeButton == null)
			negativeButton = {name: null, func: null};

		showAlertDialog_jni(title, message, positiveButton.name, new ButtonListener(positiveButton.func), negativeButton.name,
			new ButtonListener(negativeButton.func));
	}

	/**
	 * 显示原生对话框（AndroidX `AlertDialog`），支持正/负/中立按钮和 cancelable 标志。
	 */
	public static function showNativeAlertDialog(title:String, message:String, ?positiveButton:ButtonData, ?negativeButton:ButtonData, ?neutralButton:ButtonData, ?cancelable:Bool = true):Void
	{
		if (positiveButton == null)
			positiveButton = {name: null, func: null};

		if (negativeButton == null)
			negativeButton = {name: null, func: null};

		if (neutralButton == null)
			neutralButton = {name: null, func: null};

		showNativeAlertDialog_jni(title, message, positiveButton.name, new ButtonListener(positiveButton.func),
			negativeButton.name, new ButtonListener(negativeButton.func),
			neutralButton.name, new ButtonListener(neutralButton.func),
			cancelable);
	}

	/**
	 * @return `true` If the device have root.
	 */
	public static inline function isRooted():Bool
	{
		return isRooted_jni();
	}

	/**
	 * @return `true` If the device has Dolby Atmos.
	 */
	public static inline function isDolbyAtmos():Bool
	{
		return isDolbyAtmos_jni();
	}

	/**
	 * Shows a minimal notification with a title and message.
	 */
	public static inline function showNotification(title:String, message:String, ?channelID:String = 'unknown_channel',
			?channelName:String = 'Unknown Channel', ?ID:Int = 1):Void
	{
		showNotification_jni(title, message, channelID, channelName, ID);
	}

	/**
	 * Sets Activity's Title by the `title`.
	 */
	public static inline function setActivityTitle(title:String):Bool
	{
		return setActivityTitle_jni(title);
	}

	/**
	 * Minimizes app's window.
	 */
	public static inline function minimizeWindow():Void
	{
		minimizeWindow_jni();
	}

	/**
	 * @return whether the device is running Android TV.
	 */
	public static inline function isAndroidTV():Bool
	{
		return isAndroidTV_jni();
	}

	/**
	 * @return whether the device is a Tablet.
	 */
	public static inline function isTablet():Bool
	{
		return isTablet_jni();
	}

	/**
	 * @return whether the device is a ChromeBook.
	 */
	public static inline function isChromebook():Bool
	{
		return isChromebook_jni();
	}

	/**
	 * @return whether the device is running in Dex Mode.
	 */
	public static inline function isDeXMode():Bool
	{
		return isDexMode_jni();
	}

	// --- Storage Access Framework (SAF) Haxe wrappers ---


	public static inline function openDocument(mimeTypesCsv:String, ?requestCode:Int = REQUEST_CODE_OPEN_DOCUMENT):Void
	{
		openDocument_jni(mimeTypesCsv, requestCode);
	}

	/**
	 * Request system create document (save/as). mimeType may be null and suggestedFileName may be null.
	 */
	public static inline function createDocument(mimeType:String, suggestedFileName:String, ?requestCode:Int = REQUEST_CODE_CREATE_DOCUMENT):Void
	{
		createDocument_jni(mimeType, suggestedFileName, requestCode);
	}

	/**
	 * Read text (UTF-8) from a content:// URI returned by SAF. Returns null on failure.
	 */
	public static inline function readTextFromUri(uri:String):String
	{
		return readTextFromUri_jni(uri);
	}

	/**
	 * Copies a content:// URI (or any readable URI) into a local file.
	 * Returns the destination path on success, null on failure.
	 */
	public static inline function copyUriToFile(uri:String, destPath:String):String
	{
		return copyUriToFile_jni(uri, destPath);
	}

	/**
	 * Write text (UTF-8) to a content:// URI returned by SAF. Returns true on success.
	 */
	public static inline function writeTextToUri(uri:String, text:String):Bool
	{
		return writeTextToUri_jni(uri, text);
	}

	/**
	 * Persist URI permission for long-term access (if supported on device).
	 */
	public static inline function persistUriPermission(uri:String):Void
	{
		persistUriPermission_jni(uri);
	}

	// --- Native asset extraction (streamed from the APK, atomic writes) ---

	/**
	 * Counts bundled assets (under the given APK asset roots, e.g. `['assets', 'mods']`)
	 * that still need to be copied to `destDir`.
	 *
	 * Files that already exist are skipped: bundled `mods/` are never overwritten,
	 * while bundled `assets/` are re-extracted when their size differs from the APK.
	 */
	public static inline function countMissingAssets(roots:Array<String>, destDir:String):Int
	{
		return countMissingAssets_jni(roots.join(','), destDir);
	}

	/**
	 * Extracts bundled assets from the APK to `destDir` on a background thread.
	 *
	 * Progress and completion are reported on the main thread through `listener`:
	 * - `onProgress(file, done, total)`
	 * - `onComplete(resultJson)` with `total/extracted/skipped/failed/failures`.
	 */
	public static function extractAssets(roots:Array<String>, destDir:String, listener:ExtractionListener):Void
	{
		extractAssets_jni(roots.join(','), destDir, listener);
	}

	/**
	 * @return `true` if the directory exists and a probe file could be written
	 * and removed, i.e. the location is actually usable as game storage.
	 */
	public static inline function isDirectoryWritable(dirPath:String):Bool
	{
		return isDirectoryWritable_jni(dirPath);
	}

	// Request code constants
	public static inline var REQUEST_CODE_OPEN_DOCUMENT:Int = 1001;
	public static inline var REQUEST_CODE_CREATE_DOCUMENT:Int = 1002;

	@:noCompletion
	private static var installPackage_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'installPackage', '(Ljava/lang/String;)Z');

	@:noCompletion
	private static var enableAppSecure_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'enableAppSecure', '()V');

	@:noCompletion
	private static var disableAppSecure_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'disableAppSecure', '()V');

	@:noCompletion
	private static var launchPackage_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'launchPackage', '(Ljava/lang/String;I)V');

	@:noCompletion
	private static var showAlertDialog_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'showAlertDialog',
		'(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;)V');

	@:noCompletion
	private static var showNativeAlertDialog_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'showNativeAlertDialog', '(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;Z)V');

	@:noCompletion
	private static var isRooted_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'isRooted', '()Z');

	@:noCompletion
	private static var isDolbyAtmos_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'isDolbyAtmos', '()Z');

	@:noCompletion
	private static var showNotification_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'showNotification',
		'(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V');

	@:noCompletion
	private static var openDocument_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'openDocument', '(Ljava/lang/String;I)V');

	@:noCompletion
	private static var createDocument_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'createDocument', '(Ljava/lang/String;Ljava/lang/String;I)V');

	@:noCompletion
	private static var readTextFromUri_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'readTextFromUri', '(Ljava/lang/String;)Ljava/lang/String;');

	private static var copyUriToFile_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'copyUriToFile', '(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;');

	@:noCompletion
	private static var writeTextToUri_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'writeTextToUri', '(Ljava/lang/String;Ljava/lang/String;)Z');

	@:noCompletion
	private static var persistUriPermission_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'persistUriPermission', '(Ljava/lang/String;)V');

	@:noCompletion
	private static var countMissingAssets_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'countMissingAssets', '(Ljava/lang/String;Ljava/lang/String;)I');

	@:noCompletion
	private static var extractAssets_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'extractAssets',
		'(Ljava/lang/String;Ljava/lang/String;Lorg/haxe/lime/HaxeObject;)V');

	@:noCompletion
	private static var isDirectoryWritable_jni:Dynamic = JNI.createStaticMethod('org/haxe/extension/Tools', 'isDirectoryWritable', '(Ljava/lang/String;)Z');

	@:noCompletion
	private static var setActivityTitle_jni:Dynamic = JNI.createStaticMethod('org/libsdl/app/SDLActivity', 'setActivityTitle', '(Ljava/lang/String;)Z');

	@:noCompletion
	private static var minimizeWindow_jni:Dynamic = JNI.createStaticMethod('org/libsdl/app/SDLActivity', 'minimizeWindow', '()V');

	@:noCompletion
	private static var isAndroidTV_jni:Dynamic = JNI.createStaticMethod('org/libsdl/app/SDLActivity', 'isAndroidTV', '()Z');

	@:noCompletion
	private static var isTablet_jni:Dynamic = JNI.createStaticMethod('org/libsdl/app/SDLActivity', 'isTablet', '()Z');

	@:noCompletion
	private static var isChromebook_jni:Dynamic = JNI.createStaticMethod('org/libsdl/app/SDLActivity', 'isChromebook', '()Z');

	@:noCompletion
	private static var isDexMode_jni:Dynamic = JNI.createStaticMethod('org/libsdl/app/SDLActivity', 'isDeXMode', '()Z');
}

@:noCompletion
private typedef ButtonData =
{
	name:String,
	func:Void->Void
}

@:noCompletion
private class ButtonListener #if (lime >= "8.0.0") implements JNISafety #end
{
	private var onClickEvent:Event<Void->Void> = new Event<Void->Void>();

	public function new(clickCallback:Void->Void):Void
	{
		if (clickCallback != null)
			onClickEvent.add(clickCallback);
	}

	#if (lime >= "8.0.0")
	@:runOnMainThread
	#end
	public function onClick():Void
	{
		onClickEvent.dispatch();
	}
}

/**
 * Callback bridge for native asset extraction. All methods are delivered on the
 * main thread (the Java side posts through the UI handler, and lime >= 8.0 adds
 * an extra `@:runOnMainThread` guarantee).
 */
class ExtractionListener #if (lime >= "8.0.0") implements JNISafety #end
{
	public var progressHandler:String->Int->Int->Void;
	public var completeHandler:String->Void;

	public function new(?progressHandler:String->Int->Int->Void, ?completeHandler:String->Void):Void
	{
		this.progressHandler = progressHandler;
		this.completeHandler = completeHandler;
	}

	#if (lime >= "8.0.0")
	@:runOnMainThread
	#end
	public function onProgress(file:String, done:Int, total:Int):Void
	{
		if (progressHandler != null)
			progressHandler(file, done, total);
	}

	#if (lime >= "8.0.0")
	@:runOnMainThread
	#end
	public function onComplete(result:String):Void
	{
		if (completeHandler != null)
			completeHandler(result);
	}
}
