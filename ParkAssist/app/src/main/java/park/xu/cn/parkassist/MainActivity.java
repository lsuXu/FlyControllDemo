package park.xu.cn.parkassist;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
//大疆无人机相关包导入
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;
import park.xu.cn.parkassist.tools.FlightBase;

/**
 * 用于跳转到无线网连接界面，无线连接界面返回后执行回调判断是否已连接至无人机
 * 连接成功则进入FlyActivity页面，进行无人机控制操作，否则返回到MainPageActivity页面
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();
    public static final String FLAG_CONNECTION_CHANGE = "dji_sdk_connection_change";
    private static BaseProduct mProduct;
    private Handler mHandler;
    //无人机基础类
    BaseProduct baseProduct;
    //SDKManage，获取联网后SDK的使用权限
    DJISDKManager djisdkManager;
    //无人机控制类
    FlightController flightController;
    //自定义无人机控制方法类
    FlightBase flightBase;
    Intent intent = new Intent();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loginDJI();
        setContentView(R.layout.activity_main);
    }

    protected void loginDJI(){
        UserAccountManager.getInstance().logIntoDJIUserAccount(this, new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
            @Override
            public void onSuccess(UserAccountState userAccountState) {
                Log.i("login","登录成功");
                //设置默认返回mainPageActivity页面
                intent.setClass(MainActivity.this,MainPageActivity.class);
                //直接打开无线连接页面
                startActivityForResult(new Intent( Settings.ACTION_WIFI_SETTINGS),1);
                Log.i("login","登录状态："+UserAccountManager.getInstance().getUserAccountState().toString());
            }

            @Override
            public void onFailure(DJIError djiError) {
                Log.i("login","登录失败"+ djiError.getDescription());
            }
        });
    }
    private void logoutAccount(){
        UserAccountManager.getInstance().logoutOfDJIUserAccount(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (null == error) {
                    Log.i("login","登录成功");
                } else {
                    Log.i("login","登录失败"+error.getDescription());
                }
            }
        });
    }

    //连接无线网后执行回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            flightBase = new FlightBase();
            try{
                djisdkManager = DJISDKManager.getInstance();
                baseProduct = djisdkManager.getProduct();
            }catch (Exception e){

            }finally {
                if(djisdkManager == null){
                    startActivity(intent);
                }else if(baseProduct == null){
                    startActivity(intent);
                }else if(baseProduct.isConnected()) {
                    //连接无人机成功
                    intent.setClass(MainActivity.this,FlyActivity.class);
                    startActivity(intent);
                }else{
                    startActivity(intent);
                }
            }
            finish();
        }
    }
}
