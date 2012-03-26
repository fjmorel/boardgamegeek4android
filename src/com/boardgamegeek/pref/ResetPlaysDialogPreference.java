package com.boardgamegeek.pref;

import android.content.Context;
import android.util.AttributeSet;

import com.boardgamegeek.BggApplication;
import com.boardgamegeek.R;

public class ResetPlaysDialogPreference extends AsyncDialogPreference {

	public ResetPlaysDialogPreference(Context context) {
		super(context);
	}

	public ResetPlaysDialogPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected Task getTask() {
		return new Task();
	}

	@Override
	protected int getInfoMessageResource() {
		return R.string.pref_sync_reset_plays_info_message;
	}

	@Override
	protected int getConfirmMessageResource() {
		return R.string.pref_sync_reset_confirm_message;
	}

	private class Task extends AsyncDialogPreference.Task {

		@Override
		protected Void doInBackground(Void... params) {
			BggApplication.getInstance().clearSyncPlaysSettings();
			return null;
		}
	}
}