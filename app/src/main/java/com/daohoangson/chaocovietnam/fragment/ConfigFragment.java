package com.daohoangson.chaocovietnam.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.daohoangson.chaocovietnam.R;

import it.sephiroth.android.library.widget.HListView;

public class ConfigFragment extends Fragment {

    private Caller mCaller;

    private ListView mListVertical;
    private HListView mListHorizontal;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.config_fragment, container, false);

        mListVertical = (ListView) view.findViewById(R.id.listVertical);
        mListHorizontal = (HListView) view.findViewById(R.id.listHorizontal);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mCaller = (Caller) getActivity();
        mCaller.setConfigFragment(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        mCaller.setConfigFragment(null);
        mCaller = null;
    }

    public void setListAdapter(ListAdapter listAdapter) {
        if (mListVertical != null) {
            mListVertical.setAdapter(listAdapter);
        } else if (mListHorizontal != null) {
            mListHorizontal.setAdapter(listAdapter);
        }
    }

    public interface Caller {
        public void setConfigFragment(ConfigFragment flagFragment);
    }
}
