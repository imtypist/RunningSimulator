package bin.mt.running.simulator;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.Toast;

import com.amap.api.navi.AMapNaviView;
import com.amap.api.navi.AMapNaviViewOptions;
import com.amap.api.navi.model.NaviLatLng;

import java.util.ArrayList;
import java.util.List;

public class NaviActivity extends BaseNaviActivity {
    static final List<NaviLatLng> sList = new ArrayList<>();
    static final List<NaviLatLng> eList = new ArrayList<>();
    static final List<NaviLatLng> pList = new ArrayList<>();

    private AMapNaviView mAMapNaviView;
    private boolean available = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_basic_navi);

        LocationUtil.initNavi(getApplicationContext(), this);

        mAMapNaviView = (AMapNaviView) findViewById(R.id.navi_view);
        mAMapNaviView.onCreate(savedInstanceState);

        AMapNaviViewOptions options = mAMapNaviView.getViewOptions();
        options.setTrafficLayerEnabled(false);
        options.setTilt(0);
        mAMapNaviView.setViewOptions(options);
        mAMapNaviView.setNaviMode(AMapNaviView.NORTH_UP_MODE);
        mAMapNaviView.setAMapNaviViewListener(this);
    }

    @Override
    public void onInitNaviSuccess() {
        LocationUtil.calculateDriveRoute(sList, eList, pList);
    }

    @Override
    public void onCalculateRouteSuccess() {
        LocationUtil.startNavi();
        new Thread() {
            @Override
            public void run() {
                available = true;
                List<NaviLatLng> list = LocationUtil.getAllNaviLatLng();
                LocationUtil.NavigationCallback callback = new LocationUtil.NavigationCallback() {
                    @Override
                    public void onSpeedChange(final int newSpeed) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setTitle(getString(R.string.app_name) + " - " + newSpeed + "m/s");
                            }
                        });
                    }

                    @Override
                    public boolean isCancel() {
                        return !available;
                    }
                };

                LocationUtil.setTestLocationProviderEnabled(true);
                for (int i = 1; i < list.size(); i++) {
                    LocationUtil.navigationLine(list.get(i - 1), list.get(i), callback);
                    if (!available)
                        break;
                }
                LocationUtil.setTestLocationProviderEnabled(false);

                if (!available)
                    return;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            }
        }.start();
    }

    @Override
    public void onCalculateRouteFailure(int i) {
        Toast.makeText(this, "规划路径失败", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onInitNaviFailure() {
        Toast.makeText(this, "初始化导航失败", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAMapNaviView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAMapNaviView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        available = false;
        LocationUtil.destroy(this);
        if (mAMapNaviView != null)
            mAMapNaviView.onDestroy();
    }

    @Override
    public void onNaviCancel() {
        available = false;
        finish();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_BACK || super.onKeyDown(keyCode, event);
    }
}
