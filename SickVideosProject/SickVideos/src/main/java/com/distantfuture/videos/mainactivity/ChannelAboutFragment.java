package com.distantfuture.videos.mainactivity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.distantfuture.videos.R;
import com.distantfuture.videos.content.Content;
import com.distantfuture.videos.database.YouTubeData;
import com.distantfuture.videos.imageutils.BitmapLoader;
import com.distantfuture.videos.misc.ContractFragment;
import com.distantfuture.videos.misc.EmptyListHelper;
import com.distantfuture.videos.misc.Events;
import com.distantfuture.videos.misc.Utils;

import de.greenrobot.event.EventBus;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class ChannelAboutFragment extends ContractFragment<DrawerActivitySupport> implements OnRefreshListener {
  private TextView mTitle;
  private TextView mDescription;
  private ImageView mImage;
  private Content mContent;
  private PullToRefreshLayout mPullToRefreshLayout;
  private EmptyListHelper mEmptyListHelper;
  private View mContentView;
  private BitmapLoader mBitmapLoader;

  // can't add params! fragments can be recreated randomly
  public ChannelAboutFragment() {
    super();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_channel_about, container, false);

    EventBus.getDefault().register(this);

    mContent = Content.instance();
    mBitmapLoader = new BitmapLoader(getActivity(), "aboutBitmaps", 0, new BitmapLoader.GetBitmapCallback() {
      @Override
      public void onLoaded(Bitmap bitmap) {
        // put in to prevent an endless loop if the thumbnail fails to load the first time
        if (bitmap != null)
          ChannelAboutFragment.this.updateUI();
      }
    });

    mTitle = (TextView) rootView.findViewById(R.id.text_view);
    mDescription = (TextView) rootView.findViewById(R.id.description_view);
    mImage = (ImageView) rootView.findViewById(R.id.image);
    mContentView = rootView.findViewById(R.id.content_view);

    Button button = (Button) rootView.findViewById(R.id.watch_button);
    Button disclaimer = (Button) rootView.findViewById(R.id.disclaimer);
    LinearLayout card = (LinearLayout) rootView.findViewById(R.id.card);

    card.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        showPlaylistsFragment();
      }
    });
    button.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        showPlaylistsFragment();
      }
    });
    disclaimer.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        showDisclaimer();
      }
    });

    // setup empty view
    mEmptyListHelper = new EmptyListHelper(rootView.findViewById(R.id.empty_view));
    mEmptyListHelper.updateEmptyListView("Talking to YouTube...", false);

    // Now find the PullToRefreshLayout to setup
    mPullToRefreshLayout = (PullToRefreshLayout) rootView.findViewById(R.id.about_frame_layout);

    // Now setup the PullToRefreshLayout
    ActionBarPullToRefresh.from(this.getActivity())
        // Mark All Children as pullable
        .allChildrenArePullable()
            // Set the OnRefreshListener
        .listener(this)
            // Finally commit the setup to our PullToRefreshLayout
        .setup(mPullToRefreshLayout);

    mContentView.setVisibility(View.GONE);
    updateUI();

    return rootView;
  }

  // OnRefreshListener
  @Override
  public void onRefreshStarted(View view) {
    // empty cache
    mBitmapLoader.refresh();
    mContent.refreshChannelInfo();
  }

  private void showPlaylistsFragment() {
    getContract().showPlaylistsFragment();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    EventBus.getDefault().unregister(this);
  }

  // eventbus event
  public void onEvent(Events.ContentEvent event) {
    updateUI();
    mPullToRefreshLayout.setRefreshComplete();
  }

  private void showDisclaimer() {
    String title = "Disclaimer";
    String channelName = "channel";

    final YouTubeData data = mContent.channelInfo();
    if (data != null)
      channelName = data.mTitle;

    String message = Utils.getApplicationName(getActivity());
    message += " is not affiliated with " + channelName + ".";
    message += "\n\nInformation shown in this app is obtained through public YouTube APIs.  We are fans of " + channelName + " and we wanted a better viewing experience to watch their videos.";
    message += "\n\nAny copyrighted material belongs to the original owners. If you want changes made to this app, please let us know.";

    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(title)
        .setMessage(message)
        .setPositiveButton("Close", new Dialog.OnClickListener() {
          public void onClick(final DialogInterface dialogInterface, final int i) {
            dialogInterface.dismiss();
          }
        })
        .setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            dialog.dismiss();
          }
        });
    builder.create().show();
  }

  private void updateUI() {
    final YouTubeData data = mContent.channelInfo();

    // if data == null, we'll wait for the eventbus event to arrive
    if (data != null) {
      mEmptyListHelper.view().setVisibility(View.GONE);
      mContentView.setVisibility(View.VISIBLE);

      mTitle.setText(data.mTitle);
      mDescription.setText(data.mDescription);

      // uncomment to get the thumbnail image for generating icons
      // Debug.log(data.mThumbnail);

      Bitmap bitmap = mBitmapLoader.bitmap(data);
      if (bitmap != null)
        mImage.setImageBitmap(bitmap);
      else {
        mBitmapLoader.requestBitmap(data);
      }

      getContract().setActionBarTitle(data.mTitle, "About");
    }
  }
}
