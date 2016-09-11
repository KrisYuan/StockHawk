package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    private String LOG_TAG = StockTaskService.class.getSimpleName();
    public static final String ACTION_DATA_UPDATED = "com.sam_chordas.android.stockhawk.ACTION_DATA_UPDATED";
    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    private ArrayList<String> mSymbols = new ArrayList<String>();

    public StockTaskService() {

    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();

        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                    + "in (", "UTF-8"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        mSymbols.clear();
        if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);
            if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    String[] initSymbols = new String[]{"YHOO", "AAPL", "GOOG", "MSFT"};
                    for (String symbol : initSymbols) {
                        mSymbols.add(symbol);
                    }
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else if (initQueryCursor != null) {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();

                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mSymbols.add(initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")));
                    mStoredSymbols.append("\"" +
                            initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
                    initQueryCursor.moveToNext();
                }

                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            mSymbols.add(stockInput);
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                result = GcmNetworkManager.RESULT_SUCCESS;
                try {
                    //ContentValues contentValues = new ContentValues();
                    // update ISCURRENT to 0 (false) so new data is current
                    if (isUpdate) {
//            contentValues.put(QuoteColumns.ISCURRENT, 0);
//            mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
//                null, null);
                        int deletedRows = mContext.getContentResolver().delete(QuoteProvider.Quotes.CONTENT_URI, null, null);
                        Log.d("Deleted: ", deletedRows + " Rows!");
                    }
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentVals(getResponse));
                } catch (RemoteException | OperationApplicationException e) {
                    Log.e(LOG_TAG, "Error applying batch insert", e);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        try {
            String[] mDates = Utils.get30DaysPeriod(System.currentTimeMillis());
            String startDate = mDates[0];
            String endDate = mDates[1];

            for (int i = 0; i < mSymbols.size(); i++) {
                StringBuilder urlHistoryStringBuilder = new StringBuilder();
                urlHistoryStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                urlHistoryStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol = ", "UTF-8"));
                urlHistoryStringBuilder.append(URLEncoder.encode("\"" + mSymbols.get(i) + "\"" + " and startDate = \"" + startDate + "\" and endDate = \"" + endDate + "\"", "UTF-8"));
                urlHistoryStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");

                String urlHistoryString;
                String getHistoryResponse;

                if (urlHistoryStringBuilder != null) {
                    urlHistoryString = urlHistoryStringBuilder.toString();
                    Log.d(LOG_TAG, "History URL: " + urlHistoryString);
                    try {
                        getHistoryResponse = fetchData(urlHistoryString);
                        try {
                            ContentValues contentValues = new ContentValues();
                            if (isUpdate) {
//                contentValues.put(QuoteHistoryColumns.ISCURRENT, 0);
//                int updatedRows = mContext.getContentResolver().update(QuoteProvider.QuotesHistory.withSymbol(mSymbols.get(i)), contentValues,
//                        null, null);
//                Log.d("Update: ", updatedRows + " Rows!");
                                int deletedRows = mContext.getContentResolver().delete(QuoteProvider.QuotesHistory.withSymbol(mSymbols.get(i)), null, null);
                                Log.d("Deleted: ", deletedRows + " Rows!");
                            }
                            mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                                    Utils.quoteHistoryJsonToContentValues(getHistoryResponse));
                        } catch (RemoteException | OperationApplicationException e) {
                            Log.e(LOG_TAG, "Error applying batch insert", e);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

}
