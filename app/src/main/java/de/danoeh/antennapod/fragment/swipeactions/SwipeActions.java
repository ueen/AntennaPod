package de.danoeh.antennapod.fragment.swipeactions;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.annimon.stream.Optional;
import com.annimon.stream.Stream;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.dialog.SwipeActionsDialog;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import de.danoeh.antennapod.view.viewholder.EpisodeItemViewHolder;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class SwipeActions extends ItemTouchHelper.SimpleCallback implements LifecycleObserver {
    public static final String PREF_NAME = "SwipeActionsPrefs";
    public static final String KEY_PREFIX_SWIPEACTIONS = "PrefSwipeActions";
    public static final String KEY_PREFIX_NO_ACTION = "PrefNoSwipeAction";

    public static final List<SwipeAction> swipeActions = Collections.unmodifiableList(
            Arrays.asList(new AddToQueueSwipeAction(), new RemoveFromInboxSwipeAction(),
                    new StartDownloadSwipeAction(), new MarkFavouriteSwipeAction(),
                    new MarkPlayedSwipeAction(), new RemoveFromQueueSwipeAction())
    );

    private final Fragment fragment;
    private final String tag;
    private FeedItemFilter filter = null;

    Pair<SwipeAction, SwipeAction> rightleft = null;
    boolean swipeOutEnabled = true;
    int swipedOutTo = 0;
    private final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(this);

    public SwipeActions(int dragDirs, Fragment fragment, String tag) {
        super(dragDirs, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT);
        this.fragment = fragment;
        this.tag = tag;
        reloadPreference();
        fragment.getLifecycle().addObserver(this);
    }

    public SwipeActions(Fragment fragment, String tag) {
        this(0, fragment, tag);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void reloadPreference() {
        rightleft = getPrefs(fragment.requireContext(), tag);
    }

    public void setFilter(FeedItemFilter filter) {
        this.filter = filter;
    }

    public SwipeActions attachTo(RecyclerView recyclerView) {
        itemTouchHelper.attachToRecyclerView(recyclerView);
        return this;
    }

    public void detach() {
        itemTouchHelper.attachToRecyclerView(null);
    }

    private static Pair<SwipeAction, SwipeAction> getPrefs(Context context, String tag, String defaultActions) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String prefsString = prefs.getString(KEY_PREFIX_SWIPEACTIONS + tag, defaultActions);

        String[] rightleft = prefsString.split(",");

        //no preferences set, no default (very fist swipe)
        if (rightleft.length != 2) {
            return null;
        }

        Optional<SwipeAction> right = Stream.of(swipeActions)
                .filter(a -> a.getId().equals(rightleft[0])).findFirst();
        Optional<SwipeAction> left = Stream.of(swipeActions)
                .filter(a -> a.getId().equals(rightleft[1])).findFirst();

        // invalid ids
        if (!right.isPresent() || !left.isPresent()) {
            return new Pair<>(swipeActions.get(0), swipeActions.get(0));
        }

        return new Pair<>(right.get(), left.get());
    }

    private static Pair<SwipeAction, SwipeAction> getPrefs(Context context, String tag) {
        return getPrefs(context, tag, "");
    }

    public static Pair<SwipeAction, SwipeAction> getPrefsWithDefaults(Context context, String tag) {
        String defaultActions;
        switch (tag) {
            /*case InboxFragment.TAG:
                defaultActions = new int[] {ADD_TO_QUEUE, MARK_UNPLAYED};
                break;*/
            case QueueFragment.TAG:
                defaultActions = SwipeAction.REMOVE_FROM_QUEUE + "," + SwipeAction.REMOVE_FROM_QUEUE;
                break;
            default:
            case EpisodesFragment.TAG:
                defaultActions = SwipeAction.MARK_FAV + "," + SwipeAction.START_DOWNLOAD;
                break;
        }

        return getPrefs(context, tag, defaultActions);
    }

    public static boolean isSwipeActionEnabled(Context context, String tag) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PREFIX_NO_ACTION + tag, true);
    }

    private Boolean isSwipeActionEnabled() {
        return isSwipeActionEnabled(fragment.requireContext(), tag);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
        if (rightleft == null) {
            //open settings dialog if no prefs are set
            new SwipeActionsDialog(fragment.requireContext(), tag).show(this::reloadPreference);
            return;
        }

        FeedItem item = ((EpisodeItemViewHolder) viewHolder).getFeedItem();

        if (swipeDir == ItemTouchHelper.RIGHT && rightleft.first != null) {
            rightleft.first.performAction(item, fragment, filter);
        } else if (swipeDir == ItemTouchHelper.LEFT && rightleft.second != null) {
            rightleft.second.performAction(item, fragment, filter);
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            float dx, float dy, int actionState, boolean isCurrentlyActive) {
        SwipeAction right;
        SwipeAction left;
        if (rightleft != null) {
            right = rightleft.first;
            left = rightleft.second;
            if (left == null || right == null) {
                return;
            }
        } else {
            right = left = new ShowFirstSwipeDialogAction();
        }

        //check if it will be removed
        boolean rightWillRemove = right.willRemove(filter);
        boolean leftWillRemove = left.willRemove(filter);
        boolean wontLeave = (dx > 0 && !rightWillRemove) || (dx < 0 && !leftWillRemove);

        //Limit swipe if it's not removed
        int maxMovement = recyclerView.getWidth() * 2 / 5;
        float sign = dx > 0 ? 1 : -1;
        float limitMovement = Math.min(maxMovement, sign * dx);
        float displacementPercentage = limitMovement / maxMovement;

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE && wontLeave) {
            swipeOutEnabled = false;

            boolean swipeThresholdReached = displacementPercentage == 1;

            // Move slower when getting near the maxMovement
            dx = sign * maxMovement * (float) Math.sin((Math.PI / 2) * displacementPercentage);

            if (isCurrentlyActive) {
                int dir = dx > 0 ? ItemTouchHelper.RIGHT : ItemTouchHelper.LEFT;
                swipedOutTo = swipeThresholdReached ? dir : 0;
            }
        } else {
            swipeOutEnabled = true;
        }

        //add color and icon
        Context context = fragment.requireContext();
        int themeColor = ThemeUtils.getColorFromAttr(context, android.R.attr.windowBackground);
        int actionColor = ThemeUtils.getColorFromAttr(context,
                dx > 0 ? right.getActionColor() : left.getActionColor());
        RecyclerViewSwipeDecorator.Builder builder = new RecyclerViewSwipeDecorator.Builder(
                c, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive)
                .addSwipeRightActionIcon(right.getActionIcon())
                .addSwipeLeftActionIcon(left.getActionIcon())
                .addSwipeRightBackgroundColor(ThemeUtils.getColorFromAttr(context, R.attr.background_elevated))
                .addSwipeLeftBackgroundColor(ThemeUtils.getColorFromAttr(context, R.attr.background_elevated))
                .setActionIconTint(
                        ColorUtils.blendARGB(themeColor,
                                actionColor,
                                Math.max(0.5f, displacementPercentage)));
        builder.create().decorate();


        super.onChildDraw(c, recyclerView, viewHolder, dx, dy, actionState, isCurrentlyActive);
    }

    @Override
    public float getSwipeEscapeVelocity(float defaultValue) {
        return swipeOutEnabled ? defaultValue : Float.MAX_VALUE;
    }

    @Override
    public float getSwipeVelocityThreshold(float defaultValue) {
        return swipeOutEnabled ? defaultValue : 0;
    }

    @Override
    public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
        return swipeOutEnabled ? 0.6f : 1.0f;
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);

        if (swipedOutTo != 0) {
            onSwiped(viewHolder, swipedOutTo);
            swipedOutTo = 0;
        }
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        if (!isSwipeActionEnabled()) {
            return makeMovementFlags(getDragDirs(recyclerView, viewHolder), 0);
        } else {
            return super.getMovementFlags(recyclerView, viewHolder);
        }
    }

    public void startDrag(EpisodeItemViewHolder holder) {
        itemTouchHelper.startDrag(holder);
    }
}
