/*
 * Copyright (c) 2018 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package me.zhanghai.android.materialfilemanager.filelist;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.zhanghai.android.materialfilemanager.R;
import me.zhanghai.android.materialfilemanager.file.FileProvider;
import me.zhanghai.android.materialfilemanager.filesystem.File;
import me.zhanghai.android.materialfilemanager.functional.Functional;
import me.zhanghai.android.materialfilemanager.util.AppUtils;
import me.zhanghai.android.materialfilemanager.util.IntentUtils;
import me.zhanghai.android.materialfilemanager.util.ViewUtils;

public class FileListFragment extends Fragment {

    @BindView(R.id.app_bar)
    AppBarLayout mAppBarLayout;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.breadcrumb)
    BreadcrumbLayout mBreadcrumbLayout;
    @BindView(R.id.content)
    ViewGroup mContentLayout;
    @BindView(R.id.swipe_refresh)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @BindView(R.id.recycler)
    RecyclerView mRecyclerView;
    @BindView(R.id.fab)
    FloatingActionButton mFab;

    private MenuItem mSortByNameMenuItem;
    private MenuItem mSortByTypeMenuItem;
    private MenuItem mSortBySizeMenuItem;
    private MenuItem mSortByLastModifiedMenuItem;
    private MenuItem mSortOrderAscendingMenuItem;
    private MenuItem mSortDirectoriesFirstMenuItem;

    private FileListAdapter mAdapter;

    private FileViewModel mViewModel;

    public static FileListFragment newInstance() {
        //noinspection deprecation
        return new FileListFragment();
    }

    /**
     * @deprecated Use {@link #newInstance()} instead.
     */
    public FileListFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.file_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ButterKnife.bind(this, view);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        activity.setSupportActionBar(mToolbar);

        int contentLayoutInitialPaddingBottom = mContentLayout.getPaddingBottom();
        mAppBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) ->
                ViewUtils.setPaddingBottom(mContentLayout, contentLayoutInitialPaddingBottom
                        + mAppBarLayout.getTotalScrollRange() + verticalOffset));
        mBreadcrumbLayout.setOnItemSelectedListener(this::onBreadcrumbItemSelected);
        mSwipeRefreshLayout.setOnRefreshListener(this::reloadFile);
        mRecyclerView.setLayoutManager(new GridLayoutManager(activity, /*TODO*/ 1));
        mAdapter = new FileListAdapter(this, this::onFileSelected);
        mRecyclerView.setAdapter(mAdapter);

        mViewModel = ViewModelProviders.of(this).get(FileViewModel.class);
        mViewModel.getSortOptionsData().observe(this, this::onSortOptionsChanged);
        mViewModel.getFileData().observe(this, this::onFileChanged);

        // TODO: Request storage permission.
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.file_list, menu);
        mSortByNameMenuItem = menu.findItem(R.id.action_sort_by_name);
        mSortByTypeMenuItem = menu.findItem(R.id.action_sort_by_type);
        mSortBySizeMenuItem = menu.findItem(R.id.action_sort_by_size);
        mSortByLastModifiedMenuItem = menu.findItem(R.id.action_sort_by_last_modified);
        mSortOrderAscendingMenuItem = menu.findItem(R.id.action_sort_order_ascending);
        mSortDirectoriesFirstMenuItem = menu.findItem(R.id.action_sort_directories_first);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        onSortOptionsChanged(mViewModel.getSortOptions());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                // TODO
                return true;
            case R.id.action_sort_by_name:
                setSortBy(FileSortOptions.By.NAME);
                return true;
            case R.id.action_sort_by_type:
                setSortBy(FileSortOptions.By.TYPE);
                return true;
            case R.id.action_sort_by_size:
                setSortBy(FileSortOptions.By.SIZE);
                return true;
            case R.id.action_sort_by_last_modified:
                setSortBy(FileSortOptions.By.LAST_MODIFIED);
                return true;
            case R.id.action_sort_order_ascending:
                setSortOrder(!mSortOrderAscendingMenuItem.isChecked() ?
                        FileSortOptions.Order.ASCENDING : FileSortOptions.Order.DESCENDING);
                return true;
            case R.id.action_sort_directories_first:
                setSortDirectoriesFirst(!mSortDirectoriesFirstMenuItem.isChecked());
                return true;
            case R.id.action_refresh:
                reloadFile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onBackPressed() {
        return mViewModel.popPath();
    }

    private void onBreadcrumbItemSelected(int index) {
        navigateToFile(mViewModel.getTrail().get(index));
    }

    private void onFileSelected(File file) {
        if (file.isListable()) {
            navigateToFile(file);
            return;
        }
        Intent intent = IntentUtils.makeView(FileProvider.getUriForFile(file.makeJavaFile()),
                file.getMimeType())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        AppUtils.startActivity(intent, requireContext());
    }

    private void navigateToFile(File file) {
        mViewModel.pushPath(file.makeFilePath());
    }

    private void reloadFile() {
        mViewModel.reload();
    }

    private void onFileChanged(File file) {
        updateSubtitle(file);
        updateBreadcrumbLayout();
        mSwipeRefreshLayout.setRefreshing(false);
        // Create a new instance every time so that AsyncListDiffer won't skip the update.
        mAdapter.submitList(new ArrayList<>(file.getFileList()));
    }

    private void updateSubtitle(File file) {
        List<File> files = file.getFileList();
        int directoryCount = Functional.reduce(files, (count, file_) -> file_.isDirectory() ?
                count + 1 : count, 0);
        int fileCount = files.size() - directoryCount;
        Resources resources = requireContext().getResources();
        String directoryCountText = directoryCount > 0 ? resources.getQuantityString(
                R.plurals.main_subtitle_directory_count_format, directoryCount, directoryCount)
                : null;
        String fileCountText = fileCount > 0 ? resources.getQuantityString(
                R.plurals.main_subtitle_file_count_format, fileCount, fileCount) : null;
        String subtitle;
        if (!TextUtils.isEmpty(directoryCountText) && !TextUtils.isEmpty(fileCountText)) {
            subtitle = directoryCountText + getString(R.string.main_subtitle_separator) +
                    fileCountText;
        } else if (!TextUtils.isEmpty(directoryCountText)) {
            subtitle = directoryCountText;
        } else if (!TextUtils.isEmpty(fileCountText)) {
            subtitle = fileCountText;
        } else {
            subtitle = getString(R.string.main_subtitle_empty);
        }
        mToolbar.setSubtitle(subtitle);
    }

    private void updateBreadcrumbLayout() {
        List<File> trail = mViewModel.getTrail();
        mBreadcrumbLayout.setItems(Functional.map(trail, File::getName),
                mViewModel.getTrailIndex());
    }

    private void setSortBy(FileSortOptions.By by) {
        FileSortOptions sortOptions = mViewModel.getSortOptions();
        if (sortOptions.getBy() == by) {
            return;
        }
        mViewModel.setSortOptions(sortOptions.withBy(by));
    }

    private void setSortOrder(FileSortOptions.Order order) {
        FileSortOptions sortOptions = mViewModel.getSortOptions();
        if (sortOptions.getOrder() == order) {
            return;
        }
        mViewModel.setSortOptions(sortOptions.withOrder(order));
    }

    private void setSortDirectoriesFirst(boolean directoriesFirst) {
        FileSortOptions sortOptions = mViewModel.getSortOptions();
        if (sortOptions.isDirectoriesFirst() == directoriesFirst) {
            return;
        }
        mViewModel.setSortOptions(sortOptions.withDirectoriesFirst(directoriesFirst));
    }

    private void onSortOptionsChanged(FileSortOptions sortOptions) {
        if (mSortByNameMenuItem == null || mSortByTypeMenuItem == null
                || mSortBySizeMenuItem == null || mSortByLastModifiedMenuItem == null
                || mSortOrderAscendingMenuItem == null || mSortDirectoriesFirstMenuItem == null) {
            return;
        }
        MenuItem checkedSortByMenuItem;
        switch (sortOptions.getBy()) {
            case NAME:
                checkedSortByMenuItem = mSortByNameMenuItem;
                break;
            case TYPE:
                checkedSortByMenuItem = mSortByTypeMenuItem;
                break;
            case SIZE:
                checkedSortByMenuItem = mSortBySizeMenuItem;
                break;
            case LAST_MODIFIED:
                checkedSortByMenuItem = mSortByLastModifiedMenuItem;
                break;
            default:
                throw new IllegalStateException();
        }
        checkedSortByMenuItem.setChecked(true);
        mSortOrderAscendingMenuItem.setChecked(sortOptions.getOrder()
                == FileSortOptions.Order.ASCENDING);
        mSortDirectoriesFirstMenuItem.setChecked(sortOptions.isDirectoriesFirst());
    }
}
