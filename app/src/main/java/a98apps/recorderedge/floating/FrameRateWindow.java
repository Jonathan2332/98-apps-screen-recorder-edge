package a98apps.recorderedge.floating;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import a98apps.recorderedge.R;
import wei.mark.standout.StandOutWindow;
import wei.mark.standout.ui.Window;

//THIS CLASS CONTROL THE FPS OF RECORD USING ANIMATION IN BACKGROUND

public class FrameRateWindow extends StandOutWindow {
    private ObjectAnimator animator;

    @Override
    public void createAndAttachView(int id, FrameLayout frame) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.frame_rate_window, frame, true);

        ImageView view = frame.findViewById(R.id.animator);
        animator = ObjectAnimator.ofFloat(view, "rotation", 360f);
        animator.setDuration(600);
        animator.setRepeatCount(Animation.INFINITE);
        animator.setInterpolator(new TimeInterpolator() {
            @Override
            public float getInterpolation(float input) {
                return (int)(input * 60) / 60.0f;
            }
        });
        animator.start();
    }
    @Override
    protected void onFinishWindow() {
        if(animator != null)
        {
            animator.removeAllListeners();
            animator.cancel();
        }
    }

    @Override
    public StandOutLayoutParams getParams(int id, Window window) {
        return new StandOutLayoutParams(id, (int) getResources().getDimension(R.dimen.anim_width), (int) getResources().getDimension(R.dimen.anim_height),
                StandOutLayoutParams.CENTER, StandOutLayoutParams.CENTER);
    }
}
