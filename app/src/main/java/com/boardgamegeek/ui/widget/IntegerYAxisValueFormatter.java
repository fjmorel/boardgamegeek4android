package com.boardgamegeek.ui.widget;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import java.text.DecimalFormat;

public class IntegerYAxisValueFormatter implements IAxisValueFormatter {
	private final DecimalFormat format;

	public IntegerYAxisValueFormatter() {
		format = new DecimalFormat("#0");
	}

	@Override
	public String getFormattedValue(float value, AxisBase axis) {
		return format.format(value);
	}
}
