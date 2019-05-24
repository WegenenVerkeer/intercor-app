package macq.intercor.helpers;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

public class OnViewGlobalLayoutListener implements ViewTreeObserver.OnGlobalLayoutListener {
    private float maxHeight;
    private View view;

    public OnViewGlobalLayoutListener(View view, float maxHeight) {
        this.maxHeight = maxHeight;
        this.view = view;
    }

    @Override
    public void onGlobalLayout() {
        maxHeightLayout();
    }

    private void maxHeightLayout() {
        if((float)view.getHeight() > maxHeight) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = (int) maxHeight;
            view.setLayoutParams(layoutParams);
        }
    }
}
