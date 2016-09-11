package com.sam_chordas.android.stockhawk.ui;

import android.app.Fragment;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.sam_chordas.android.stockhawk.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class LineGraphActivityFragment extends Fragment {

    public static final String DETAIL_URI = "detail_uri";
    public static final String SYMBOL_FOR_LINE_GRAPH = "symbol_for_line_graph";
    public static final String DATE_LIST = "date_list";
    public static final String ADJ_CLOSE_LIST = "adj_close_list";
    public static final String OPEN_LIST = "open_list";
    public static final String CLOSE_LIST = "close_list";
    public static final String HIGH_LIST = "high_list";
    public static final String LOW_LIST = "low_list";
    private LineChart mChart;
    private Cursor mQueryCursor;
    private String mSymbol;
    private Thread thread;
    List<Entry> entries = new ArrayList<Entry>();
    private ArrayList<String> dateList = new ArrayList<String>();
    private ArrayList<String> adjCloseList = new ArrayList<String>();
    private ArrayList<String> openList = new ArrayList<String>();
    private ArrayList<String> closeList = new ArrayList<String>();
    private ArrayList<String> highList = new ArrayList<String>();
    private ArrayList<String> lowList = new ArrayList<String>();


    public LineGraphActivityFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_line_graph, container, false);
        mChart = (LineChart) rootView.findViewById(R.id.line_chart);

//        Bundle arguments = getArguments();
//        if (arguments != null) {
//            mSymbol = arguments.getString(SYMBOL_FOR_LINE_GRAPH);
//            dateList = arguments.getStringArrayList(DATE_LIST);
//            adjCloseList = arguments.getStringArrayList(ADJ_CLOSE_LIST);
//            closeList = arguments.getStringArrayList(CLOSE_LIST);
//            openList = arguments.getStringArrayList(OPEN_LIST);
//            highList = arguments.getStringArrayList(HIGH_LIST);
//            lowList = arguments.getStringArrayList(LOW_LIST);
//        }
//        else{
            if (getActivity().getIntent() != null && getActivity().getIntent().getExtras() != null) {

                Bundle bundle = getActivity().getIntent().getExtras();
                mSymbol = bundle.getString(SYMBOL_FOR_LINE_GRAPH);
                dateList = bundle.getStringArrayList(DATE_LIST);
                adjCloseList = bundle.getStringArrayList(ADJ_CLOSE_LIST);
                closeList = bundle.getStringArrayList(CLOSE_LIST);
                openList = bundle.getStringArrayList(OPEN_LIST);
                highList = bundle.getStringArrayList(HIGH_LIST);
                lowList = bundle.getStringArrayList(LOW_LIST);

            //}
        }

        List<Entry> entries = new ArrayList<Entry>();
        String adjClose;
        for(int i = 0; i < dateList.size(); i++){
            adjClose = adjCloseList.get(i);
            entries.add(new Entry(i,Float.valueOf(adjClose)));
        }

        LineDataSet dataSet = new LineDataSet(entries,"Adj_Close");
        dataSet.setColor(Color.WHITE);
        dataSet.setValueTextColor(Color.WHITE);

        LineData lineData = new LineData(dataSet);
        mChart.setData(lineData);
        mChart.setScaleEnabled(true);
        mChart.setDragEnabled(true);
        XAxis xAxis = mChart.getXAxis();
        xAxis.setLabelRotationAngle(45.0f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);

        String[] values = new String[dateList.size()];
        for(int i = 0; i < dateList.size(); i++){
            values[i] = dateList.get(i);
        }

        xAxis.setValueFormatter(new MyXAxisValueFormatter(values));

        mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {

            }

            @Override
            public void onNothingSelected() {

            }
        });
        mChart.invalidate();

        return rootView;
    }


    public class MyXAxisValueFormatter implements AxisValueFormatter {

        private String[] mValues;

        public MyXAxisValueFormatter(String[] values) {
            this.mValues = values;
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            // "value" represents the position of the label on the axis (x or y)
            return mValues[(int) value];
        }

        /** this is only needed if numbers are returned, else return 0 */
        @Override
        public int getDecimalDigits() { return 0; }
    }

}
