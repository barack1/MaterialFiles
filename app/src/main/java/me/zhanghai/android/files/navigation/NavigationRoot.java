/*
 * Copyright (c) 2018 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.navigation;

import android.content.Context;

import androidx.annotation.NonNull;
import java8.nio.file.Path;

public interface NavigationRoot {

    @NonNull
    Path getPath();

    @NonNull
    String getName(@NonNull Context context);
}
