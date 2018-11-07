/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import me.zhanghai.android.files.filesystem.File;
import me.zhanghai.android.files.util.FragmentUtils;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_PREFIX = MainActivity.class.getName() + '.';

    private static final String EXTRA_FILE = KEY_PREFIX + "FILE";

    @Nullable
    private File mExtraFile;

    @NonNull
    private MainFragment mMainFragment;

    @NonNull
    public static Intent makeIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class);
    }

    @NonNull
    public static Intent makeIntent(@NonNull File file, @NonNull Context context) {
        return makeIntent(context)
                .putExtra(EXTRA_FILE, file);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExtraFile = getIntent().getParcelableExtra(EXTRA_FILE);

        // Calls ensureSubDecor().
        findViewById(android.R.id.content);

        if (savedInstanceState == null) {
            mMainFragment = MainFragment.newInstance(mExtraFile);
            FragmentUtils.add(mMainFragment, this, android.R.id.content);
        } else {
            mMainFragment = FragmentUtils.findById(this, android.R.id.content);
        }
    }

    @Override
    public void onBackPressed() {
        if (mMainFragment.onBackPressed()) {
            return;
        }
        super.onBackPressed();
    }
}