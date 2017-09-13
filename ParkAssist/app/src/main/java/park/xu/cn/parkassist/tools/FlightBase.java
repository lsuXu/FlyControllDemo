package park.xu.cn.parkassist.tools;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dji.common.error.DJIError;
import dji.common.gimbal.CapabilityKey;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.sdk.base.BaseProduct;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.mission.MissionControl;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LATITUDE;
import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LONGITUDE;

/**
 * Created by Namuh on 2017/8/31.
 */

public class FlightBase {

    MissionControl missionControl;
    //用于构建，设置任务
    WaypointMission.Builder builder;
    WaypointMissionOperator waypointMissionOperator;

    WaypointMission waypointMission;
    WaypointMissionOperatorListener waypointMissionOperatorListener;
    Waypoint waypoint;
    List<Waypoint> waypointList;

    public void takeoff(BaseProduct baseProduct){
        Aircraft aircraft = (Aircraft)baseProduct;
        aircraft.getFlightController().startTakeoff(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });
    }

    public void land(BaseProduct baseProduct){
        Aircraft aircraft = (Aircraft)baseProduct;
        //开始降落
        aircraft.getFlightController().startLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });
        //确认降落
        //TODO
        aircraft.getFlightController().confirmLanding(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {

            }
        });
    }
    //装载航点任务
    public void load(){
        missionControl = DJISDKManager.getInstance().getMissionControl();
        if(waypointMissionOperator == null)
            waypointMissionOperator = missionControl.getWaypointMissionOperator();
        Log.i("mission","状态1:" + waypointMissionOperator.getCurrentState().getName());
        waypointMission = createWaypointMission();
        //将WaypointMission加载到设备内存中,同时验证mission信息，如果mission不正确，返回错误
        DJIError error = waypointMissionOperator.loadMission(waypointMission);
        if(error != null)
            Log.i("mission","加载任务失败" + error);
        //上传任务
        Log.i("mission","状态2:" + waypointMissionOperator.getCurrentState().getName());
    }

    public void upload(){
        if (WaypointMissionState.READY_TO_RETRY_UPLOAD.equals(waypointMissionOperator.getCurrentState())
                || WaypointMissionState.READY_TO_UPLOAD.equals(waypointMissionOperator.getCurrentState())) {
            //上传任务方法
            waypointMissionOperator.uploadMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    if(djiError != null) {
                        Log.i("mission", "上传失败" + djiError.getDescription() + "重试中。。。");
                        waypointMissionOperator.retryUploadMission(new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if(djiError != null)
                                    Log.i("mission", "重试失败" + djiError.getDescription());
                            }
                        });
                    }
                }
            });
        }else Log.i("mission","不满足上传条件");

        //执行任务
        if (waypointMission != null) {
            waypointMissionOperator.startMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError djiError) {
                    Log.i("mission","2" + djiError.getDescription() + "---状态：" + waypointMissionOperator.getCurrentState().getName());
                }
            });
        }else Log.i("mission","开始失败");
    }

    public WaypointMission createWaypointMission(){
        waypointList = new ArrayList<>();
        double baseLatitude = 22;
        double baseLongitude = 113;
        final float baseAltitude = 30.0f;
        //设置维度
        if (KeyManager.getInstance().getValue((FlightControllerKey.create(HOME_LOCATION_LATITUDE))) != null
                && KeyManager.getInstance()
                .getValue((FlightControllerKey.create(HOME_LOCATION_LATITUDE))) instanceof Double) {
            baseLatitude =
                    (double) KeyManager.getInstance().getValue((FlightControllerKey.create(HOME_LOCATION_LATITUDE)));
        }
        //设置经度
        if (KeyManager.getInstance().getValue((FlightControllerKey.create(HOME_LOCATION_LONGITUDE))) != null
                && KeyManager.getInstance()
                .getValue((FlightControllerKey.create(HOME_LOCATION_LONGITUDE))) instanceof Double) {
            baseLongitude =
                    (double) KeyManager.getInstance().getValue((FlightControllerKey.create(HOME_LOCATION_LONGITUDE)));
        }
        for(int i =0 ; i <3; i++) {
            waypoint = new Waypoint(baseLatitude + 0.00005, baseLongitude + 0.00008, baseAltitude + i*3);
            waypoint.addAction(new WaypointAction(WaypointActionType.STAY, 1));
            waypointList.add(waypoint);
        }

        if(builder == null)
            builder = new WaypointMission.Builder().finishedAction(WaypointMissionFinishedAction.NO_ACTION)
                    .headingMode(WaypointMissionHeadingMode.AUTO)
                    .autoFlightSpeed(5.0f)
                    .maxFlightSpeed(5.0f)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        else
            builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION)//设置飞机结束后不执行任何操作，可由遥控器控制
                    .headingMode(WaypointMissionHeadingMode.AUTO)//设置飞机航向模型
                    .autoFlightSpeed(5.0f)
                    .maxFlightSpeed(5.0f)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);//设置飞机飞行路线为正常模式
        Log.i("mission","经度：" + baseLongitude + "--维度："+ baseLatitude);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        //设置执行任务重复次数，0为执行一次，不重复。1位执行2次
        builder.repeatTimes(0);
        //设置航点
        if(builder != null) {
            builder.waypointList(waypointList).waypointCount(waypointList.size());
        }
        else {
            builder = new WaypointMission.Builder().waypointList(waypointList).waypointCount(waypointList.size());
        }
        if(builder.getWaypointList().size()>0){
            for(int i = 0 ;i<builder.getWaypointList().size();i ++){
                builder.getWaypointList().get(i).altitude = baseAltitude;
            }
        }
        return builder.build();
    }
}
