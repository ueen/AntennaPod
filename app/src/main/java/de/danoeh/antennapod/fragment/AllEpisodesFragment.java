package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.joanzapata.iconify.Iconify;
import de.danoeh.antennapod.R;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.dialog.FilterDialog;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Like 'EpisodesFragment' except that it only shows new episodes and
 * supports swiping to mark as read.
 */
public class AllEpisodesFragment extends EpisodesListFragment {
    private static final String PREF_NAME = "PrefAllEpisodesFragment";
    private static final String PREF_PAUSEDFIRST = "pausedfirst";
    private static final String PREF_FILTER = "filter";

    private FeedItemFilter feedItemFilter = new FeedItemFilter("");
    private boolean pausedOnTop = true;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        feedItemFilter = new FeedItemFilter(getPrefFilter());
        loadPrefBooleans();
    }

    public String getPrefFilter() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_FILTER, "");
    }
    private void loadPrefBooleans() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        pausedOnTop = prefs.getBoolean(PREF_PAUSEDFIRST,true);
    }

    public void loadMenuCheked(Menu menu) {
        menu.findItem(R.id.paused_first_item).setChecked(pausedOnTop);
    }

    @Override
    protected String getPrefName() {
        return PREF_NAME;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!super.onOptionsItemSelected(item)) {
            switch (item.getItemId()) {
                case R.id.filter_items:
                    showFilterDialog();
                    return true;
                case R.id.paused_first_item:
                    pausedOnTop = !pausedOnTop;
                    item.setChecked(pausedOnTop);
                    savePrefsBoolean(PREF_PAUSEDFIRST, pausedOnTop);
                    loadItems();
                    return true;
            }
            return false;
        } else {
            return true;
        }
    }

    private void savePrefsBoolean(String s, Boolean b) {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(s, b).apply();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.filter_items).setVisible(true);
        menu.findItem(R.id.mark_all_read_item).setVisible(true);
        menu.findItem(R.id.remove_all_new_flags_item).setVisible(false);
    }

    @Override
    protected void onFragmentLoaded(List<FeedItem> episodes) {
        super.onFragmentLoaded(episodes);

        if (feedItemFilter.getValues().length > 0) {
            txtvInformation.setText("{md-info-outline} " + this.getString(R.string.filtered_label));
            Iconify.addIcons(txtvInformation);
            txtvInformation.setVisibility(View.VISIBLE);
        } else {
            txtvInformation.setVisibility(View.GONE);
        }

        resetItemTouchHelper();
    }

    private void showFilterDialog() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        FeedItemFilter prefFilter = new FeedItemFilter(prefs.getString(PREF_FILTER, ""));
        FilterDialog filterDialog = new FilterDialog(getContext(), prefFilter) {
            @Override
            protected void updateFilter(Set<String> filterValues) {
                feedItemFilter = new FeedItemFilter(filterValues.toArray(new String[0]));
                prefs.edit().putString(PREF_FILTER, StringUtils.join(filterValues, ",")).apply();
                loadItems();
            }
        };

        filterDialog.openDialog();
    }

    public void updateFeedItemFilter(String strings) {
        feedItemFilter = new FeedItemFilter(strings);
        loadItems();
    }

    ItemTouchHelper itemTouchHelper;

    public void setSwipeAction(){
        itemTouchHelper = SwipeActions.itemTouchHelper(this);

        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private ItemTouchHelper.SimpleCallback firstSwipe() {
        return new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
                builder.setTitle("what do you want to happen?");
                String[] options = requireActivity().getResources().getStringArray(R.array.mark_all_array);
                builder.setNegativeButton("normal mode", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //TODO
                    }
                });
                builder.setPositiveButton("inbox mode", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //TODO
                    }
                });
                builder.create().show();
                //Toast.makeText(requireActivity(),"First swipe",Toast.LENGTH_SHORT).show();
            }
        };
    }

    private void resetItemTouchHelper() {
        //prevent swipe staying if item is staying in the list
        if (itemTouchHelper != null && recyclerView != null) {
            itemTouchHelper.attachToRecyclerView(null);
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }
    }

    @Override
    protected boolean shouldUpdatedItemRemainInList(FeedItem item) {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        FeedItemFilter feedItemFilter = new FeedItemFilter(prefs.getString(PREF_FILTER, ""));

        if (feedItemFilter.isShowDownloaded() && (!item.hasMedia() || !item.getMedia().isDownloaded())) {
            return false;
        }

        return true;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return load(0);
    }

    private List<FeedItem> load(int offset) {
        int limit = EPISODES_PER_PAGE;
        return DBReader.getRecentlyPublishedEpisodes(offset, limit, feedItemFilter, pausedOnTop);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData() {
        return load((page - 1) * EPISODES_PER_PAGE);
    }
}
