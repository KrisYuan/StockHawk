package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.sam_chordas.android.stockhawk.R;

public class LineGraphActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);

        String symbol = getIntent().getStringExtra(LineGraphActivityFragment.SYMBOL_FOR_LINE_GRAPH);

        Bundle arguments = new Bundle();
        arguments.putString(LineGraphActivityFragment.SYMBOL_FOR_LINE_GRAPH, symbol);

        LineGraphActivityFragment fragment = new LineGraphActivityFragment();
        fragment.setArguments(arguments);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().add(R.id.line_graph_container, fragment).commit();
        }
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }



}
