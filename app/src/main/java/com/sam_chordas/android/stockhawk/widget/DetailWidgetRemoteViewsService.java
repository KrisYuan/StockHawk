package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteHistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.LineGraphActivityFragment;

import java.util.ArrayList;

/**
 * Created by kris on 16/9/10.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailWidgetRemoteViewsService extends RemoteViewsService {

    public final String LOG_TAG = DetailWidgetRemoteViewsService.class.getSimpleName();
    private static final String[] QUOTE_COLUMNS = {
            QuoteColumns.SYMBOL,
            QuoteColumns.BIDPRICE,
            QuoteColumns.CHANGE
    };
    private static final int INDEX_SYMBOL = 0;
    private static final int INDEX_BID_PRICE = 1;
    private static final int INDEX_CHANGE = 2;

    private ArrayList<String> dateList = new ArrayList<String>();
    private ArrayList<String> adjCloseList = new ArrayList<String>();
    private ArrayList<String> openList = new ArrayList<String>();
    private ArrayList<String> closeList = new ArrayList<String>();
    private ArrayList<String> highList = new ArrayList<String>();
    private ArrayList<String> lowList = new ArrayList<String>();

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {

            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();
                String selection = QuoteColumns.ISCURRENT + " = ? ";
                String[] selectionArgs = new String[]{"1"};
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        QUOTE_COLUMNS,
                        selection,
                        selectionArgs,
                        null);
                Binder.restoreCallingIdentity(identityToken);

            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_quote_detail_list_item);
                //int id = data.getInt(INDEX_ID);
                String symbol = data.getString(INDEX_SYMBOL);
                String bidPrice = data.getString(INDEX_BID_PRICE);
                String change = data.getString(INDEX_CHANGE);

//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
//                    setRemoteContentDescription(views, description);
//                }
                views.setTextViewText(R.id.widget_stock_symbol, symbol);
                views.setTextViewText(R.id.widget_bid_price, bidPrice);
                views.setTextViewText(R.id.widget_change, change);


                String selection = QuoteHistoryColumns.SYMBOL + " = ? AND " + QuoteHistoryColumns.ISCURRENT + " = ? ";
                String sortOrder = QuoteHistoryColumns.DATE + " ASC ";
                Cursor mQueryCursor = getContentResolver().query(QuoteProvider.QuotesHistory.CONTENT_URI,
                        null,
                        selection,
                        new String[] {symbol,"1"},
                        sortOrder);
                getData(mQueryCursor);
                mQueryCursor.close();

                Bundle args = new Bundle();
                args.putString(LineGraphActivityFragment.SYMBOL_FOR_LINE_GRAPH,symbol);
                args.putStringArrayList(LineGraphActivityFragment.DATE_LIST,dateList);
                args.putStringArrayList(LineGraphActivityFragment.ADJ_CLOSE_LIST,adjCloseList);
                args.putStringArrayList(LineGraphActivityFragment.CLOSE_LIST,closeList);
                args.putStringArrayList(LineGraphActivityFragment.HIGH_LIST,highList);
                args.putStringArrayList(LineGraphActivityFragment.LOW_LIST,lowList);
                args.putStringArrayList(LineGraphActivityFragment.OPEN_LIST,openList);

                final Intent fillInIntent = new Intent();
                fillInIntent.putExtras(args);
//                fillInIntent.setData(QuoteProvider.Quotes.withSymbol(symbol));
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_quote_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
    public void getData(Cursor cursor){

        int dateIndex = cursor.getColumnIndex(QuoteHistoryColumns.DATE);
        int adjCloseIndex = cursor.getColumnIndex(QuoteHistoryColumns.ADJ_CLOSE);
        int openIndex = cursor.getColumnIndex(QuoteHistoryColumns.OPEN);
        int closeIndex = cursor.getColumnIndex(QuoteHistoryColumns.CLOSE);
        int highIndex = cursor.getColumnIndex(QuoteHistoryColumns.HIGH);
        int lowIndex = cursor.getColumnIndex(QuoteHistoryColumns.LOW);

        dateList.clear();
        adjCloseList.clear();
        openList.clear();
        closeList.clear();
        highList.clear();
        lowList.clear();

        if(dateIndex != -1 && cursor.getCount() != 0 && adjCloseIndex != -1){
            while(cursor.moveToNext()){
                dateList.add(cursor.getString(dateIndex));
                adjCloseList.add(cursor.getString(adjCloseIndex));
                openList.add(cursor.getString(openIndex));
                closeList.add(cursor.getString(closeIndex));
                highList.add(cursor.getString(highIndex));
                lowList.add(cursor.getString(lowIndex));

            }
        }

    }
}
