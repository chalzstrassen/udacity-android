package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.util.Log;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Log.v("Shared Preferences: ", preferences.getAll().toString());
        AsyncTask<String, Void, String[]> task = new FetchWeatherTask().execute(preferences.getString("location", "94043"),
                preferences.getString("temp_units", "C"));

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        try {
            String[] result = task.get();
            mForecastAdapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.list_item_forecast, result);
            listView.setAdapter(mForecastAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String forecast = mForecastAdapter.getItem(position);
//                    Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
                    Intent displayDetail = new Intent(getActivity(), DetailActivity.class)
                            .putExtra(Intent.EXTRA_TEXT, forecast);
                    startActivity(displayDetail);
                }
            });
        } catch (Exception e) {
            Log.e("Exception", "ForecastFragment:onCreateView.");
        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            // new FetchWeatherTask().execute("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&appid=db693a00da1ce2621cae968dbed6102e");
            updateWeather();
            return true;
        } else if (id == R.id.action_view_map) {
            try {
                showMap();
            } catch (Exception e) {
                Log.e("View map: ", "Error in showMap method");
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }
    private void showMap() throws URISyntaxException {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        intent.setData(Uri.parse("geo:0,0?q=" + preferences.getString("location", "94042")));
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        }
    }
    private void updateWeather() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        AsyncTask<String, Void, String[]> task = new FetchWeatherTask().execute(preferences.getString("location", "94043"),
                preferences.getString("temp_units", "C"));
        ListView listView = (ListView) getView().findViewById(R.id.listview_forecast);
        try {
            String[] result = task.get();
            mForecastAdapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.list_item_forecast, result);
            listView.setAdapter(mForecastAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String forecast = mForecastAdapter.getItem(position);
//                    Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
                    Intent displayDetail = new Intent(getActivity(), DetailActivity.class)
                            .putExtra(Intent.EXTRA_TEXT, forecast);
                    startActivity(displayDetail);
                }
            });
        } catch (Exception e) {
            Log.e("Exception", "ForecastFragment:updateWeather.");
        }
    }

    private class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        private static final String appId = "<open weather api key here>";

        @Override
        protected void onPostExecute(String[] strings) {
            mForecastAdapter = new ArrayAdapter<String>(getActivity(),
                    R.layout.list_item_forecast, strings);
        }

        protected String[] doInBackground(String... args) {
            Uri.Builder uri = new Uri.Builder();
            uri.scheme("http");
            uri.path("//api.openweathermap.org/data/2.5/forecast/daily");
            uri.appendQueryParameter("q", args[0]);
            uri.appendQueryParameter("mode", "json");
            String unit = args[1];
            Log.v("Temperature unit: ", unit);
            if (args[1].equals("C")) {
                uri.appendQueryParameter("units", "metric");
            } else if (args[1].equals("F")) {
                uri.appendQueryParameter("units", "imperial");
            }
            uri.appendQueryParameter("cnt","7");
            uri.appendQueryParameter("appid", appId);
            Log.v("Built uri: ", uri.build().toString());
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr;
            String[] forecastArr;
            try {
                URL url = new URL(uri.build().toString());
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                forecastJsonStr = buffer.toString();
                Log.v("Returned json string: ", forecastJsonStr);
                forecastArr = getWeatherDataFromJson(forecastJsonStr, 7, unit);
            } catch (IOException e) {
                Log.e("ForecastFragment", "Error", e);
                return null;
            } catch (JSONException j) {
                Log.e("ForecastFragment", "Error", j);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("ForecastFragment", "Error closing stream", e);
                    }
                }
            }
            return forecastArr;
        }
    }

    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low, String unit) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        final String DEG  = " \u00b0";
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        return roundedHigh + DEG + unit +  "/" + roundedLow + DEG + unit;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays, String unit)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();

        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime;
            // Cheating to convert this to UTC time, which is what we want anyhow
            dateTime = dayTime.setJulianDay(julianStartDay+i);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low, unit);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }
        return resultStrs;

    }
}
