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
    public void onCalculateRouteSuccess(int[] ints) {
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

    /** errorInfo👇
     * -1	路径计算失败。	在导航过程中调用calculateDriveRoute方法导致的失败，导航过程中只能用reCalculate方法进行路径计算。
     * 1	路径计算成功。
     * 2	网络超时或网络失败,请检查网络是否通畅，稍候再试。
     * 3	路径规划起点经纬度不合法,请选择国内坐标点，确保经纬度格式正常。
     * 4	协议解析错误,请稍后再试。
     * 6	路径规划终点经纬度不合法,请选择国内坐标点，确保经纬度格式正常。
     * 7	算路服务端编码失败.
     * 10	起点附近没有找到可行道路,请对起点进行调整。
     * 11	终点附近没有找到可行道路,请对终点进行调整。
     * 12	途经点附近没有找到可行道路,请对途经点进行调整。
     * 13	key鉴权失败。	请仔细检查key绑定的sha1值与apk签名sha1值是否对应，或通过;高频问题查找相关解决办法。
     * 14	请求的服务不存在,	请稍后再试。
     * 15	请求服务响应错误,请检查网络状况，稍后再试。
     * 16	无权限访问此服务,请稍后再试。
     * 17	请求超出配额。
     * 18	请求参数非法,请检查传入参数是否符合要求。
     * 19	未知错误。
     **/
    @Override
    public void onCalculateRouteFailure(int errorInfo) {
        Toast.makeText(this, "路线计算失败：错误码=" + errorInfo, Toast.LENGTH_LONG).show();
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
