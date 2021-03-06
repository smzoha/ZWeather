package com.zedapps.zweather;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.zedapps.zweather.model.TimeData;
import com.zedapps.zweather.model.WeatherData;
import com.zedapps.zweather.service.TimeDataFetcher;
import com.zedapps.zweather.service.WeatherDataFetcher;
import com.zedapps.zweather.util.IconUtils;
import com.zedapps.zweather.util.NetworkUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

import static android.R.layout.simple_dropdown_item_1line;
import static android.widget.Toast.LENGTH_LONG;
import static com.zedapps.zweather.R.array.coordinates;
import static com.zedapps.zweather.R.array.country;
import static com.zedapps.zweather.R.string.err_msg_no_internet;
import static com.zedapps.zweather.util.IconUtils.getIconDrawableCode;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * @author Shamah M Zoha
 * @since 5/17/18
 */
public class MainActivity extends AppCompatActivity {

    public static final String TIME_FORMAT_12H = "hh:mm a";
    public static final String TIMESTAMP_FORMAT = "dd/MM/yyyy hh:mm:ss a";

    public static final SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT_12H, Locale.US);
    public static final SimpleDateFormat timeStampFormat = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US);

    private static List<String> countryList;
    private static List<String> coordinateList;

    private AutoCompleteTextView cityAutoComplete;
    private TextView txtCityLabel;
    private ImageView imgWeatherIcon;

    private TextView txtCurrentTemperature;
    private TextView txtWeatherDesc;

    private TextView txtMinTemperature;
    private TextView txtMaxTemperature;
    private TextView txtHumidity;

    private TextView txtWindSpeed;
    private TextView txtWindDeg;

    private TextView txtSunrise;
    private TextView txtSunset;

    private TextView txtUpdatedStamp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(this.getClass().getName(), "initializing application");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSearch = findViewById(R.id.btnSearch);
        cityAutoComplete = findViewById(R.id.cityAutocomplete);

        txtCityLabel = findViewById(R.id.txtCityLabel);
        txtCurrentTemperature = findViewById(R.id.txtCurrentTemperature);
        txtWeatherDesc = findViewById(R.id.txtWeatherDesc);

        txtMinTemperature = findViewById(R.id.txtMinTemp);
        txtMaxTemperature = findViewById(R.id.txtMaxTemp);
        txtHumidity = findViewById(R.id.txtHumidity);

        txtWindSpeed = findViewById(R.id.txtWindSpeed);
        txtWindDeg = findViewById(R.id.txtWindDeg);

        txtSunrise = findViewById(R.id.txtSunrise);
        txtSunset = findViewById(R.id.txtSunset);

        txtUpdatedStamp = findViewById(R.id.txtUpdatedTime);
        imgWeatherIcon = findViewById(R.id.imgWeatherIcon);

        Log.d(this.getClass().getName(), "retrieving data from resources");

        populateLists();
        initializeCityList();

        Log.d(this.getClass().getName(), "data retrieval complete");

        if (NetworkUtils.isNetworkNotConnected(getApplicationContext())) {
            Log.d(this.getClass().getName(), "no connectivity found");
            Toast.makeText(getApplicationContext(), err_msg_no_internet, LENGTH_LONG).show();
        }

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSearchAction();
            }
        });
    }

    private void initializeCityList() {
        ArrayAdapter<String> cityListAdapter = new ArrayAdapter<>(this,
                simple_dropdown_item_1line, getResources().getStringArray(country));

        cityAutoComplete.setAdapter(cityListAdapter);
    }

    @SuppressLint("SetTextI18n")
    private void handleSearchAction() {
        Log.d(this.getClass().getName(), "starting data fetching action");

        if (NetworkUtils.isNetworkNotConnected(getApplicationContext())) {
            Log.d(this.getClass().getName(), "no connectivity found");
            Toast.makeText(getApplicationContext(), err_msg_no_internet, LENGTH_LONG).show();

            return;
        }

        String cityCountryCombo = cityAutoComplete.getText().toString();

        if (countryList.contains(cityCountryCombo)) {
            String coordinates = coordinateList.get(countryList.indexOf(cityCountryCombo));
            String[] coordinatesComps = coordinates.split(",");

            AsyncTask<Object, JSONObject, WeatherData> weatherFetcher = new WeatherDataFetcher();
            weatherFetcher.execute(MainActivity.this, coordinatesComps[0], coordinatesComps[1]);

            AsyncTask<Object, JSONObject, TimeData> timeFetcher = new TimeDataFetcher();
            timeFetcher.execute(MainActivity.this, coordinatesComps[0], coordinatesComps[1]);

            try {
                Log.d(this.getClass().getName(), "processing fetched data");

                WeatherData weatherData = weatherFetcher.get();
                TimeData timeData = timeFetcher.get();

                formatViewableWeatherData(cityCountryCombo, weatherData, timeData);

                Log.d(this.getClass().getName(), "processing data successful");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(this.getClass().getName(), "invalid city param");

            txtCityLabel.setText(R.string.err_msg_invalid_city);
        }
    }

    private void formatViewableWeatherData(String cityCountryCombo, WeatherData weatherData,
                                           TimeData timeData) {

        txtCityLabel.setText(cityCountryCombo);
        imgWeatherIcon.setBackgroundResource(getIconDrawableCode(weatherData.getWeatherCode()));

        Map<String, Drawable> drawableMap = IconUtils.getDrawableMap(getApplicationContext());

        txtCurrentTemperature.setText(weatherData.getCurrentTemperature());
        txtWeatherDesc.setText(WordUtils.capitalize(weatherData.getWeatherDescription()));

        txtMinTemperature.setCompoundDrawablesWithIntrinsicBounds(drawableMap.get("minTemp"),
                null, null, null);
        txtMinTemperature.setText(weatherData.getMinTemperature());

        txtMaxTemperature.setCompoundDrawablesWithIntrinsicBounds(drawableMap.get("maxTemp"),
                null, null, null);
        txtMaxTemperature.setText(weatherData.getMaxTemperature());

        txtHumidity.setCompoundDrawablesWithIntrinsicBounds(drawableMap.get("humidity"),
                null, null, null);
        txtHumidity.setText(weatherData.getHumidity());

        if (StringUtils.isNotEmpty(weatherData.getWindSpeed())) {
            txtWindSpeed.setCompoundDrawablesWithIntrinsicBounds(drawableMap.get("windSpeed"),
                    null, null, null);
            txtWindSpeed.setText(weatherData.getWindSpeed());
        } else {
            txtWindSpeed.setText(R.string.lbl_no_wind_speed);
        }

        if (StringUtils.isNotEmpty(weatherData.getWindDeg())) {
            txtWindDeg.setCompoundDrawablesWithIntrinsicBounds(drawableMap.get("windDeg"),
                    null, null, null);
            txtWindDeg.setText(weatherData.getWindDeg());
        } else {
            txtWindDeg.setText(R.string.lbl_no_wind_deg);
        }

        timeFormat.setTimeZone(TimeZone.getTimeZone(timeData.getTimeZone()));

        txtSunrise.setCompoundDrawablesWithIntrinsicBounds(drawableMap.get("sunrise"),
                null, null, null);
        txtSunrise.setText(timeFormat.format(weatherData.getSunriseTime()));

        txtSunset.setCompoundDrawablesWithIntrinsicBounds(drawableMap.get("sunset"),
                null, null, null);
        txtSunset.setText(timeFormat.format(weatherData.getSunsetTime()));

        String lastUpdatedString = getString(R.string.lbl_last_updated) + " " +
                timeStampFormat.format(weatherData.getLastUpdatedTime());
        txtUpdatedStamp.setText(lastUpdatedString);
    }

    private void populateLists() {
        countryList = unmodifiableList(asList(getResources().getStringArray(country)));
        coordinateList = unmodifiableList(asList(getResources().getStringArray(coordinates)));
    }
}