package com.android.ex.photo;

import android.os.Build;
import android.os.Process;
import android.view.View;

public class PhotoViewController {

    private int mLastFlags;

    public interface PhotoViewControllerCallbacks {
        public void showActionBar();
        public void hideActionBar();
        public boolean isScaleAnimationEnabled();
        public boolean isEnterAnimationFinished();
        public View getRootView();
        public void setNotFullscreenCallbackDoNotUseThisFunction();
    }

    private final PhotoViewControllerCallbacks mCallback;

    private final View.OnSystemUiVisibilityChangeListener mSystemUiVisibilityChangeListener;

    public PhotoViewController(PhotoViewControllerCallbacks callback) {
        mCallback = callback;

        // View.OnSystemUiVisibilityChangeListener is an API that was introduced in API level 11.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            mSystemUiVisibilityChangeListener = null;
        } else {
            mSystemUiVisibilityChangeListener = new View.OnSystemUiVisibilityChangeListener() {
                @Override
                public void onSystemUiVisibilityChange(int visibility) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                            visibility == 0 && mLastFlags == 3846) {
                        mCallback.setNotFullscreenCallbackDoNotUseThisFunction();
                    }
                }
            };
        }
    }

    public void setImmersiveMode(boolean enabled) {
        int flags = 0;
        final int version = Build.VERSION.SDK_INT;
        final boolean manuallyUpdateActionBar = version < Build.VERSION_CODES.JELLY_BEAN;
        if (enabled &&
                (!mCallback.isScaleAnimationEnabled() || mCallback.isEnterAnimationFinished())) {
            // Turning on immersive mode causes an animation. If the scale animation is enabled and
            // the enter animation isn't yet complete, then an immersive mode animation should not
            // occur, since two concurrent animations are very janky.

            // Disable immersive mode for seconary users to prevent b/12015090 (freezing crash)
            // This is fixed in KK_MR2 but there is no way to differentiate between  KK and KK_MR2.
            if (version > Build.VERSION_CODES.KITKAT ||
                    version == Build.VERSION_CODES.KITKAT && !kitkatIsSecondaryUser()) {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE;
            } else if (version >= Build.VERSION_CODES.JELLY_BEAN) {
                // Clients that use the scale animation should set the following system UI flags to
                // prevent janky animations on exit when the status bar is hidden:
                //     View.SYSTEM_UI_FLAG_VISIBLE | View.SYSTEM_UI_FLAG_STABLE
                // As well, client should ensure `android:fitsSystemWindows` is set on the root
                // content view.
                flags = View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN;
            } else if (version >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                flags = View.SYSTEM_UI_FLAG_LOW_PROFILE;
            } else if (version >= Build.VERSION_CODES.HONEYCOMB) {
                flags = View.STATUS_BAR_HIDDEN;
            }

            if (manuallyUpdateActionBar) {
                mCallback.hideActionBar();
            }
        } else {
            if (version >= Build.VERSION_CODES.KITKAT) {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            } else if (version >= Build.VERSION_CODES.JELLY_BEAN) {
                flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            } else if (version >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                flags = View.SYSTEM_UI_FLAG_VISIBLE;
            } else if (version >= Build.VERSION_CODES.HONEYCOMB) {
                flags = View.STATUS_BAR_VISIBLE;
            }

            if (manuallyUpdateActionBar) {
                mCallback.showActionBar();
            }
        }

        if (version >= Build.VERSION_CODES.HONEYCOMB) {
            mLastFlags = flags;
            mCallback.getRootView().setSystemUiVisibility(flags);
        }
    }

    /**
     * Return true iff the app is being run as a secondary user on kitkat.
     *
     * This is a hack which we only know to work on kitkat.
     */
    private boolean kitkatIsSecondaryUser() {
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.KITKAT) {
            throw new IllegalStateException("kitkatIsSecondary user is only callable on KitKat");
        }
        return Process.myUid() > 100000;
    }

    /**
     * Note: This should only be called when API level is 11 or above.
     */
    public View.OnSystemUiVisibilityChangeListener getSystemUiVisibilityChangeListener() {
        return mSystemUiVisibilityChangeListener;
    }
}
