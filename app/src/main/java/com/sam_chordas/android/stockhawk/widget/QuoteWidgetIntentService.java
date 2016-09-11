package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteHistoryColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

/**
 * Created by kris on 16/9/10.
 */
public class QuoteWidgetIntentService extends IntentService {

    private static final String[] QUOTE_COLUMNS = {
            QuoteColumns.SYMBOL,
            QuoteColumns.BIDPRICE,
            QuoteColumns.CHANGE
    };
    private static final int INDEX_SYMBOL = 0;
    private static final int INDEX_BID_PRICE = 1;
    private static final int INDEX_CHANGE = 2;

    public QuoteWidgetIntentService() {
        super("QuoteWidgetIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, QuoteWidgetProvider.class));

        // Get Google's stock price from the ContentProvider
        String selection = QuoteHistoryColumns.ISCURRENT + " = ? ";
        String[] selectionArgs = new String[]{"1"};
        Cursor cursor = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI, QUOTE_COLUMNS, selection, selectionArgs, null);
        if (cursor == null) {
            return;
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        String symbol = cursor.getString(INDEX_SYMBOL);
        String bidPrice = cursor.getString(INDEX_BID_PRICE);
        String change = cursor.getString(INDEX_CHANGE);

        cursor.close();

        for (int appWidgetId : appWidgetIds) {

            int widgetWidth = getWidgetWidth(appWidgetManager, appWidgetId);
            int defaultWidth = getResources().getDimensionPixelSize(R.dimen.widget_today_default_width);
            int largeWidth = getResources().getDimensionPixelSize(R.dimen.widget_today_large_width);
            int layoutId;
            if (widgetWidth >= largeWidth) {
                layoutId = R.layout.widget_quote_today_large;
            } else if (widgetWidth >= defaultWidth) {
                layoutId = R.layout.widget_quote_today;
            } else {
                layoutId = R.layout.widget_quote_today_small;
            }
            RemoteViews views = new RemoteViews(getPackageName(), layoutId);
            views.setTextViewText(R.id.widget_stock_symbol, symbol);

            Intent launchIntent = new Intent(this, MyStocksActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);
            views.setTextViewText(R.id.widget_bid_price, bidPrice);
            views.setTextViewText(R.id.widget_change, change);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

    private int getWidgetWidth(AppWidgetManager appWidgetManager, int appWidgetId) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return getResources().getDimensionPixelSize(R.dimen.widget_today_default_width);
        }
        return getWidgetWidthFromOptions(appWidgetManager, appWidgetId);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private int getWidgetWidthFromOptions(AppWidgetManager appWidgetManager, int appWidgetId) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        if (options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
            int minWidthDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            // The width returned is in dp, but we'll convert it to pixels to match the other widths
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, minWidthDp,
                    displayMetrics);
        }
        return getResources().getDimensionPixelSize(R.dimen.widget_today_default_width);
    }

}
