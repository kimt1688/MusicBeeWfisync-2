// Generated by view binder compiler. Do not edit!
package kim.tkland.musicbeewifisync.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;
import kim.tkland.musicbeewifisync.R;

public final class RowItemSyncResultsBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final ImageView syncResultsDirectionIcon;

  @NonNull
  public final LinearLayout syncResultsItem;

  @NonNull
  public final TextView syncResultsLine1;

  @NonNull
  public final TextView syncResultsLine2;

  @NonNull
  public final ImageView syncResultsStatusIcon;

  private RowItemSyncResultsBinding(@NonNull LinearLayout rootView,
      @NonNull ImageView syncResultsDirectionIcon, @NonNull LinearLayout syncResultsItem,
      @NonNull TextView syncResultsLine1, @NonNull TextView syncResultsLine2,
      @NonNull ImageView syncResultsStatusIcon) {
    this.rootView = rootView;
    this.syncResultsDirectionIcon = syncResultsDirectionIcon;
    this.syncResultsItem = syncResultsItem;
    this.syncResultsLine1 = syncResultsLine1;
    this.syncResultsLine2 = syncResultsLine2;
    this.syncResultsStatusIcon = syncResultsStatusIcon;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static RowItemSyncResultsBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static RowItemSyncResultsBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.row_item_sync_results, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static RowItemSyncResultsBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.syncResultsDirectionIcon;
      ImageView syncResultsDirectionIcon = ViewBindings.findChildViewById(rootView, id);
      if (syncResultsDirectionIcon == null) {
        break missingId;
      }

      LinearLayout syncResultsItem = (LinearLayout) rootView;

      id = R.id.syncResultsLine1;
      TextView syncResultsLine1 = ViewBindings.findChildViewById(rootView, id);
      if (syncResultsLine1 == null) {
        break missingId;
      }

      id = R.id.syncResultsLine2;
      TextView syncResultsLine2 = ViewBindings.findChildViewById(rootView, id);
      if (syncResultsLine2 == null) {
        break missingId;
      }

      id = R.id.syncResultsStatusIcon;
      ImageView syncResultsStatusIcon = ViewBindings.findChildViewById(rootView, id);
      if (syncResultsStatusIcon == null) {
        break missingId;
      }

      return new RowItemSyncResultsBinding((LinearLayout) rootView, syncResultsDirectionIcon,
          syncResultsItem, syncResultsLine1, syncResultsLine2, syncResultsStatusIcon);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}