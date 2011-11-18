package com.seeedstudio.btkit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;

public class BTkitChart extends Activity {	
    // Debugging
    private static final String TAG = "BTkitService";
    private static final boolean D = true;
	
	private Handler mHandler;
	
	private XYMultipleSeriesRenderer mRenderer;
	private View mChartView;	
	private List<double[]> X = new ArrayList<double[]>();
	private List<double[]> Y = new ArrayList<double[]>();
	private  List<Date[]> date = null;
	private Date[] dateValues;
	
	private String Title;
	private String[] LINE_CHART = new String[]{"Line Chart"};
	private String DATA = "Some Data";
	private String TIME = "Time";
	
	/**
	 * Consturs
	 * @param context
	 *            the lastest activity
	 * @param mHandler
	 *            udpate UI thread
	 * @param title
	 *            chart title
	 * @param yValues
	 *            Y axis data
	 * @param xValues
	 * 			  X axis data
	 */
	public BTkitChart(Context context, Handler mHandler, String title, 
			List<double[]> yValues, List<double[]> xValues) {
		
		this.mHandler = mHandler;
		Y = yValues;
		X = xValues;
		this.Title = title;
	}

	/**
	 * Use time for the X axis by default
	 * 
	 * @param context
	 *            the lastest activity
	 * @param mHandler
	 *            udpate UI thread
	 * @param title
	 *            chart title
	 * @param yValues
	 *            Y axis data
	 */
	public BTkitChart(Context context, Handler mHandler, String title, 
			List<double[]> yValues) {
		
		this.mHandler = mHandler;
		Y = yValues;
		this.Title = title;
				
		dateValues = new Date[30];	
		date.add(dateValues);
		if(D) Log.d(TAG, dateValues.toString());
	}
	
    protected void start() {
    	int[] colors = new int[] { Color.BLUE };//line is color
    	PointStyle[] styles = new PointStyle[] { PointStyle.POINT };//line is point style for chart
    	
    	mRenderer = buildRenderer(colors, styles);//render the chart
        
    	setChartSettings(mRenderer, this.Title, TIME, DATA, dateValues[0].getTime(),
    			dateValues[dateValues.length - 1].getTime(), -4, 11, Color.GRAY, Color.LTGRAY);
        
    	mRenderer.setYLabels(10);
    	mChartView = ChartFactory.getTimeChartView(this, buildDateDataset(LINE_CHART, X, Y),
    			mRenderer, "mm:ss");
    	setContentView(mChartView);
    	
    	mHandler.obtainMessage(BTkitMain.MESSAGE_TOAST, -1, -1, "Charting")
    			.sendToTarget();
    }
    
	/**
	 * Sets a few of the series renderer settings.
	 * 
	 * @param renderer
	 *            the renderer to set the properties to
	 * @param title
	 *            the chart title
	 * @param xTitle
	 *            the title for the X axis
	 * @param yTitle
	 *            the title for the Y axis
	 * @param xMin
	 *            the minimum value on the X axis
	 * @param xMax
	 *            the maximum value on the X axis
	 * @param yMin
	 *            the minimum value on the Y axis
	 * @param yMax
	 *            the maximum value on the Y axis
	 * @param axesColor
	 *            the axes color
	 * @param labelsColor
	 *            the labels color
	 */
	protected void setChartSettings(XYMultipleSeriesRenderer renderer, String title, String xTitle, String yTitle, 
			double xMin, double xMax, double yMin, double yMax, int axesColor, int labelsColor) {
		renderer.setChartTitle(title);
		renderer.setXTitle(xTitle);
		renderer.setYTitle(yTitle);
		renderer.setXAxisMin(xMin);
		renderer.setXAxisMax(xMax);
		renderer.setYAxisMin(yMin);
		renderer.setYAxisMax(yMax);
		renderer.setAxesColor(axesColor);
		renderer.setLabelsColor(labelsColor);
	}

	/**
	 * Builds an XY multiple series renderer.
	 * 
	 * @param colors
	 *            the series rendering colors
	 * @param styles
	 *            the series point styles
	 * @return the XY multiple series renderers
	 */
	protected XYMultipleSeriesRenderer buildRenderer(int[] colors, PointStyle[] styles) {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		setRenderer(renderer, colors, styles);
		return renderer;
	}

	/**
	 * Set up the chart style and colors, etc.
	 * 
	 * @param renderer
	 *            renderer of this chart instance
	 * @param colors
	 *            chart line is color
	 * @param styles
	 *            the chart point style
	 */
	protected void setRenderer(XYMultipleSeriesRenderer renderer, int[] colors, PointStyle[] styles) {
		renderer.setAxisTitleTextSize(16);
		renderer.setChartTitleTextSize(20);
		renderer.setLabelsTextSize(15);
		renderer.setLegendTextSize(15);//图标文字大小
		renderer.setPointSize(5f);
		renderer.setMargins(new int[] { 20, 30, 15, 20 });
		int length = colors.length;
		for (int i = 0; i < length; i++) {
			XYSeriesRenderer r = new XYSeriesRenderer();
			r.setColor(colors[i]);
			r.setPointStyle(styles[i]);
			renderer.addSeriesRenderer(r);
		}
	}

	/**
	 * Builds an XY multiple time dataset using the provided values.
	 * 
	 * @param titles
	 *            the series titles
	 * @param xValues
	 *            the values for the X axis
	 * @param yValues
	 *            the values for the Y axis
	 * @return the XY multiple time dataset
	 */
	protected XYMultipleSeriesDataset buildDateDataset(String[] titles, List<double[]> xValues, List<double[]> yValues) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		int length = titles.length;
		for (int i = 0; i < length; i++) {
			TimeSeries series = new TimeSeries(titles[i]);
			double[] xV = xValues.get(i);
			double[] yV = yValues.get(i);
			int seriesLength = xV.length;
			for (int k = 0; k < seriesLength; k++) {
				series.add(xV[k], yV[k]);
			}
			dataset.addSeries(series);
		}
		return dataset;
	}
}
