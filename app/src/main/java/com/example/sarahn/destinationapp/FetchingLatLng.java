package com.example.sarahn.destinationapp;

import android.os.AsyncTask;

import java.net.URL;

/**
 * Created by SarahN on 7/11/2017.
 */
public class FetchingLatLng extends AsyncTask<URL, Void, Double > {

    @Override
    protected Double doInBackground(URL... params) {
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Double aDouble) {
        super.onPostExecute(aDouble);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }
}
