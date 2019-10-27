package wei.mark.standout;

import android.app.AlertDialog;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import a98apps.recorderedge.BuildConfig;
import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.floating.FrameRateWindow;
import wei.mark.standout.ui.Window;

/**
 * Extend this class to easily create and manage floating StandOut windows.
 *
 * @author Mark Wei <markwei@gmail.com>
 *         <p>
 *         Contributors: Jason <github.com/jasonconnery>
 *             Modified by 98 Apps, Mestre
 */
public abstract class StandOutWindow extends Service
{
	private static final String TAG = "StandOutWindow";

	/**
	 * StandOut window id: You may use this sample id for your first window.
	 */
	public static final int DEFAULT_ID = 0;

	/**
	 * Special StandOut window id: You may NOT use this id for any windows.
	 */
	private static final int ONGOING_NOTIFICATION_ID = -1;

	/**
	 * Intent action: Show a new window corresponding to the id.
	 */
	private static final String ACTION_SHOW = "SHOW";


	private static final String ACTION_RESTORE = "RESTORE";

	/**
	 * Intent action: Close an existing window with an existing id.
	 */
	private static final String ACTION_CLOSE = "CLOSE";

	/**
	 * Intent action: Close all existing windows.
	 */
	private static final String ACTION_CLOSE_ALL = "CLOSE_ALL";


	public static void show(Context context,
							Class<? extends StandOutWindow> cls, int id) {
		context.startService(getShowIntent(context, cls, id));
	}
	public static void close(Context context,
							 Class<? extends StandOutWindow> cls, int id) {
		context.startService(getCloseIntent(context, cls, id));
	}
	private static Intent getCloseIntent(Context context,
										 Class<? extends StandOutWindow> cls, int id) {
		return new Intent(context, cls).putExtra("id", id).setAction(
				ACTION_CLOSE);
	}
	private static Intent getShowIntent(Context context,
										Class<? extends StandOutWindow> cls, int id) {
		boolean cached = sWindowCache.isCached(id, cls);
		String action = cached ? ACTION_RESTORE : ACTION_SHOW;
		Uri uri = cached ? Uri.parse("standout://" + cls + '/' + id) : null;
		return new Intent(context, cls).putExtra("id", id).setAction(action)
				.setData(uri);
	}

	// internal map of ids to shown/hidden views
	private static final WindowCache sWindowCache;

	// static constructors
	static {
		sWindowCache = new WindowCache();
	}

	// internal system services
	private WindowManager mWindowManager;


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		// intent should be created with
		// getShowIntent(), getHideIntent(), getCloseIntent()
		if (intent != null) {
			try
			{
				String action = intent.getAction();
				if(action != null)
				{
					int id = intent.getIntExtra("id", DEFAULT_ID);
					// this will interfere with getPersistentNotification()
					if (id == ONGOING_NOTIFICATION_ID) {
						throw new RuntimeException(
								"ID cannot equals StandOutWindow.ONGOING_NOTIFICATION_ID");
					}

					switch (action) {
						case ACTION_SHOW:
						case ACTION_RESTORE:
							show(id);
							break;
						case ACTION_CLOSE:
							close(id);
							break;
						case ACTION_CLOSE_ALL:
							closeAll();
							break;
					}
				}
				else
					stopService(new Intent(this, FrameRateWindow.class));
			}
			catch (final IllegalArgumentException i)
			{
				stopService(new Intent(this, FrameRateWindow.class));

				if (!Settings.canDrawOverlays(getApplicationContext()))
				{
					new Handler().post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
						}
					});
				}
				else
				{
					File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "logs");
					String path;
					boolean success = true;
					if (!folder.exists())
						success = folder.mkdir();

					if (success)
						path = folder.getAbsolutePath();
					else
						path = Environment.getExternalStorageDirectory().getAbsolutePath();

					final File logFile = new File(path,
							"log-" + new SimpleDateFormat(Constants.DEFAULT_FILE_FORMAT, Locale.US).format(new Date()) + ".txt");
					try {
						Runtime.getRuntime().exec(
								"logcat -f " + logFile.getAbsolutePath());
					} catch (IOException e) {
						new Handler().post(new Runnable() {
							@Override
							public void run() {
								Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.failed_get_log), Toast.LENGTH_SHORT).show();
							}
						});
					}
					new Handler().post(new Runnable() {
						@Override
						public void run() {
							final AlertDialog errorDialog = new AlertDialog.Builder(getApplicationContext().getApplicationContext()).create();
							errorDialog.setTitle("Ops!");
							if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
								Objects.requireNonNull(errorDialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
							else
								Objects.requireNonNull(errorDialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

							errorDialog.setMessage(getApplicationContext().getString(R.string.failed_report_message) + i.toString());
							errorDialog.setIcon(R.drawable.ic_error);
							errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.text_send),
									new DialogInterface.OnClickListener() {
										public void onClick(DialogInterface dialog, int whichButton) {
											Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
											emailIntent.setData(Uri.parse("mailto:98appshelp@gmail.com"));
											emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[Report][Screen Recorder Edge]");
											emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));
											emailIntent.putExtra(Intent.EXTRA_TEXT, getApplicationContext().getResources().getString(R.string.dont_remove_information_text) + "\n" + "--------------------------\n"
													+ Build.MANUFACTURER.toUpperCase() + "\n" + Build.MODEL + "\nAndroid: " + Build.VERSION.RELEASE + "\n" + "App Version: " + BuildConfig.VERSION_NAME + "\n\n" + "Log Report ---------------------------\n"+ Log.getStackTraceString(i));

											ComponentName emailApp = emailIntent.resolveActivity(getApplicationContext().getPackageManager());
											ComponentName unsupportedAction = ComponentName.unflattenFromString("com.android.fallback/.Fallback");
											if (emailApp != null && !emailApp.equals(unsupportedAction))
											{
												try
												{
													getApplicationContext().startActivity(Intent.createChooser(emailIntent, getApplicationContext().getString(R.string.send_email)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
												}
												catch (ActivityNotFoundException i) {
													new Handler().post(new Runnable() {
														@Override
														public void run() {
															Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.report_bug_error), Toast.LENGTH_LONG).show();
														}
													});
												}
											}
											else
											{
												new Handler().post(new Runnable() {
													@Override
													public void run() {
														Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.report_bug_error), Toast.LENGTH_LONG).show();
													}
												});
											}
										}
									});
							errorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									errorDialog.dismiss();
								}
							});
							errorDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
								@Override
								public void onCancel(DialogInterface dialog) {
									errorDialog.dismiss();
								}
							});
							errorDialog.show();
						}
					});
				}
			}

		}
		else
			Log.w(TAG, "Tried to onStartCommand() with a null intent.");

		// the service is started in foreground in show()
		// so we don't expect Android to kill this service
		return START_NOT_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// closes all windows
		closeAll();
	}



	public abstract void createAndAttachView(int id, FrameLayout frame);

	protected abstract void onFinishWindow();

	public abstract StandOutLayoutParams getParams(int id, Window window);

	private synchronized void show(int id) {
		// get the window corresponding to the id
		Window cachedWindow = getWindow(id);
		final Window window;

		// check cache first
		if (cachedWindow != null) {
			window = cachedWindow;
		} else {
			window = new Window(this, id);
		}

		// focus an already shown window
		if (window.visibility == Window.VISIBILITY_VISIBLE) {
			Log.d(TAG, "Window " + id + " is already shown.");
			return;
		}

		window.visibility = Window.VISIBILITY_VISIBLE;

		// get the params corresponding to the id
		StandOutLayoutParams params = window.getLayoutParams();

		// add the view to the window manager
		mWindowManager.addView(window, params);

		// add view to internal map
		sWindowCache.putCache(id, getClass(), window);
	}

	private synchronized void close(final int id) {
		// get the view corresponding to the id
		final Window window = getWindow(id);

		if (window == null) {
			stopService(new Intent(this, FrameRateWindow.class));
			return;
		}

		if (window.visibility == Window.VISIBILITY_TRANSITION) {
			return;
		}

		window.visibility = Window.VISIBILITY_TRANSITION;

		onFinishWindow();

		// remove the window from the window manager
		mWindowManager.removeView(window);
		window.visibility = Window.VISIBILITY_GONE;

		// remove view from internal map
		sWindowCache.removeCache(id,
				StandOutWindow.this.getClass());

		stopService(new Intent(this, FrameRateWindow.class));
	}
	private synchronized void closeAll() {

		// add ids to temporary set to avoid concurrent modification
		LinkedList<Integer> ids = new LinkedList<>(getExistingIds());
		// close each window
		for (int id : ids) {
			close(id);
		}
	}

	private Set<Integer> getExistingIds() {
		return sWindowCache.getCacheIds(getClass());
	}

	private Window getWindow(int id) {
		return sWindowCache.getCache(id, getClass());
	}

	/**
	 * LayoutParams specific to floating StandOut windows.
	 *
	 * @author Mark Wei <markwei@gmail.com>
	 */
	@SuppressWarnings("deprecation")
	public class StandOutLayoutParams extends WindowManager.LayoutParams {

		/**
		 * Special value for x position that represents the right of the screen.
		 */
		private static final int RIGHT = Integer.MAX_VALUE;
		/**
		 * Special value for y position that represents the bottom of the
		 * screen.
		 */
		private static final int BOTTOM = Integer.MAX_VALUE;
		/**
		 * Special value for x or y position that represents the center of the
		 * screen.
		 */
		public static final int CENTER = Integer.MIN_VALUE;
		/**
		 * Special value for x or y position which requests that the system
		 * determine the position.
		 */
		private static final int AUTO_POSITION = Integer.MIN_VALUE + 1;

		/**
		 * Optional constraints of the window.
		 */
		final int minWidth;
		final int minHeight;
		final int maxWidth;
		final int maxHeight;

		/**
		 * @param id The id of the window.
		 */
		private StandOutLayoutParams(int id)
		{
			super(200, 200, Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? TYPE_APPLICATION_OVERLAY : TYPE_PHONE,
					StandOutLayoutParams.FLAG_NOT_TOUCH_MODAL
							| StandOutLayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
					PixelFormat.TRANSLUCENT);

			setFocusFlag();

			x = getX(id, width);
			y = getY(id, height);

			gravity = Gravity.TOP | Gravity.START;

			minWidth = minHeight = 0;
			maxWidth = maxHeight = Integer.MAX_VALUE;
		}

		/**
		 * @param id The id of the window.
		 * @param w  The width of the window.
		 * @param h  The height of the window.
		 */
		private StandOutLayoutParams(int id, int w, int h) {
			this(id);
			width = w;
			height = h;
		}

		/**
		 * @param id   The id of the window.
		 * @param w    The width of the window.
		 * @param h    The height of the window.
		 * @param xpos The x position of the window.
		 * @param ypos The y position of the window.
		 */
		public StandOutLayoutParams(int id, int w, int h, int xpos, int ypos) {
			this(id, w, h);

			if (xpos != AUTO_POSITION) {
				x = xpos;
			}
			if (ypos != AUTO_POSITION) {
				y = ypos;
			}

			Display display = mWindowManager.getDefaultDisplay();
			int width = display.getWidth();
			int height = display.getHeight();

			if (x == RIGHT) {
				x = width - w;
			} else if (x == CENTER) {
				x = (width - w) / 2;
			}

			if (y == BOTTOM) {
				y = height - h;
			} else if (y == CENTER) {
				y = (height - h) / 2;
			}
		}

		// helper to create cascading windows
		private int getX(int id, int width) {
			Display display = mWindowManager.getDefaultDisplay();
			int displayWidth = display.getWidth();

			int types = sWindowCache.size();

			int initialX = 100 * types;
			int variableX = 100 * id;
			int rawX = initialX + variableX;

			return rawX % (displayWidth - width);
		}

		// helper to create cascading windows
		private int getY(int id, int height) {
			Display display = mWindowManager.getDefaultDisplay();
			int displayWidth = display.getWidth();
			int displayHeight = display.getHeight();

			int types = sWindowCache.size();

			int initialY = 100 * types;
			int variableY = x + 200 * (100 * id) / (displayWidth - width);

			int rawY = initialY + variableY;

			return rawY % (displayHeight - height);
		}

		private void setFocusFlag() {
			flags = flags | StandOutLayoutParams.FLAG_NOT_FOCUSABLE;
		}
	}
}