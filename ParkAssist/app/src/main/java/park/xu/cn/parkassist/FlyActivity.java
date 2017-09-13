package park.xu.cn.parkassist;

import android.content.pm.ActivityInfo;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import dji.common.error.DJIError;
import dji.common.flightcontroller.virtualstick.Limits;
import dji.common.remotecontroller.AircraftMappingStyle;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.camera.VideoFeeder;
import dji.sdk.codec.DJICodecManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mobilerc.MobileRemoteController;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKManager;
import park.xu.cn.parkassist.tools.FlightBase;
import park.xu.cn.parkassist.tools.OnScreenJoystick;
import park.xu.cn.parkassist.tools.OnScreenJoystickListener;

/**
 * Created by Namuh on 2017/8/30.
 */

public class FlyActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{
    Button btn_fly_start ;
    Button btn_fly_back;
    Button btn_execute;
    Button btn_upload;
    //操纵杆
    OnScreenJoystick screenJoystickLeft ;
    OnScreenJoystick screenJoystickRight ;

    MobileRemoteController mobileRemoteController ;
    RemoteController remoteController;
    private long exitTime = 0;
    VideoFeeder.VideoDataCallback mReceivedVideoDataCallBack = null;
    DJICodecManager mCodecManager = null;
    TextureView mVideoSurface = null;
    //无人机基础类
    BaseProduct baseProduct;
    //SDKManage，获取联网后SDK的使用权限
    DJISDKManager djisdkManager;
    //自定义无人机控制方法类
    FlightBase flightBase;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);//隐藏标题
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏
        setContentView(R.layout.fly_control);

        init();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            exit();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }
    public void exit(){
        if((System.currentTimeMillis() -exitTime) > 2000){
            Toast.makeText(getApplicationContext(),"再按一次退出应用程序",
                    Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        }else{
            finish();
            System.exit(0);
        }
    }

    //初始化
    private void init(){
        mVideoSurface = (TextureView)findViewById(R.id.video_previewer_surface);
        //按钮初始化
        btn_fly_start = (Button) findViewById(R.id.btn_start_fly);
        btn_fly_back = (Button) findViewById(R.id.btn_end_fly);
        btn_execute = (Button) findViewById(R.id.btn_execute);
        btn_upload = (Button)findViewById(R.id.btn_upload);
        screenJoystickLeft = (OnScreenJoystick) findViewById(R.id.directionJoystickLeft);
        screenJoystickRight = (OnScreenJoystick) findViewById(R.id.directionJoystickRight);

        //按钮事件监听
        btn_fly_back.setOnClickListener(new controlFly());
        btn_fly_start.setOnClickListener(new controlFly());
        btn_execute.setOnClickListener(new controlFly());
        btn_upload.setOnClickListener(new controlFly());
        screenJoystickLeft.setJoystickListener(new OnScreenJoystickListener() {
            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }
                if (mobileRemoteController != null) {
                    mobileRemoteController.setLeftStickHorizontal(pX);
                    mobileRemoteController.setLeftStickVertical(pY);
                }else
                    myToast("mobileRemoteController为空");
            }
        });

        screenJoystickRight.setJoystickListener(new OnScreenJoystickListener() {
            @Override
            public void onTouch(OnScreenJoystick joystick, float pX, float pY) {
                if (Math.abs(pX) < 0.02) {
                    pX = 0;
                }

                if (Math.abs(pY) < 0.02) {
                    pY = 0;
                }
                if (mobileRemoteController != null) {
                        mobileRemoteController.setRightStickHorizontal(pX);
                        mobileRemoteController.setRightStickVertical(pY);
                        Log.i("controll", "Horizontal" + mobileRemoteController.getRightStickHorizontal() + "------" + mobileRemoteController.getRightStickVertical());
                }else
                    myToast("mobileRemoteController为空");
            }
        });

        //初始化相关类
        flightBase = new FlightBase();
        try{
            djisdkManager = DJISDKManager.getInstance();
            baseProduct = djisdkManager.getProduct();
            mobileRemoteController = ((Aircraft)baseProduct).getMobileRemoteController();
        }catch (Exception e){
            myToast("初始化DJISDK异常："+ e.getMessage());
        }finally {
            if(djisdkManager == null){
                myToast("djisdkManager获取失败");
            }else if(baseProduct == null)
                myToast("baseProduct获取失败");
            else if(mobileRemoteController == null)
                myToast("mobileRemoteController获取失败");
            else if(baseProduct.isConnected())
                myToast("连接成功");
            else
                myToast("连接失败");
        }

        //获取无人机视频
        mReceivedVideoDataCallBack = new VideoFeeder.VideoDataCallback(){

            @Override
            public void onReceive(byte[] videoBuffer, int size) {
                if(mCodecManager != null){
                    mCodecManager.sendDataToDecoder(videoBuffer,size);
                }
            }
        };

        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);

        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null) {
            mCodecManager.cleanSurface();
            mCodecManager = null;
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        runOnUiThread(new Runnable() {
            public void run() {
            }
        });
    }

    //内部类，实现按钮点击事件监听
    private class controlFly implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                //起飞
                case R.id.btn_start_fly:
                    //TODO
                    flightBase.takeoff(baseProduct);
                    break;
                //着陆
                case R.id.btn_end_fly:
                    //TODO
                    flightBase.land(baseProduct);
                    break;
                //创建任务并load
                case R.id.btn_execute:
                    flightBase.load();
                    break;
                //上传任务
                case R.id.btn_upload:
                    flightBase.upload();
                    break;
            }
        }
    }


    @Override
    protected void onResume() {
        //设置为横屏
        if(getRequestedOrientation()!= ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        super.onResume();
    }

    private void myToast(String s){
        Toast.makeText(getApplication(),s,Toast.LENGTH_LONG).show();
    }
}
