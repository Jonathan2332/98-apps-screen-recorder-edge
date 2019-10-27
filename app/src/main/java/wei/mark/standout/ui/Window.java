package wei.mark.standout.ui;

import java.util.LinkedList;
import java.util.Queue;

import a98apps.recorderedge.R;
import wei.mark.standout.StandOutWindow;
import wei.mark.standout.StandOutWindow.StandOutLayoutParams;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Special view that represents a floating window.
 * 
 * @author Mark Wei <markwei@gmail.com>
 *     Modified by 98 Apps, Mestre
 * 
 */
public class Window extends FrameLayout {
	public static final int VISIBILITY_GONE = 0;
	public static final int VISIBILITY_VISIBLE = 1;
	public static final int VISIBILITY_TRANSITION = 2;

    /**
	 * Id of the window.
	 */
    private int id;

	/**
	 * Whether the window is shown, hidden/closed, or in transition.
	 */
	public int visibility;

	/**
	 * Original params from {@link StandOutWindow#getParams(int, Window)}.
	 */
    private StandOutLayoutParams originalParams;

	private LayoutInflater mLayoutInflater;

	public Window(Context context) {
		super(context);
	}

	@SuppressLint("ClickableViewAccessibility")
	public Window(final StandOutWindow context, final int id) {
		super(context);
		context.setTheme(0);

		mLayoutInflater = LayoutInflater.from(context);

        this.originalParams = context.getParams(id, this);

		this.id = id;

		// create the window contents
		View content;
		FrameLayout body;

		content = getSystemDecorations();
		body = content.findViewById(R.id.body);

		addView(content);

		// attach the view corresponding to the id from the
		// implementation
		context.createAndAttachView(id, body);

		// make sure the implementation attached the view
		if (body.getChildCount() == 0) {
			throw new RuntimeException(
					"You must attach your view to the given frame in createAndAttachView()");
		}

		// implement StandOut specific workarounds
        fixCompatibility(body);

		// attach the existing tag from the frame to the window
		setTag(body.getTag());
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params) {
		if (params instanceof StandOutLayoutParams) {
			super.setLayoutParams(params);
		} else {
			throw new IllegalArgumentException(
					"Window"
							+ id
							+ ": LayoutParams must be an instance of StandOutLayoutParams.");
		}
	}

	@Override
	public StandOutLayoutParams getLayoutParams() {
		StandOutLayoutParams params = (StandOutLayoutParams) super
				.getLayoutParams();
		if (params == null) {
			params = originalParams;
		}
		return params;
	}

	@SuppressLint("InflateParams")
	private View getSystemDecorations() {
		return mLayoutInflater.inflate(
				R.layout.system_window_decorators, null);
	}


	/**
	 * Iterate through each View in the view hiearchy and implement StandOut
	 * specific compatibility workarounds.
	 * 
	 * <p>
	 * Currently, this method does the following:
	 * 
	 * <p>
	 * Nothing yet.
	 * 
	 * @param root
	 *            The root view hierarchy to iterate through and check.
	 */
    private void fixCompatibility(View root) {
		Queue<View> queue = new LinkedList<>();
		queue.add(root);

		View view;
		while ((view = queue.poll()) != null) {
			// do nothing yet

			// iterate through children
			if (view instanceof ViewGroup) {
				ViewGroup group = (ViewGroup) view;
				for (int i = 0; i < group.getChildCount(); i++) {
					queue.add(group.getChildAt(i));
				}
			}
		}
	}
}