package de.danoeh.antennapod.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.gridlayout.widget.GridLayout;

import com.annimon.stream.Stream;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.FeeditemlistItemBinding;
import de.danoeh.antennapod.databinding.SwipeactionsDialogBinding;
import de.danoeh.antennapod.databinding.SwipeactionsPickerBinding;
import de.danoeh.antennapod.databinding.SwipeactionsPickerItemBinding;
import de.danoeh.antennapod.databinding.SwipeactionsRowBinding;
import de.danoeh.antennapod.fragment.EpisodesFragment;
import de.danoeh.antennapod.fragment.FeedItemlistFragment;
import de.danoeh.antennapod.fragment.QueueFragment;
import de.danoeh.antennapod.fragment.swipeactions.SwipeAction;
import de.danoeh.antennapod.fragment.swipeactions.SwipeActions;
import de.danoeh.antennapod.ui.common.ThemeUtils;

import java.util.List;

public class SwipeActionsDialog {
    private static final int LEFT = 1;
    private static final int RIGHT = 0;

    private final Context context;
    private final String tag;

    private SwipeAction rightAction;
    private SwipeAction leftAction;
    private List<SwipeAction> keys;

    public SwipeActionsDialog(Context context, String tag) {
        this.context = context;
        this.tag = tag;
    }

    public void show(Callback prefsChanged) {
        List<SwipeAction> rightLeft = SwipeActions.getPrefsWithDefaults(context, tag);
        leftAction = rightLeft.get(LEFT);
        rightAction = rightLeft.get(RIGHT);

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        keys = SwipeActions.swipeActions;

        String forFragment = "";
        switch (tag) {
            /*case InboxFragment.TAG:
                forFragment = context.getString(R.string.inbox_label);
                break;*/
            case EpisodesFragment.TAG:
                forFragment = context.getString(R.string.episodes_label);
                break;
            case FeedItemlistFragment.TAG:
                forFragment = context.getString(R.string.feeds_label);
                break;
            case QueueFragment.TAG:
                forFragment = context.getString(R.string.queue_label);
                keys = Stream.of(keys).filter(a -> !a.id().equals(SwipeAction.ADD_TO_QUEUE)).toList();
                break;
            default: break;
        }

        if (!tag.equals(QueueFragment.TAG)) {
            keys = Stream.of(keys).filter(a -> !a.id().equals(SwipeAction.REMOVE_FROM_QUEUE)).toList();
        }

        builder.setTitle(context.getString(R.string.swipeactions_label) + " - " + forFragment);
        SwipeactionsDialogBinding viewBinding = SwipeactionsDialogBinding.inflate(LayoutInflater.from(context));
        builder.setView(viewBinding.getRoot());

        viewBinding.enableSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            viewBinding.actionLeftContainer.getRoot().setAlpha(b ? 1.0f : 0.4f);
            viewBinding.actionRightContainer.getRoot().setAlpha(b ? 1.0f : 0.4f);
        });

        viewBinding.enableSwitch.setChecked(!SwipeActions.getNoActionPref(context, tag));

        setupSwipeDirectionView(viewBinding.actionLeftContainer, LEFT);
        setupSwipeDirectionView(viewBinding.actionRightContainer, RIGHT);

        builder.setPositiveButton(R.string.confirm_label, (dialog, which) -> {
            savePrefs(tag, rightAction.id(), leftAction.id());
            saveNoActionsPrefs(!viewBinding.enableSwitch.isChecked());
            prefsChanged.onCall();
        });

        builder.setNegativeButton(R.string.cancel_label, null);
        builder.create().show();
    }

    private void setupSwipeDirectionView(SwipeactionsRowBinding view, int direction) {
        SwipeAction action = direction == LEFT ? leftAction : rightAction;

        view.swipeDirectionLabel.setText(direction == LEFT ? R.string.swipe_left : R.string.swipe_right);
        view.swipeActionLabel.setText(action.title(context));
        populateMockEpisode(view.mockEpisode);
        if (direction == RIGHT && view.previewContainer.getChildAt(0) != view.swipeIcon) {
            view.previewContainer.removeView(view.swipeIcon);
            view.previewContainer.addView(view.swipeIcon, 0);
        }

        view.swipeIcon.setImageResource(action.actionIcon());
        view.swipeIcon.setColorFilter(ContextCompat.getColor(context, action.actionColor()));

        view.changeButton.setOnClickListener(v -> showPicker(view, direction));
        view.previewContainer.setOnClickListener(v -> showPicker(view, direction));
    }

    private void showPicker(SwipeactionsRowBinding view, int direction) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(direction == LEFT ? R.string.swipe_left : R.string.swipe_right);

        SwipeactionsPickerBinding picker = SwipeactionsPickerBinding.inflate(LayoutInflater.from(context));
        builder.setView(picker.getRoot());
        builder.setNegativeButton(R.string.cancel_label, null);
        AlertDialog dialog = builder.show();

        for (int i = 0; i < keys.size(); i++) {
            final int actionIndex = i;
            SwipeAction action = keys.get(actionIndex);
            SwipeactionsPickerItemBinding item = SwipeactionsPickerItemBinding.inflate(LayoutInflater.from(context));
            item.swipeActionLabel.setText(action.title(context));

            Drawable icon = DrawableCompat.wrap(AppCompatResources.getDrawable(context, action.actionIcon()));
            DrawableCompat.setTintMode(icon, PorterDuff.Mode.SRC_ATOP);
            if ((direction == LEFT && leftAction == action) || (direction == RIGHT && rightAction == action)) {
                DrawableCompat.setTint(icon, ContextCompat.getColor(context, action.actionColor()));
                item.swipeActionLabel.setTextColor(ContextCompat.getColor(context, action.actionColor()));
            } else {
                DrawableCompat.setTint(icon, ThemeUtils.getColorFromAttr(context, R.attr.action_icon_color));
            }
            item.swipeIcon.setImageDrawable(icon);

            item.getRoot().setOnClickListener(v -> {
                if (direction == LEFT) {
                    leftAction = keys.get(actionIndex);
                } else {
                    rightAction = keys.get(actionIndex);
                }
                setupSwipeDirectionView(view, direction);
                dialog.dismiss();
            });
            GridLayout.LayoutParams param = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.BASELINE),
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f));
            param.width  = 0;
            picker.pickerGridLayout.addView(item.getRoot(), param);
        }
        picker.pickerGridLayout.setColumnCount(2);
        picker.pickerGridLayout.setRowCount((keys.size() + 1) / 2);
    }

    private void populateMockEpisode(FeeditemlistItemBinding view) {
        view.container.setAlpha(0.3f);
        view.secondaryActionButton.secondaryActionButton.setVisibility(View.GONE);
        view.dragHandle.setVisibility(View.GONE);
        view.statusUnread.setText("███");
        view.txtvTitle.setText("███████");
        view.txtvPosition.setText("█████");
    }

    private void savePrefs(String tag, String right, String left) {
        SharedPreferences prefs = context.getSharedPreferences(SwipeActions.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(SwipeActions.PREF_SWIPEACTIONS + tag, right + "," + left).apply();
    }

    private void saveNoActionsPrefs(Boolean noActions) {
        SharedPreferences prefs = context.getSharedPreferences(SwipeActions.PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(SwipeActions.PREF_NO_ACTION + tag, noActions).apply();
    }

    public interface Callback {
        void onCall();
    }
}
