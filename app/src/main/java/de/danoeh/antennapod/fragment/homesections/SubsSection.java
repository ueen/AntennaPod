package de.danoeh.antennapod.fragment.homesections;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.core.storage.DBReader;
import de.danoeh.antennapod.core.util.FeedItemUtil;
import de.danoeh.antennapod.fragment.InboxFragment;
import de.danoeh.antennapod.fragment.ItemPagerFragment;
import de.danoeh.antennapod.fragment.SubscriptionFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import kotlin.Unit;


public class SubsSection extends HomeSection {

    public static final String TAG = "SubsSection";

    public SubsSection(Fragment context) {
        super(context);
        sectionTitle = "Rediscover";
        sectionNavigateTitle = context.getString(R.string.subscriptions_label);
        itemType = ItemType.COVER_SMALL;
    }

    @NonNull
    @Override
    protected View.OnClickListener navigate() {
        return view -> {
            ((MainActivity) context.requireActivity()).loadFragment(SubscriptionFragment.TAG, null);
        };
    }

    @Override
    protected Unit onItemClick(View view, FeedItem feedItem) {
        //TODO PLAY
        long[] ids = FeedItemUtil.getIds(loadItems());
        int position = ArrayUtils.indexOf(ids, feedItem.getId());
        ((MainActivity) context.requireActivity()).loadChildFragment(ItemPagerFragment.newInstance(ids, position));
        return null;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadItems() {
        return DBReader.getRecentlyPublishedEpisodes(0, 6, new FeedItemFilter(""), false);
    }
}
