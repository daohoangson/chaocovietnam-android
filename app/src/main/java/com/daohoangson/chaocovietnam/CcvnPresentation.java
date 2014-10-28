package com.daohoangson.chaocovietnam;

import android.annotation.TargetApi;
import android.app.Presentation;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.TextView;

import com.daohoangson.chaocovietnam.adapter.Config;
import com.daohoangson.chaocovietnam.widget.StarView;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class CcvnPresentation extends Presentation implements
        Config.OnConfigChangeListener {

    private StarView mStarView;
    private TextView mLyricsView;

    public CcvnPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @SuppressWarnings("UnusedDeclaration")
    public CcvnPresentation(Context outerContext, Display display, int theme) {
        super(outerContext, display, theme);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.presentation);

        mStarView = (StarView) findViewById(R.id.star);
        mLyricsView = (TextView) findViewById(R.id.lblLyrics);
        mLyricsView.setText("");
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
}
