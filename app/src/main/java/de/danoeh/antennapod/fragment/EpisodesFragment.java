package de.danoeh.antennapod.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;
import androidx.transition.TransitionSet;

import com.annimon.stream.Stream;
import com.joanzapata.iconify.Iconify;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.download.AutoUpdateManager;
import de.danoeh.antennapod.dialog.FilterDialog;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.view.EmptyViewHandler;
import de.ueen.fabmenu.FloatingActionMenu;

public class EpisodesFragment extends EpisodesListFragment {

    public static final String TAG = "PowerEpisodesFragment";
    private static final String PREF_NAME = "PrefPowerEpisodesFragment";
    private static final String PREF_POSITION = "lastquickfilter";

    public static final String PREF_FILTER = "filter";

    public EpisodesFragment() {
        super();
    }

    public EpisodesFragment(boolean hideToolbar) {
        super();
        this.hideToolbar = hideToolbar;
    }

    private FloatingActionMenu floatingQuickFilter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        feedItemFilter = new FeedItemFilter(getPrefFilter());
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        toolbar.setTitle(R.string.episodes_label);

        setSwipeActions(TAG);

        floatingQuickFilter = rootView.findViewById(R.id.fam);
        setUpQuickFilter();

        return  rootView;
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    public String getPrefFilter() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(PREF_FILTER, "");
    }


    @Override
    protected String getPrefName() {
        return PREF_NAME;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (!super.onMenuItemClick(item)) {
            if (item.getItemId() == R.id.filter_items) {
                AutoUpdateManager.runImmediate(requireContext());
                floatingQuickFilter.selectActionItemToFab(0);
                showFilterDialog();
            } else {
                return false;
            }
        }

        return true;
    }

    private void setUpQuickFilter() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        floatingQuickFilter.addActions(Stream.of(quickfilters).toArray(FloatingActionMenu.ActionItem[]::new));

        floatingQuickFilter.setOnActionClickListener(true, actionItem -> {
            setEmptyView(TAG + actionItem.getTag());

            prefs.edit().putString(PREF_POSITION, actionItem.getTag()).apply();

            updateFeedItemFilter(actionItem.getTag());
        });

        String lastpref = prefs.getString(PREF_POSITION, quickfilters.get(0).getTag());
        floatingQuickFilter.selectActionItemToFab(lastpref);
        updateFeedItemFilter(lastpref);

        floatingQuickFilter.setVisibility(View.VISIBLE);

    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.filter_items).setVisible(true);
        menu.findItem(R.id.refresh_item).setVisible(false);
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

        setEmptyView(TAG);
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

    public void updateFeedItemFilter(String tag) {
        String newFilter;
        switch (tag) {
            default:
                newFilter = getPrefFilter();
                break;
            case FeedItemFilter.UNPLAYED:
            case FeedItemFilter.DOWNLOADED:
            case FeedItemFilter.IS_FAVORITE:
                newFilter = tag;
                break;
        }

        feedItemFilter = new FeedItemFilter(newFilter);
        swipeActions.setFilter(feedItemFilter);
        loadItems();
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
        return DBReader.getRecentlyPublishedEpisodes(offset, limit, feedItemFilter);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData() {
        return load((page - 1) * EPISODES_PER_PAGE);
    }
}
