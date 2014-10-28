package com.daohoangson.chaocovietnam.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.daohoangson.chaocovietnam.R;
import com.daohoangson.chaocovietnam.adapter.Config;
import com.daohoangson.chaocovietnam.widget.StarView;

public class FlagFragment extends Fragment implements
        Config.OnConfigChangeListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        View.OnTouchListener {

    private static final String ARG_INSTRUCTION_GONE = "instructionGone";

    private StarView mStarView;
    private TextView mLyricsView;
    private TextView mInstruction;
    private GestureDetectorCompat mGDC;

    private boolean mInstructionGone = false;

    final private Handler mHandler = new Handler();
    private Caller mCaller;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.flag_fragment, container, false);
        view.setOnTouchListener(this);

        mStarView = (StarView) view.findViewById(R.id.star);
        mLyricsView = (TextView) view.findViewById(R.id.lblLyrics);
        mLyricsView.setText("");
        mInstruction = (TextView) view.findViewById(R.id.lblInstruction);

        mGDC = new GestureDetectorCompat(getActivity(), this);
        mGDC.setOnDoubleTapListener(this);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_INSTRUCTION_GONE)) {
                mInstructionGone = savedInstanceState.getBoolean(ARG_INSTRUCTION_GONE);
            }
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mCaller = (Caller) getActivity();
        mCaller.setFlagFragment(this);

        if (mInstructionGone) {
            mInstruction.setVisibility(View.GONE);
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mInstruction.setVisibility(View.GONE);
                    mInstructionGone = true;
                }
            }, 3000);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mCaller.setFlagFragment(null);
        mCaller = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(ARG_INSTRUCTION_GONE, mInstructionGone);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        mGDC.onTouchEvent(motionEvent);

        return true;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent2, float v1, float v2) {
        if (mCaller == null) {
            return false;
        }

        final float absV1 = Math.abs(v1);
        final float absV2 = Math.abs(v2);
        final float v;
        final float r;
        if (absV1 > absV2) {
            v = v1 * -1;
            r = mStarView.getWidth();
        } else {
            v = v2;
            r = mStarView.getHeight();
        }

        final float percentage = v / r;
        if (Math.abs(percentage) > 0.01) {
            mCaller.seekRelative(percentage);
        }

        return true;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        if (mCaller == null) {
            return;
        }

        mCaller.flipView();
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent2, float v, float v2) {
        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        if (mCaller == null) {
            return false;
        }

        if (mCaller.isPlaying()) {
            mCaller.pausePlaying(true, false);
        } else {
            mCaller.startPlaying(true);
        }

        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent motionEvent) {
        if (mCaller == null) {
            return false;
        }

        mCaller.pausePlaying(true, true);
        mCaller.startPlaying(true);

        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onConfigChange(Config config) {
        mLyricsView.setVisibility(config.getShowLyrics() ? View.VISIBLE : View.GONE);
        mStarView.setProgressVisibility(config.getShowProgress());
    }

    public void applyConfig(Config config) {
        config.setListener(this);
    }

    public StarView getStarView() {
        return mStarView;
    }

    public TextView getLyricsView() {
        return mLyricsView;
    }

    public interface Caller {
        public void setFlagFragment(FlagFragment flagFragment);

        public void flipView();

        public void startPlaying(boolean callService);

        public void pausePlaying(boolean callService, boolean stop);

        public boolean isPlaying();

        public void seekRelative(float percentage);
    }
}
