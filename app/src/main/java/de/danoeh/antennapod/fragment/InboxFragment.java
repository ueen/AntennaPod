package de.danoeh.antennapod.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.model.feed.FeedItem;

/**
 * Like 'EpisodesFragment' except that it only shows new episodes and
 * supports swiping to mark as read.
 */
public class InboxFragment extends EpisodesListFragment {

    public static final String TAG = "InboxFragment";
    private static final String PREF_NAME = "PrefInboxFragment";

    @Override
    protected String getPrefName() {
        return PREF_NAME;
    }

    public InboxFragment() {
        super();
    }

    public InboxFragment(boolean hideToolbar) {
        super();
        this.hideToolbar = hideToolbar;
    }

    @Override
    protected boolean shouldUpdatedItemRemainInList(FeedItem item) {
        return item.isNew();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.filter_items).setVisible(false);
        menu.findItem(R.id.mark_all_item).setVisible(true);
        menu.findItem(R.id.swipe_settings).setVisible(true);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        toolbar.setTitle(R.string.inbox_label);

        setEmptyView(TAG);

        setSwipeActions(TAG);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (!super.onMenuItemClick(item)) {
            switch (item.getItemId()) {
                case R.id.swipe_settings:
                    swipeActions.show();
                    return true;
                default:
                    return false;
            }
        }

        return true;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        return DBReader.getNewItemsList(0, page * EPISODES_PER_PAGE);
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData() {
        return DBReader.getNewItemsList((page - 1) * EPISODES_PER_PAGE, EPISODES_PER_PAGE);
    }
}
