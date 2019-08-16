package bin.mt.running.simulator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.widget.Toast;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.enums.NaviType;
import com.amap.api.navi.model.AMapNaviStep;
import com.amap.api.navi.model.NaviLatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class LocationUtil {
    private static final Random RANDOM = new Random();

    private static AMapNavi mAMapNavi;
    private static LocationManager locationManager;

    static boolean initLocationManager(Activity activity) {
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        try {
            locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false, false, true, true, true, 0, 5);
            return true;
        } catch (Exception e) {
            activity.finish();
            locationManager = null;
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            activity.startActivity(intent);
            Toast.makeText(activity, "请先开启模拟位置功能", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    static void initNavi(Context context, AMapNaviListener listener) {
        mAMapNavi = AMapNavi.getInstance(context);
        mAMapNavi.addAMapNaviListener(listener);
        mAMapNavi.setIsUseExtraGPSData(true);
    }

    static void startNavi() {
        mAMapNavi.startNavi(NaviType.GPS);
    }

    static void calculateDriveRoute(List<NaviLatLng> start, List<NaviLatLng> end, List<NaviLatLng> pass) {
        int strategy = 0;
        try {
            strategy = mAMapNavi.strategyConvert(true, false, false, false, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mAMapNavi.calculateDriveRoute(start, end, pass, strategy);
    }

    static List<NaviLatLng> getAllNaviLatLng() {
        speedExtra = 5.4f;
        lastChangeSpeedExtraTime = System.currentTimeMillis();
        List<NaviLatLng> list = new ArrayList<>();
        for (AMapNaviStep aMapNaviStep : mAMapNavi.getNaviPath().getSteps()) {
            list.addAll(aMapNaviStep.getCoords());
        }
        return list;
    }

    static void destroy(AMapNaviListener listener) {
        if (mAMapNavi != null) {
            mAMapNavi.removeAMapNaviListener(listener);
            mAMapNavi.stopNavi();
            mAMapNavi.destroy();
            mAMapNavi = null;
        }
    }

    private static final int NAVI_SLEEP_TIME = 200;
    private static float speedExtra;
    private static long lastChangeSpeedExtraTime;

    static void navigationLine(NaviLatLng n1, NaviLatLng n2, NavigationCallback callback) {
        float distance = AMapUtils.calculateLineDistance(newLatLng(n1), newLatLng(n2));
        if (distance == 0)
            return;
        if (System.currentTimeMillis() - lastChangeSpeedExtraTime > 30000) {
            // 每半分钟减小一次speedExtra
            // 到后面平均速度就会越来越慢
            lastChangeSpeedExtraTime = System.currentTimeMillis();
            speedExtra *= 0.94f;
        }
        int speed = 1 + RANDOM.nextInt((int) speedExtra); // 每秒几米
        if (speed == 1) {// 降低速度为1的概率
            speed = 1 + RANDOM.nextInt((int) speedExtra);
        }
        if (callback != null)
            callback.onSpeedChange(speed);
        float timeLen = distance / speed * 1000;
        // System.out.println("timeLen: "+ timeLen);

        // 距离过短
        if (timeLen <= NAVI_SLEEP_TIME) {
            // SystemClock.sleep((long) timeLen);
            setGPSData(n2.getLatitude(), n2.getLongitude(), speed);
            SystemClock.sleep(NAVI_SLEEP_TIME);
            return;
        }

        long time = System.currentTimeMillis();
        double latStart = n1.getLatitude();
        double lngStart = n1.getLongitude();
        double latDif = n2.getLatitude() - latStart;
        double lngDif = n2.getLongitude() - lngStart;
        boolean isArrive = false;
        while (true) {
            if (callback != null && callback.isCancel())
                return;
            float pass = System.currentTimeMillis() - time;
            if (pass > timeLen && isArrive)
                break;
            float frc = pass / timeLen;
            if(frc > 1){
                frc = 1;
                isArrive = true;
            }
            setGPSData(latStart + latDif * frc, lngStart + lngDif * frc, speed);
            SystemClock.sleep(NAVI_SLEEP_TIME);
        }
    }

    interface NavigationCallback {
        void onSpeedChange(int newSpeed);

        boolean isCancel();
    }


    private static LatLng newLatLng(NaviLatLng naviLatLng) {
        return new LatLng(naviLatLng.getLatitude(), naviLatLng.getLongitude());
    }

    private static long lastSetGPSDataTime = 0;

    private static void setGPSData(double lat, double lng, int speed) {
        if (System.currentTimeMillis() - lastSetGPSDataTime < NAVI_SLEEP_TIME)
            return;
        lastSetGPSDataTime = System.currentTimeMillis();
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setSpeed(speed + RANDOM.nextFloat());
        location.setAccuracy(1);
        location.setBearing(5);
        location.setTime(System.currentTimeMillis());

        // 海拔 平均22加减5
        location.setAltitude(22 + 5 * Math.sin((lat + lng) * 500));

//        System.out.println("GPS lat:" + lat + " lng:" + lng + " speed:" + speed + " altitude:" + location.getAltitude());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
        mAMapNavi.setExtraGPSData(2, location);
        emulateGPS(location);
    }

    static void setTestLocationProviderEnabled(boolean enabled) {
        locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, enabled);
    }

    private static void emulateGPS(Location gaoDeLocation) {
        Location location = new Location(LocationManager.GPS_PROVIDER);
        location.setTime(gaoDeLocation.getTime());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            location.setElapsedRealtimeNanos(gaoDeLocation.getElapsedRealtimeNanos());
        location.setAltitude(gaoDeLocation.getAltitude());
        location.setAccuracy(gaoDeLocation.getAccuracy());

        double lat = gaoDeLocation.getLatitude();
        double lon = gaoDeLocation.getLongitude();
        double a = 6378245.0;
        double ee = 0.00669342162296594323;
        double dLat = transformLat(lon - 105.0, lat - 35.0);
        double dLon = transformLon(lon - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
        dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI);
        location.setLatitude(lat - dLat);
        location.setLongitude(lon - dLon);
        locationManager.setTestProviderLocation(LocationManager.GPS_PROVIDER, location);
    }

    //转换经度
    private static double transformLon(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    //转换纬度
    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }
}
