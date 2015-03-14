package com.boardgamegeek.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;

import com.boardgamegeek.R;
import com.boardgamegeek.ui.tasks.RenameLocationTask;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.TaskUtils;
import com.boardgamegeek.util.ToolbarUtils;

import de.greenrobot.event.EventBus;

public class LocationActivity extends SimpleSinglePaneActivity implements PlaysFragment.Callbacks {
	private int mCount;
	private String mLocationName;
	private AlertDialog mDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		mLocationName = intent.getStringExtra(ActivityUtils.KEY_LOCATION_NAME);
		setTitle(mLocationName);
	}

	@Override
	protected void onStart() {
		super.onStart();
		EventBus.getDefault().register(this);
	}

	@Override
	protected void onStop() {
		EventBus.getDefault().unregister(this);
		super.onStop();
	}

	private void setTitle(String title) {
		if (TextUtils.isEmpty(title)) {
			title = getString(R.string.no_location);
		}
		getSupportActionBar().setSubtitle(title);
	}

	@Override
	protected Bundle onBeforeArgumentsSet(Bundle arguments) {
		final Intent intent = getIntent();
		arguments.putInt(PlaysFragment.KEY_MODE, PlaysFragment.MODE_LOCATION);
		arguments.putString(PlaysFragment.KEY_LOCATION, intent.getStringExtra(ActivityUtils.KEY_LOCATION_NAME));
		return arguments;
	}

	@Override
	protected Fragment onCreatePane(Intent intent) {
		return new PlaysFragment();
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.location;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		ToolbarUtils.setActionBarText(menu, R.id.menu_list_count,
			(isDrawerOpen() || mCount < 0) ? "" : String.valueOf(mCount));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_edit) {
			showDialog(mLocationName);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onPlaySelected(int playId, int gameId, String gameName, String thumbnailUrl, String imageUrl) {
		ActivityUtils.startPlayActivity(this, playId, gameId, gameName, thumbnailUrl, imageUrl);
		return false;
	}

	@Override
	public void onPlayCountChanged(int count) {
		mCount = count;
		supportInvalidateOptionsMenu();
	}

	@Override
	public void onSortChanged(String sortName) {
		// sorting not allowed
	}

	public void onEvent(RenameLocationTask.Event event) {
		mLocationName = event.locationName;
		getIntent().putExtra(ActivityUtils.KEY_LOCATION_NAME, mLocationName);
		setTitle(mLocationName);
		// recreate fragment to load the list with the new location
		getSupportFragmentManager().beginTransaction().remove(getFragment()).commit();
		createFragment();
	}

	private void showDialog(final String oldLocation) {
		LayoutInflater inflater = LayoutInflater.from(this);
		View view = inflater.inflate(R.layout.dialog_edit_text, (ViewGroup) findViewById(R.id.root_container), false);

		final EditText editText = (EditText) view.findViewById(R.id.edit_text);
		if (!TextUtils.isEmpty(oldLocation)) {
			editText.setText(oldLocation);
			editText.setSelection(0, oldLocation.length());
		}

		if (mDialog == null) {
			mDialog = new AlertDialog.Builder(this).setView(view).setTitle(R.string.title_edit_location)
				.setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String newLocation = editText.getText().toString();
						RenameLocationTask task = new RenameLocationTask(LocationActivity.this, oldLocation, newLocation);
						TaskUtils.executeAsyncTask(task);
					}
				}).create();
			mDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
		}
		mDialog.show();
	}
}
