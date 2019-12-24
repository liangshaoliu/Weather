package com.example.qingjiaxu.coolweather;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.bumptech.glide.Glide;
import com.example.qingjiaxu.coolweather.gson.Forecast;
import com.example.qingjiaxu.coolweather.gson.Weather;
import com.example.qingjiaxu.coolweather.service.AutoUpdateService;
import com.example.qingjiaxu.coolweather.util.HttpUtil;
import com.example.qingjiaxu.coolweather.util.Utility;
import com.example.qingjiaxu.coolweather.util.WheelView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private TextView windDirectionText;

    private TextView windForceText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;

    public SwipeRefreshLayout swipeRefresh;

    private String mWeatherId;

    public DrawerLayout drawerLayout;

    private Button navButton;

    private ImageView weatherImage;

    private ImageButton speakBtn;

    private ImageButton fixBtn;

    private ImageButton messageBtn;

    private ImageButton intervalBtn;

    private TextToSpeech textToSpeech;

    private LocationClient mLocationClient;

    private AutoUpdateService autoUpdateService;

    private RemoteViews remoteViews;

    private static final String TAG = "WeatherActivity";

    private ServiceConnection serviceConnection = new ServiceConnection() {
        // bindService成功后回调onServiceConnected函数
        // 通过IBinder获取Service对象,实现Activity与Service的绑定

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            autoUpdateService = ((AutoUpdateService.MyBinder)service).getService();
        }
        // 解除绑定
        @Override
        public void onServiceDisconnected(ComponentName name) {
            autoUpdateService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        //        View decorView = getWindow().getDecorView();
        //        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        //        getWindow().setStatusBarColor(Color.TRANSPARENT);

        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        getWindow().setStatusBarColor(Color.TRANSPARENT);

        setContentView(R.layout.activity_weather);

        remoteViews = new RemoteViews(this.getPackageName(), R.layout.notification_layout);
        //getNotificationManager().notify(1, getNotification(weatherImageResource("大雨")));

        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        windDirectionText = findViewById(R.id.wind_info_dir);
        windForceText = findViewById(R.id.wind_info_level);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        bingPicImg = findViewById(R.id.bing_pic_img);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        drawerLayout = findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);
        weatherImage = findViewById(R.id.weather_info_image);
        speakBtn = findViewById(R.id.speak_btn);
        fixBtn = findViewById(R.id.fix_btn);
        messageBtn = findViewById(R.id.message_btn);
        intervalBtn = findViewById(R.id.interval_btn);
        textToSpeech = new TextToSpeech(this, this);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }

        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        Intent intent = new Intent(this, AutoUpdateService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

    }
//根据天气id 请求城市天气信息
    public void requestWeather(final String weatherId) {

        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=263a5bac38e4426190a89015a5f4e8e2";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);
                            Toast.makeText(WeatherActivity.this, "更新天气信息成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });

            }
        });
        loadBingPic();
    }

    private void loadBingPic() {
        final String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        String windDirection = weather.now.windDirection;
        String windForce = weather.now.windForce + "级";
        String pm25 = "";
        final String[] UPDATE_INTERVAL = new String[]{"1小时", "3小时", "5小时", "8小时", "1天", "3天"};
        String maxTemperature = null;
        String minTemperature = null;
        String pollutionInfo = null;

        titleCity.setText(cityName);
        titleUpdateTime.setText("更新时间：" + updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        windDirectionText.setText(windDirection);
        windForceText.setText(windForce);

        weatherImage.setImageResource(weatherImageResource(weatherInfo));

        forecastLayout.removeAllViews();

        setForecastLayoutView(weather.forecastList);
        
        if (weather.forecastList.size() == 0) {
            Toast.makeText(this, "三天预报请求失败", Toast.LENGTH_SHORT).show();
        }
        else {
            maxTemperature = weather.forecastList.get(0).temperature.max + "℃";
            minTemperature = weather.forecastList.get(0).temperature.min + "℃";
        }
        
        if (weather.aqi != null) {
            pm25 = weather.aqi.city.pm25;
            pollutionInfo = weather.aqi.city.quality;
            aqiText.setText(pm25);
            pm25Text.setText(pollutionInfo);
        }
        else {
            Toast.makeText(this, "空气质量请求失败", Toast.LENGTH_SHORT).show();
        }

        final String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        //小米天气为您播报，朝阳区，今天白天到晚间，晴，最高温xx摄氏度，最低温xx摄氏度，西风，3级，空气质量优
        final String message = "和风天气为您播报," + cityName + "区,今天白天到晚间," + weatherInfo
                + ",最高温," + maxTemperature + ",最低温," + minTemperature + "," + windDirection
                + "," + windForce + ",空气质量" + pollutionInfo;

        String temperatureRange = minTemperature + "~" + maxTemperature;

        getNotificationManager().notify(1, getNotification(weatherImageResource(weatherInfo),
                cityName, degree, temperatureRange, weatherInfo, pm25, pollutionInfo, updateTime));

        speakBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textToSpeech.setSpeechRate(0.9f);
                textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null);
            }
        });

        fixBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                mLocationClient = new LocationClient(getApplicationContext());
                mLocationClient.registerLocationListener(new BDLocationListener() {
                    @Override
                    public void onReceiveLocation(BDLocation bdLocation) {
                        final BDLocation bd = bdLocation;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (bd.getDistrict().equals("朝阳区")) {
                                    mWeatherId = "CN101010300";
                                    requestWeather(mWeatherId);
                                    String locInfo;
                                    String locMethod = null;
                                    locInfo = bd.getProvince() + bd.getCity() + bd.getDistrict() + bd.getStreet();
                                    if (bd.getLocType() == BDLocation.TypeGpsLocation)
                                        locMethod = "GPS";
                                    else if (bd.getLocType() == BDLocation.TypeNetWorkLocation)
                                        locMethod = "网络";
                                    Toast.makeText(WeatherActivity.this, "已为您定位到：" + locInfo + "\n"
                                            + "定位方式：" + locMethod, Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    }
                });
                List<String> permissionList = new ArrayList<>();
                if (ContextCompat.checkSelfPermission(WeatherActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
                }

                if (!permissionList.isEmpty()) {
                    String[] permissions = permissionList.toArray(new String[permissionList.size()]);
                    ActivityCompat.requestPermissions(WeatherActivity.this, permissions, 1);
                } else {
                    requestLocation();
                }
            }
        });

        messageBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ActivityCompat.requestPermissions(WeatherActivity.this, new String[]{Manifest.permission.SEND_SMS}, 0);
                smsDialog(comfort.substring(comfort.indexOf("：") + 1, comfort.length()));
            }
        });

        intervalBtn.setOnClickListener(new View.OnClickListener() {
            String selectedItem = "8小时";

            @Override
            public void onClick(View v) {
                View outerView = LayoutInflater.from(WeatherActivity.this).inflate(R.layout.wheel_view, null);
                WheelView wv = outerView.findViewById(R.id.wheel_view_wv);
                wv.setOffset(2);
                wv.setItems(Arrays.asList(UPDATE_INTERVAL));
                wv.setSeletion(3);
                wv.setOnWheelViewListener(new WheelView.OnWheelViewListener() {
                    @Override
                    public void onSelected(int selectedIndex, String item) {
                        Log.d(TAG, "[Dialog]selectedIndex: " + selectedIndex + ", item: " + item);
                        selectedItem = item;
                    }
                });

                new AlertDialog.Builder(WeatherActivity.this)
                        .setTitle("请选择更新频率")
                        .setView(outerView)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int intervalHours = Integer.parseInt(selectedItem.substring(0, 1));
                                if (selectedItem.contains("天"))
                                    intervalHours *= 24;
                                Log.d(TAG, "onClick: " + intervalHours);
                                autoUpdateService.setInterval(intervalHours);
                                autoUpdateService.restartService();
                                Toast.makeText(WeatherActivity.this, "设置成功", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();


            }
        });
        weatherLayout.setVisibility(View.VISIBLE);

    }

    private int weatherImageResource(String weather) {
        if (weather.equals("晴"))
            //weatherImage.setImageResource(R.drawable.sunny);
            return R.drawable.sunny;
        else if (weather.equals("多云"))
            //weatherImage.setImageResource(R.drawable.sunny_cloudy);
            return R.drawable.sunny_cloudy;
        else if (weather.equals("阴"))
            //weatherImage.setImageResource(R.drawable.cloudy);
            return R.drawable.cloudy;
        else if (weather.equals("小雨"))
            //weatherImage.setImageResource(R.drawable.drizzle);
            return R.drawable.drizzle;
        else if (weather.equals("大雨"))
            //weatherImage.setImageResource(R.drawable.heavy_rain);
            return R.drawable.heavy_rain;
        else if (weather.equals("雷阵雨"))
            //weatherImage.setImageResource(R.drawable.thunder_shower);
            return R.drawable.thunder_shower;
        else if (weather.equals("强雷阵雨"))
            //weatherImage.setImageResource(R.drawable.heavy_thunder_storm);
            return R.drawable.heavy_thunder_storm;
        else if (weather.equals("霾"))
            //weatherImage.setImageResource(R.drawable.foggy);
            return R.drawable.foggy;
        else if (weather.equals("雪"))
            //weatherImage.setImageResource(R.drawable.snowy);
            return R.drawable.snowy;
        else return R.drawable.clear_night;
    }
    
    private void setForecastLayoutView(List<Forecast> list) {
        for (int i = 0; i < list.size(); i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(list.get(i).date);
            infoText.setText(list.get(i).more.info);
            maxText.setText(list.get(i).temperature.max + "℃");
            minText.setText(list.get(i).temperature.min + "℃");
            forecastLayout.addView(view);
        }
    }

    @Override
    public void onInit(int status) {
        // 判断是否转化成功
        if (status == TextToSpeech.SUCCESS) {
            //默认设定语言为中文，原生的android貌似不支持中文。
            int result = textToSpeech.setLanguage(Locale.CHINA);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(WeatherActivity.this, R.string.error_info, Toast.LENGTH_SHORT).show();
            } else {
                //不支持中文就将语言设置为英文
                textToSpeech.setLanguage(Locale.US);
            }
        }
    }

    private void smsDialog(String message) {
        final LinearLayout linearLayout = (LinearLayout) getLayoutInflater().inflate(R.layout.sms_layout, null);
        final EditText txtPhoneNo = linearLayout.findViewById(R.id.editTextPhoneNo);
        final EditText txtMessage = linearLayout.findViewById(R.id.editTextSMS);
        txtMessage.setText(message);
        linearLayout.setPadding(20, 10, 20, 10);
        new AlertDialog.Builder(this)
                .setTitle("发送信息")
                .setView(linearLayout)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendSMSMessage(txtPhoneNo.getText().toString(), txtMessage.getText().toString());

                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .create()
                .show();

    }

    private void sendSMSMessage(String phoneNo, String message) {
        Log.i("Send SMS", "");

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, message, null, null);
            Toast.makeText(getApplicationContext(), "SMS sent.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "SMS failed, please try again.",
                    Toast.LENGTH_LONG).show();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void requestLocation() {
        initLocation();
        mLocationClient.start();
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(30 * 60 * 5000);
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "必须同意定位权限才能使用本服务", Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    private Notification getNotification(int imageId, String city, String temperature, String range, String weather,
                                        String pm25, String quality, String update) {

        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,0, intent,0);
        Notification.Builder builder;
        // 获取remoteViews（参数一：包名；参数二：布局资源）
//        remoteViews.setTextViewText(R.id.item_tv1, name);
//        remoteViews.setTextViewText(R.id.item_tv2, singer);
//        remoteViews.setOnClickPendingIntent(R.id.play, pendButtonIntent1);
//        remoteViews.setOnClickPendingIntent(R.id.stop, pendButtonIntent2);
        remoteViews.setImageViewResource(R.id.notification_image, imageId);
        remoteViews.setTextViewText(R.id.notification_city, city);
        remoteViews.setTextViewText(R.id.notification_temperature, temperature);
        remoteViews.setTextViewText(R.id.notification_range, range);
        remoteViews.setTextViewText(R.id.notification_weather, weather);
        remoteViews.setTextViewText(R.id.notification_pm25, pm25);
        remoteViews.setTextViewText(R.id.notification_quality, quality);
        remoteViews.setTextViewText(R.id.notification_update, update);

        NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= 26)
        {
            //当sdk版本大于26
            String id = "channel_1";
            String description = "143";
            int importance = getNotificationManager().IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(id, description, importance);
//                     channel.enableLights(true);
//                     channel.enableVibration(true);//
            manager.createNotificationChannel(channel);
            builder = new Notification.Builder(this, id)
                    .setCategory(Notification.CATEGORY_MESSAGE)
                    .setSmallIcon(R.drawable.logo)
                    .setContent(remoteViews)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

        }
        else
        {
            //当sdk版本小于26
            builder = new Notification.Builder(this)
                    .setContentTitle("This is content title")
                    .setContentText("This is content text")
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher);
        }
        return builder.build();
    }

}
