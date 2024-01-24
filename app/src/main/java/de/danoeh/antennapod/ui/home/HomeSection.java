package de.danoeh.antennapod.ui.home;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;

import com.google.android.material.color.MaterialColors;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.adapter.EpisodeItemListAdapter;
import de.danoeh.antennapod.adapter.HorizontalFeedListAdapter;
import de.danoeh.antennapod.adapter.HorizontalItemListAdapter;
import de.danoeh.antennapod.databinding.HomeSectionBinding;
import de.danoeh.antennapod.menuhandler.FeedItemMenuHandler;
import de.danoeh.antennapod.menuhandler.FeedMenuHandler;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import org.greenrobot.eventbus.EventBus;

import java.util.Locale;

/**
 * Section on the HomeFragment
 */
public abstract class HomeSection extends Fragment implements View.OnCreateContextMenuListener {
    public static final String TAG = "HomeSection";
    protected HomeSectionBinding viewBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        viewBinding = HomeSectionBinding.inflate(inflater);
        viewBinding.titleLabel.setText(getSectionTitle());
        if (TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_LTR) {
            viewBinding.moreButton.setText(getMoreLinkTitle() + "\u00A0»");
        } else {
            viewBinding.moreButton.setText("«\u00A0" + getMoreLinkTitle());
        }
        viewBinding.moreButton.setOnClickListener((view) -> handleMoreClick());
        if (TextUtils.isEmpty(getMoreLinkTitle())) {
            viewBinding.moreButton.setVisibility(View.INVISIBLE);
        }
        Fragment expand = getExpandable();
        if (expand != null) {
            viewBinding.recyclerView.setVisibility(View.GONE);
            viewBinding.homeExpandableContainer.setVisibility(View.VISIBLE);
            viewBinding.parent.setLayoutParams(
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            viewBinding.parent.setPadding(0,0,0,0);
            getChildFragmentManager().beginTransaction().add(viewBinding.homeExpandableContainer.getId(), expand).commit();
        } else {
            // Dummies are necessary to ensure height, but do not animate them
            viewBinding.recyclerView.setItemAnimator(null);
            viewBinding.recyclerView.postDelayed(
                    () -> viewBinding.recyclerView.setItemAnimator(new DefaultItemAnimator()), 500);
        }
        return viewBinding.getRoot();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (!getUserVisibleHint() || !isVisible() || !isMenuVisible()) {
            // The method is called on all fragments in a ViewPager, so this needs to be ignored in invisible ones.
            // Apparently, none of the visibility check method works reliably on its own, so we just use all.
            return false;
        }
        if (viewBinding.recyclerView.getAdapter() instanceof HorizontalFeedListAdapter) {
            HorizontalFeedListAdapter adapter = (HorizontalFeedListAdapter) viewBinding.recyclerView.getAdapter();
            Feed selectedFeed = adapter.getLongPressedItem();
            return selectedFeed != null
                    && FeedMenuHandler.onMenuItemClicked(this, item.getItemId(), selectedFeed, () -> { });
        }
        FeedItem longPressedItem;
        if (viewBinding.recyclerView.getAdapter() instanceof EpisodeItemListAdapter) {
            EpisodeItemListAdapter adapter = (EpisodeItemListAdapter) viewBinding.recyclerView.getAdapter();
            longPressedItem = adapter.getLongPressedItem();
        } else if (viewBinding.recyclerView.getAdapter() instanceof HorizontalItemListAdapter) {
            HorizontalItemListAdapter adapter = (HorizontalItemListAdapter) viewBinding.recyclerView.getAdapter();
            longPressedItem = adapter.getLongPressedItem();
        } else {
            return false;
        }

        if (longPressedItem == null) {
            Log.i(TAG, "Selected item or listAdapter was null, ignoring selection");
            return super.onContextItemSelected(item);
        }
        return FeedItemMenuHandler.onMenuItemClicked(this, item.getItemId(), longPressedItem);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isExpandable()) EventBus.getDefault().register(this);
        if (!isExpandable()) registerForContextMenu(viewBinding.recyclerView);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!isExpandable()) EventBus.getDefault().unregister(this);
        if (!isExpandable()) unregisterForContextMenu(viewBinding.recyclerView);
    }

    protected abstract String getSectionTitle();

    protected Fragment getExpandable() {
        return null;
    }
    protected boolean isExpandable() {
        return false;
    }

    protected abstract String getMoreLinkTitle();

    protected abstract void handleMoreClick();
}
