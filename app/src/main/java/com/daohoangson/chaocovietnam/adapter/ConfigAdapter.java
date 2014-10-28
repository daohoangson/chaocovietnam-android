package com.daohoangson.chaocovietnam.adapter;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.daohoangson.chaocovietnam.R;

public class ConfigAdapter extends BaseAdapter implements CompoundButton.OnCheckedChangeListener {

    final private static String ARG_PRESENTATION_CONFIGS = "presentationConfigs";

    final private Context mContext;
    final private Config mPrimaryConfig = new Config();
    final private SparseArray<Config> mIncompleteConfigs = new SparseArray<Config>();
    final private SparseArray<Config> mPresentationConfigs = new SparseArray<Config>();

    public ConfigAdapter(Context context, Bundle savedInstanceState) {
        mContext = context;

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_PRESENTATION_CONFIGS)) {
                SparseArray<Parcelable> saved =
                        savedInstanceState.getSparseParcelableArray(ARG_PRESENTATION_CONFIGS);
                for (int i = 0, l = saved.size(); i < l; i++) {
                    Config config = (Config) saved.valueAt(i);
                    mIncompleteConfigs.put(saved.keyAt(i), config);
                }
            }
        }

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display primaryDisplay = windowManager.getDefaultDisplay();
        mPrimaryConfig.resetDisplayMax();
        mPrimaryConfig.populateDisplay(context, primaryDisplay);
        mPrimaryConfig.setShowLyrics(true);
        mPrimaryConfig.setShowProgress(true);
    }

    @Override
    public int getCount() {
        return 1 + mPresentationConfigs.size();
    }

    @Override
    public Config getItem(int i) {
        if (i == 0) {
            return mPrimaryConfig;
        } else {
            return mPresentationConfigs.valueAt(i - 1);
        }
    }

    @Override
    public long getItemId(int i) {
        if (i == 0) {
            return 0;
        } else {
            return mPresentationConfigs.keyAt(i - 1);
        }
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final Config config = getItem(i);
        final ViewHolder holder;

        if (view == null) {
            holder = new ViewHolder();

            view = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.config_fragment_display, viewGroup, false);

            holder.display = (com.daohoangson.chaocovietnam.widget.Display) view.findViewById(R.id.display);
            holder.displayName = (TextView) view.findViewById(R.id.lblDisplayName);

            holder.showLyrics = (CheckBox) view.findViewById(R.id.chkShowLyrics);
            holder.showLyrics.setTag(config);
            holder.showLyrics.setOnCheckedChangeListener(this);

            holder.showProgress = (CheckBox) view.findViewById(R.id.chkShowProgress);
            holder.showProgress.setTag(config);
            holder.showProgress.setOnCheckedChangeListener(this);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        holder.display.setConfig(config);
        holder.displayName.setText(config.getDisplayName());
        holder.showLyrics.setChecked(config.getShowLyrics());
        holder.showProgress.setChecked(config.getShowProgress());

        return view;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
        Object tag = compoundButton.getTag();
        try {
            Config config = (Config) tag;

            switch (compoundButton.getId()) {
                case R.id.chkShowLyrics:
                    config.setShowLyrics(checked);
                    break;
                case R.id.chkShowProgress:
                    config.setShowProgress(checked);
                    break;
            }

            config.notifyListener();
        } catch (ClassCastException e) {
            // ignore
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putSparseParcelableArray(ARG_PRESENTATION_CONFIGS, mPresentationConfigs);
    }

    public void addPresentationConfig(Context outerContext, Display display) {
        final int displayId = display.getDisplayId();

        if (mIncompleteConfigs.indexOfKey(displayId) >= 0) {
            // config for this display has already existed
            // (may happen via instance state restoration)

            final Config config = mIncompleteConfigs.get(displayId);
            config.populateDisplay(outerContext, display);
            mPresentationConfigs.put(displayId, config);
            mIncompleteConfigs.delete(displayId);
        } else {
            final Config config = new Config();
            config.populateDisplay(outerContext, display);
            config.populateDefaults();
            mPresentationConfigs.put(displayId, config);
        }

        notifyDataSetChanged();
    }

    public void deletePresentationConfig(int displayId) {
        mPresentationConfigs.delete(displayId);

        notifyDataSetChanged();
    }

    public Config getPrimaryConfig() {
        return mPrimaryConfig;
    }

    public Config getPresentationConfig(int displayId) {
        return mPresentationConfigs.get(displayId);
    }

    private static class ViewHolder {

        private com.daohoangson.chaocovietnam.widget.Display display;
        private TextView displayName;
        private CheckBox showLyrics;
        private CheckBox showProgress;

    }
}
