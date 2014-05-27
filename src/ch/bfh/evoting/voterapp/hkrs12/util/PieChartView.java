/* 
 * MobiVote
 * 
 *  MobiVote: Mobile application for boardroom voting
 *  Copyright (C) 2014 Bern
 *  University of Applied Sciences (BFH), Research Institute for Security
 *  in the Information Society (RISIS), E-Voting Group (EVG) Quellgasse 21,
 *  CH-2501 Biel, Switzerland
 * 
 *  Licensed under Dual License consisting of:
 *  1. GNU Affero General Public License (AGPL) v3
 *  and
 *  2. Commercial license
 * 
 *
 *  1. This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *
 *  2. Licensees holding valid commercial licenses for MobiVote may use this file in
 *   accordance with the commercial license agreement provided with the
 *   Software or, alternatively, in accordance with the terms contained in
 *   a written agreement between you and Bern University of Applied Sciences (BFH), 
 *   Research Institute for Security in the Information Society (RISIS), E-Voting Group (EVG)
 *   Quellgasse 21, CH-2501 Biel, Switzerland.
 * 
 *
 *   For further information contact us: http://e-voting.bfh.ch/
 * 
 *
 * Redistributions of files must retain the above copyright notice.
 */
package ch.bfh.evoting.voterapp.hkrs12.util;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.AbstractChart;
import org.achartengine.model.CategorySeries;
import org.achartengine.renderer.DefaultRenderer;
import org.achartengine.renderer.SimpleSeriesRenderer;

import ch.bfh.evoting.voterapp.hkrs12.entities.Option;
import ch.bfh.evoting.voterapp.hkrs12.entities.Poll;
import ch.bfh.evoting.voterapp.hkrs12.R;

import android.content.Context;
import android.graphics.Color;

/**
 * This is a content class which has hard coded values for color and value
 * variables that make up the slices in a pie chart. At the moment this is only
 * used to diplay the income, cost and sub total in the results view.
 * 
 * @author Daniel Kvist
 * http://danielkvist.net/code/piechart-with-achartengine-in-android
 */
public class PieChartView extends GraphicalView {

	private static int BASE_COLOR;

	private static int BASE_COLOR_RED;
	private static int BASE_COLOR_GREEN;
	private static int BASE_COLOR_BLUE;

	private static int LABEL_COLOR;

	/**
	 * 
	 * 
	 * Constructor that only calls the super method. It is not used to
	 * instantiate the object from outside of this class.
	 * 
	 * @param context
	 * @param abstractChart
	 */
	private PieChartView(Context context, AbstractChart abstractChart) {
		super(context, abstractChart);
	}

	/**
	 * This method returns a new graphical view as a pie chart view. It uses the
	 * income and costs and the static color constants of the class to create
	 * the chart.
	 * 
	 * @param context
	 *            the context
	 * @param poll
	 * 			  the poll which results will be displayed
	 * @return a GraphicalView object as a pie chart
	 */
	public static GraphicalView getNewInstance(Context context, Poll poll) {

		BASE_COLOR = context.getResources().getColor(
				R.color.theme_color);
		BASE_COLOR_RED = Color.red(BASE_COLOR);
		BASE_COLOR_GREEN = Color.green(BASE_COLOR);
		BASE_COLOR_BLUE = Color.blue(BASE_COLOR);

		LABEL_COLOR = context.getResources().getColor(android.R.color.black);

		return ChartFactory.getPieChartView(context, getDataSet(context, poll),
				getRenderer(poll));
	}

	/**
	 * Creates the renderer for the pie chart and sets the basic color scheme
	 * and hides labels and legend.
	 * 
	 * @return a renderer for the pie chart
	 */
	private static DefaultRenderer getRenderer(Poll poll) {

		int alpha = 255;
		DefaultRenderer defaultRenderer = new DefaultRenderer();

		for (Option option : poll.getOptions()) {
			if (option.getPercentage() > 0) {
				SimpleSeriesRenderer simpleRenderer = new SimpleSeriesRenderer();
				simpleRenderer.setColor(Color.argb(alpha, BASE_COLOR_RED,
						BASE_COLOR_GREEN, BASE_COLOR_BLUE));
				defaultRenderer.addSeriesRenderer(simpleRenderer);

				alpha = alpha - (255 / poll.getOptions().size());
			}
		}

		defaultRenderer.setLabelsColor(LABEL_COLOR);
		defaultRenderer.setLabelsTextSize(20);
		defaultRenderer.setShowLabels(true);
		defaultRenderer.setShowLegend(false);

		// Start at the 12 o clock position with drawing the slices
		defaultRenderer.setStartAngle(270);
		defaultRenderer.setAntialiasing(true);

		// Disable pan and zoom
		defaultRenderer.setPanEnabled(false);
		defaultRenderer.setZoomEnabled(false);

		defaultRenderer.setClickEnabled(true);
		return defaultRenderer;
	}

	/**
	 * Creates the data set used by the piechart by adding the string
	 * represantation and it's integer value to a CategorySeries object. Note
	 * that the string representations are hard coded.
	 * 
	 * @param context
	 *            the context
	 * @param income
	 *            the total income
	 * @param costs
	 *            the total costs
	 * @return a CategorySeries instance with the data supplied
	 */
	private static CategorySeries getDataSet(Context context, Poll poll) {
		CategorySeries series = new CategorySeries("Chart");

		for (Option option : poll.getOptions()) {
			if (option.getPercentage() > 0) {
				series.add(option.getText(), option.getPercentage());
			}
		}

		return series;
	}
}
