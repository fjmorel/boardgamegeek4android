package com.boardgamegeek.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.boardgamegeek.R;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.provider.BggContract.GamePollResults;
import com.boardgamegeek.provider.BggContract.GamePollResultsResult;
import com.boardgamegeek.provider.BggContract.GamePolls;
import com.boardgamegeek.provider.BggContract.Games;
import com.boardgamegeek.ui.widget.IntegerValueFormatter;
import com.boardgamegeek.ui.widget.PlayerNumberRow;
import com.boardgamegeek.ui.widget.PollKeyRow;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.ColorUtils;
import com.boardgamegeek.util.UIUtils;
import com.github.mikephil.charting.animation.Easing.EasingOption;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment;
import com.github.mikephil.charting.components.Legend.LegendVerticalAlignment;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import timber.log.Timber;

public class PollFragment extends DialogFragment implements LoaderCallbacks<Cursor>, OnChartValueSelectedListener {
	public static final String LANGUAGE_DEPENDENCE = "language_dependence";
	public static final String SUGGESTED_PLAYER_AGE = "suggested_playerage";
	public static final String SUGGESTED_NUM_PLAYERS = "suggested_numplayers";
	// The following should not be externalized, they're used to match the incoming XML
	private static final String BEST = "Best";
	private static final String RECOMMENDED = "Recommended";
	private static final String NOT_RECOMMENDED = "Not Recommended";

	private static final Format FORMAT = new DecimalFormat("#0");

	private String pollType;
	private int totalVoteCount;
	private int keyCount;
	private boolean isBarChart;
	private Uri pollResultUri;
	private int[] chartColors;
	private Snackbar snackbar;

	private Unbinder unbinder;
	@BindView(R.id.progress) View progressView;
	@BindView(R.id.poll_scroll) ScrollView scrollView;
	@BindView(R.id.poll_vote_total) TextView totalVoteView;
	@BindView(R.id.pie_chart) PieChart pieChart;
	@BindView(R.id.poll_list) LinearLayout pollList;
	@BindView(R.id.poll_key) LinearLayout keyList;
	@BindView(R.id.poll_key2) LinearLayout keyList2;
	@BindView(R.id.poll_key_container) View keyContainer;
	@BindView(R.id.poll_key_divider) View keyDivider;
	@BindView(R.id.no_votes_switch) Switch noVotesSwitch;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = UIUtils.fragmentArgumentsToIntent(getArguments());
		int gameId = intent.getIntExtra(ActivityUtils.KEY_GAME_ID, BggContract.INVALID_ID);
		pollType = intent.getStringExtra(ActivityUtils.KEY_TYPE);
		pollResultUri = Games.buildPollResultsResultUri(gameId, pollType);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_poll, container, false);
		unbinder = ButterKnife.bind(this, rootView);

		pieChart.setDrawEntryLabels(false);
		pieChart.setRotationEnabled(false);
		Legend legend = pieChart.getLegend();
		legend.setHorizontalAlignment(LegendHorizontalAlignment.LEFT);
		legend.setVerticalAlignment(LegendVerticalAlignment.BOTTOM);
		legend.setWordWrapEnabled(true);
		pieChart.setDescription(null);
		pieChart.setOnChartValueSelectedListener(this);

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// size the graph to be 80% of the screen width
		DisplayMetrics display = this.getResources().getDisplayMetrics();
		ViewGroup.LayoutParams lp = pieChart.getLayoutParams();
		lp.width = (int) (display.widthPixels * .8);
		pieChart.setLayoutParams(lp);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (pollType == null) {
			Timber.w("Missing type");
		}
		switch (pollType) {
			case LANGUAGE_DEPENDENCE:
				getDialog().setTitle(R.string.language_dependence);
				chartColors = ColorUtils.FIVE_STAGE_COLORS;
				break;
			case SUGGESTED_PLAYER_AGE:
				getDialog().setTitle(R.string.suggested_playerage);
				chartColors = null;
				break;
			case SUGGESTED_NUM_PLAYERS:
				isBarChart = true;
				getDialog().setTitle(R.string.suggested_numplayers);
				addKeyRow(ContextCompat.getColor(getActivity(), R.color.best), BEST);
				addKeyRow(ContextCompat.getColor(getActivity(), R.color.recommended), RECOMMENDED);
				addKeyRow(ContextCompat.getColor(getActivity(), R.color.not_recommended), NOT_RECOMMENDED);
				break;
		}

		getLoaderManager().restartLoader(Query._TOKEN, null, this);
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		unbinder.unbind();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {
		CursorLoader loader = null;
		if (id == Query._TOKEN) {
			loader = new CursorLoader(getActivity(), pollResultUri, Query.PROJECTION, null, null, Query.SORT);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (getActivity() == null) {
			return;
		}

		if (loader.getId() == Query._TOKEN) {
			if (cursor != null && cursor.moveToFirst()) {
				totalVoteCount = cursor.getInt(Query.POLL_TOTAL_VOTES);
			} else {
				totalVoteCount = 0;
			}
			totalVoteView.setText(getResources().getQuantityString(R.plurals.votes_suffix, totalVoteCount, totalVoteCount));
			totalVoteView.setVisibility(!isBarChart ? View.GONE : View.VISIBLE);

			pieChart.setVisibility((totalVoteCount == 0 || isBarChart) ? View.GONE : View.VISIBLE);
			pollList.setVisibility((totalVoteCount == 0 || !isBarChart) ? View.GONE : View.VISIBLE);
			keyContainer.setVisibility((totalVoteCount == 0 || !isBarChart) ? View.GONE : View.VISIBLE);
			noVotesSwitch.setVisibility((totalVoteCount == 0 || !isBarChart) ? View.GONE : View.VISIBLE);
			if (totalVoteCount > 0) {
				if (isBarChart) {
					createBarChart(cursor);
				} else {
					createPieChart(cursor, totalVoteCount);
				}
			}

			progressView.setVisibility(View.GONE);
			scrollView.setVisibility(View.VISIBLE);
		} else {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@Override
	public void onValueSelected(Entry e, Highlight h) {
		PieEntry pe = (PieEntry) e;
		if (pe == null || pieChart == null) {
			if (snackbar != null) {
				snackbar.dismiss();
			}
			return;
		}

		final View view = getView();
		if (view != null) {
			String message = getString(R.string.pie_chart_click_description, FORMAT.format(pe.getY()), pe.getLabel());
			snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
			snackbar.show();
		}
	}

	@Override
	public void onNothingSelected() {
		if (snackbar != null) {
			snackbar.dismiss();
		}
	}

	@OnClick(R.id.no_votes_switch)
	public void onNoVotesClick() {
		for (int i = 0; i < pollList.getChildCount(); i++) {
			PlayerNumberRow row = (PlayerNumberRow) pollList.getChildAt(i);
			row.showNoVotes(noVotesSwitch.isChecked());
		}
	}

	private void createBarChart(Cursor cursor) {
		pollList.removeAllViews();
		PlayerNumberRow row = null;
		String playerNumber;
		String lastPlayerNumber = "-1";
		do {
			playerNumber = cursor.getString(Query.POLL_RESULTS_PLAYERS);
			if (!lastPlayerNumber.equals(playerNumber)) {
				lastPlayerNumber = playerNumber;
				row = new PlayerNumberRow(getActivity());
				row.setText(playerNumber);
				row.setTotal(totalVoteCount);
				row.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						for (int i = 0; i < pollList.getChildCount(); i++) {
							((PlayerNumberRow) pollList.getChildAt(i)).clearHighlight();
						}
						PlayerNumberRow row = (PlayerNumberRow) v;
						row.setHighlight();

						int[] voteCount = row.getVotes();
						for (int i = 0; i < keyList.getChildCount(); i++) {
							((PollKeyRow) keyList.getChildAt(i)).setInfo(String.valueOf(voteCount[i]));
						}
					}
				});
				pollList.addView(row);
			}

			if (row != null) {
				String value = cursor.getString(Query.POLL_RESULTS_RESULT_VALUE);
				int votes = cursor.getInt(Query.POLL_RESULTS_RESULT_VOTES);
				if (value == null) {
					Timber.w("Missing key");
				} else {
					switch (value) {
						case BEST:
							row.setBest(votes);
							break;
						case RECOMMENDED:
							row.setRecommended(votes);
							break;
						case NOT_RECOMMENDED:
							row.setNotRecommended(votes);
							break;
						default:
							Timber.w("Bad key: %s", value);
							break;
					}
				}
			}
		} while (cursor.moveToNext());
	}

	private void createPieChart(Cursor cursor, int voteCount) {
		List<PieEntry> entries = new ArrayList<>();
		do {
			String value = cursor.getString(Query.POLL_RESULTS_RESULT_VALUE);
			int votes = cursor.getInt(Query.POLL_RESULTS_RESULT_VOTES);
			entries.add(new PieEntry(votes, value));
		} while (cursor.moveToNext());

		PieDataSet dataSet = new PieDataSet(entries, "");
		dataSet.setValueFormatter(new IntegerValueFormatter(true));
		if (chartColors != null) {
			dataSet.setColors(chartColors);
		} else {
			dataSet.setColors(ColorUtils.createColors(cursor.getCount()));
		}

		PieData data = new PieData(dataSet);
		pieChart.setData(data);
		pieChart.setCenterText(getResources().getQuantityString(R.plurals.votes_suffix, voteCount, voteCount));

		pieChart.animateY(1000, EasingOption.EaseOutCubic);
	}

	private void addKeyRow(int color, CharSequence text) {
		PollKeyRow pkr = new PollKeyRow(getActivity());
		pkr.setColor(color);
		pkr.setText(text);
		keyCount++;
		if (keyCount > 6) {
			keyList2.addView(pkr);
			keyList2.setLayoutParams(new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f));
			keyDivider.setVisibility(View.VISIBLE);
		} else {
			keyList.addView(pkr);
		}
	}

	private interface Query {
		int _TOKEN = 0x0;
		String[] PROJECTION = { GamePollResultsResult.POLL_RESULTS_RESULT_VALUE,
			GamePollResultsResult.POLL_RESULTS_RESULT_VOTES, GamePollResults.POLL_RESULTS_PLAYERS,
			GamePolls.POLL_TOTAL_VOTES };
		int POLL_RESULTS_RESULT_VALUE = 0;
		int POLL_RESULTS_RESULT_VOTES = 1;
		int POLL_RESULTS_PLAYERS = 2;
		int POLL_TOTAL_VOTES = 3;

		String SORT = GamePollResultsResult.POLL_RESULTS_SORT_INDEX + " ASC, "
			+ GamePollResultsResult.POLL_RESULTS_RESULT_SORT_INDEX;
	}
}
