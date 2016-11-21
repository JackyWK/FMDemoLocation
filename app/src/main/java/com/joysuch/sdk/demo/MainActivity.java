package com.joysuch.sdk.demo;


import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.fengmap.android.FMErrorMsg;
import com.fengmap.android.analysis.navi.FMNaviAnalyser;
import com.fengmap.android.analysis.navi.FMNaviResult;
import com.fengmap.android.analysis.search.FMSearchAnalyser;
import com.fengmap.android.analysis.search.FMSearchResult;
import com.fengmap.android.analysis.search.model.FMSearchModelByKeywordRequest;
import com.fengmap.android.data.FMDataManager;
import com.fengmap.android.map.FMGroupInfo;
import com.fengmap.android.map.FMMap;
import com.fengmap.android.map.FMMapExtent;
import com.fengmap.android.map.FMMapInfo;
import com.fengmap.android.map.FMMapView;
import com.fengmap.android.map.event.OnFMMapInitListener;
import com.fengmap.android.map.geometry.FMMapCoord;
import com.fengmap.android.map.layer.FMImageLayer;
import com.fengmap.android.map.layer.FMLineLayer;
import com.fengmap.android.map.layer.FMLocationLayer;
import com.fengmap.android.map.marker.FMImageMarker;
import com.fengmap.android.map.marker.FMLineMarker;
import com.fengmap.android.map.marker.FMLocationMarker;
import com.fengmap.android.map.marker.FMSegment;
import com.fengmap.android.map.style.FMImageMarkerStyle;
import com.fengmap.android.map.style.FMLineMarkerStyle;
import com.fengmap.android.map.style.FMLocationMarkerStyle;
import com.fengmap.android.utils.FMLog;
import com.joysuch.sdk.IndoorLocateListener;
import com.joysuch.sdk.locate.JSLocateManager;
import com.joysuch.sdk.locate.JSPosition;

import java.util.ArrayList;

/**
 * The type Main activity.
 */
public class MainActivity extends Activity implements OnClickListener {
    private FMMapView mapView;                    //地图容器对象
    private FMMap map;                            //操作地图的对象
    private FMMapInfo scene;                    //整个场景的配置信息
    private FMMapExtent ex;                       //地图范围
    private FMLocationLayer locLayer;             //定位图层
    private ArrayList<FMGroupInfo> groups;
    private Button btLoc;                         //定位图标
    private int focus = 0;                //焦点层

    private FMImageLayer mlForNaviStart;    //标注物图层，起点
    private FMImageLayer mlForNaviEnd;      //标注物图层，终点
    private FMLineLayer lineLayer;                //线图层
    private FMNaviAnalyser naviAnalyser;
    private FMMapCoord startPt;
    private FMMapCoord endPt;              //起点,终点
    private FMMapCoord locPt;
    private FMMapCoord analyserPt;          //点击点，定位点，查询分析点
    private FMSearchAnalyser searchAnalyser;        //搜索分析器
    private int startGroupId;
    private int endGroupId;          //起点，终点所在组Id
    private int locGroupId;
    private int analyserId;            //点击点，定位点，查询分析点所在Id
    private int[] gids;

    private Button btAnalyser;                      //查询分析按钮
    private EditText etKw;                         //搜索框

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去标题
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        initView();
        initMap();
        initJoysuchSDK();

    }

    /*
    *地图初始化
    *@author wangkuan
    *cteate at 2016/11/21 下午4:14
    */
    private void initMap() {
        //拷贝地图
        String dstDir = FMDataManager.getDefaultMapDirectory() + "90254/";
        String dstFileName = "90254.fmap";
        ResourcesUtils.writeRc(this, dstDir, dstFileName, "90254.fmap");

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mapView = (FMMapView) findViewById(R.id.mapview);
        map = mapView.getFMMap();                                 //获取地图操作对象

        String mapId = "90254";                                    //地图id
        map.openMapById(mapId);                                    //打开地图
        map.setOnFMMapInitListener(new OnFMMapInitListener() {   //地图初始化的回调

            @Override
            public void onMapInitSuccess(String path) {
                //场景配置
                scene = map.getFMMapInfo();      //场景配置
                groups = scene.getGroups();
                int floorNum = groups.size();                            //有多少层
                gids = new int[floorNum];
                for (int i = 0; i < floorNum; i++) {
                    gids[i] = groups.get(i).getGroupId();
                }

                map.setMultiDisplay(gids, 1);                //设置多层显示,初始化焦点层
                map.showCompass(true);                         //设置指南针
                ex = map.getFMMapExtent();                   //地图范围

                locLayer = map.getFMLayerProxy().getFMLocationLayer();  //获取定位或者创建定位图层
                locLayer.setVisible(true);
                map.addLayer(locLayer);                                //将定位图层添加到场景

                lineLayer = map.getFMLayerProxy().getFMLineLayer();
                map.addLayer(lineLayer);                                //将路径图层添加到地图
                naviAnalyser = new FMNaviAnalyser(map);                //导航分析器

                searchAnalyser = new FMSearchAnalyser(map);            //搜索分析器
                map.updateMap();
            }

            @Override
            public void onMapInitFailure(String path, int errCode) {
                FMLog.i("FMMap Init", FMErrorMsg.getErrorMsg(errCode));
            }

        });
    }

    /*
    *控件初始化
    *@author wangkuan
    *cteate at 2016/11/21 下午4:11
    */
    private void initView() {
        btLoc = (Button) findViewById(R.id.btLoc);              //添加点位按钮
        btLoc.setOnClickListener(this);

        btAnalyser = (Button) findViewById(R.id.btNav);        //查询分析按钮
        btAnalyser.setOnClickListener(this);

        etKw = (EditText) findViewById(R.id.editText);
    }

    /*
    *真趣SDK的初始化
    *@author wangkuan
    *cteate at 2016/11/21 下午4:06
    */
    private void initJoysuchSDK() {
        //真趣定位SDK初始化
        JSLocateManager.getInstance().init(getApplicationContext());
        JSLocateManager.getInstance().setOnIndoorLocateListener(indoorLocateListener);
        JSLocateManager.getInstance().setOfflineMode();//数据内置APP，使用离线模式
        JSLocateManager.getInstance().setLocateTimesSecond(2);//设置每秒定位次数
        JSLocateManager.getInstance().start();   //启动定位
    }

    /*
    *定位监听器
    *@author wangkuan
    *cteate at 2016/11/21 下午4:17
    */
    private final IndoorLocateListener indoorLocateListener = new IndoorLocateListener() {
        //接收到定位信息时进行实时定位显示
        @Override
        public void onReceivePosition(JSPosition position) {
            // TODO Auto-generated method stub
            // 设置路径导航起点坐标
            //定位地图坐标
            locPt = null;
            locGroupId = 0;
            locPt = new FMMapCoord(ex.getMinX() + position.getxMeters(), ex.getMaxY() - position.getyMeters(), 2.0);
            //定位地图楼层
            locGroupId = floorIDToIndex(position.getFloorID());
            //开始定位
            locating(locPt, locGroupId);
            if (locGroupId == 2) focus = 1;
            else if (locGroupId == 1) focus = 2;
            map.setMultiDisplay(gids, focus);                //设置多层显示,焦点层=定位层
            map.updateMap();
        }
    };

    /*
    *楼层ID转换-定位楼层to地图楼层
    *@author wangkuan
    *cteate at 2016/11/21 下午4:15
    */
    private int floorIDToIndex(int locateFloorID) {
        int mapFloorID = -1;
        String floorName = "";
        if (locateFloorID == 0) {
            Toast.makeText(getApplicationContext(), "定位引擎输出楼层错误，楼层ID=0", Toast.LENGTH_SHORT).show();
        } else if (locateFloorID > 0) {
            floorName = "F" + String.valueOf(locateFloorID);
        } else {
            floorName = "B" + String.valueOf((-1) * locateFloorID);
        }
        for (int i = 0; i < groups.size(); i++) {
            if (floorName.equals(groups.get(i).getGroupName().toUpperCase())) {
                mapFloorID = groups.get(i).getGroupId();
                break;
            }
        }
        return mapFloorID;
    }

    /*
    *进行实时定位
    *@author wangkuan
    *cteate at 2016/11/21 下午4:15
    */
    private void locating(FMMapCoord point, int floor) {
        locLayer.removeAll();
        FMLocationMarkerStyle style = new FMLocationMarkerStyle();
        style.setActiveImageFromAssets("active.png");
        style.setStaticImageFromAssets("static.png");
        FMLocationMarker marker = new FMLocationMarker(floor, point, style);
        locLayer.addMarker(marker);
        map.updateMap();
    }

    /*
    *定位与导航按钮
    *@author wangkuan
    *cteate at 2016/11/21 下午4:17
    */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btLoc:
                if (locPt == null) {
                    Toast.makeText(this, "不在服务区内", Toast.LENGTH_SHORT).show();
                    return;
                }
                JSLocateManager.getInstance().start();
                break;

            case R.id.btNav:   //navigation
                navigation();
                break;
            default:
                break;
        }
    }

    /*
    *路径导航
    *@author wangkuan
    *cteate at 2016/11/21 下午4:17
    */
    private void navigation() {
        if (locPt == null) {
            Toast.makeText(this, "不在服务区内", Toast.LENGTH_SHORT).show();
            return;
        }
        String input_dest = etKw.getText().toString().trim();
        if (input_dest == null || "".equals(input_dest)) {
            Toast.makeText(this, "请输入目的地", Toast.LENGTH_SHORT).show();
            return;
        }
        //查询分析
        ArrayList<FMGroupInfo> groups = scene.getGroups();
        int floorNum = groups.size();                            //有多少层
        for (int groupId = 1; groupId <= floorNum; groupId++) {
            FMSearchModelByKeywordRequest keywordRequest = new FMSearchModelByKeywordRequest(groupId, input_dest.trim());   //关键字搜索
            ArrayList<FMSearchResult> resultSet = searchAnalyser.executeFMSearchRequest(keywordRequest);
            for (FMSearchResult info : resultSet) {         //查询坐标
                int gid = (Integer) info.get("gid");
                analyserPt = searchAnalyser.getModelCoord(groupId, gid);
                analyserId = groupId;
            }
        }
        if (analyserPt == null) {
            Toast.makeText(this, "没有找到目的地", Toast.LENGTH_SHORT).show();
            clear();
            return;
        }

        if (startPt == null) {  //起点
            clear();                           //坐标为空时，清空所有路径和图标
            startPt = locPt;                   //起点坐标=定位坐标
            startGroupId = locGroupId;               //起点楼层=定位楼层
            //添加起点图层
            mlForNaviStart = new FMImageLayer(map, startGroupId);
            map.addLayer(mlForNaviStart);
            //标注物样式
            FMImageMarkerStyle startStyle = new FMImageMarkerStyle();
            //起点图标
            startStyle.setImageFromAssets("ico_start.png");
            addMarker(mlForNaviStart, startPt, startStyle);
        }

        if (endPt == null) {    //终点
            endPt = analyserPt;
            endGroupId = analyserId;
            //添加终点图层
            mlForNaviEnd = new FMImageLayer(map, endGroupId);
            map.addLayer(mlForNaviEnd);
            //标注物样式
            FMImageMarkerStyle endStyle = new FMImageMarkerStyle();
            //终点图标
            endStyle.setImageFromAssets("ico_end.png");
            addMarker(mlForNaviEnd, endPt, endStyle);
        }

        //导航路径
        if (startPt != null && endPt != null) {
            anlyzeNavi(startGroupId, startPt, endGroupId, endPt);
            //画完置空
            startPt = null;
            endPt = null;
            analyserPt = null;
        }

    }

    /**
     * 导航分析
     *
     * @param startGroupId 起点所在层
     * @param startPt      起点坐标
     * @param endGroupId   终点所在层
     * @param endPt        终点坐标
     *                     cteate at 2016/11/21 下午4:18
     */
    private void anlyzeNavi(int startGroupId, FMMapCoord startPt, int endGroupId, FMMapCoord endPt) {
        int type = naviAnalyser.analyzeNavi(startGroupId, startPt, endGroupId, endPt,
                FMNaviAnalyser.FMNaviModule.MODULE_SHORTEST);
        if (type == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_SUCCESS) {
            ArrayList<FMNaviResult> results = naviAnalyser.getNaviResults();
            //画线
            FMLineMarkerStyle lineStyle1 = new FMLineMarkerStyle();
            lineStyle1.setFillColor(Color.RED);
            lineStyle1.setLineWidth(1.5f);
            lineStyle1.setLineMode(FMLineMarker.LineMode.FMLINE_CIRCLE);
            lineStyle1.setLineType(FMLineMarker.LineType.FMLINE_DASHED);

            FMLineMarker line = new FMLineMarker();
            line.setStyle(lineStyle1);

            for (FMNaviResult r : results) {
                FMSegment s = new FMSegment(r.getGroupId(), r.getPointList());
                line.addSegment(s);
            }
            lineLayer.addMarker(line);
            results.clear();
            map.updateMap();

        } else if (type == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_FAILURE_NO_STAIR_FLOORS) {
            Toast.makeText(this, "没有电梯或者扶梯进行跨层导航", Toast.LENGTH_SHORT).show();
        } else if (type == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_FAILURE_NOTSUPPORT_FLOORS) {
            Toast.makeText(this, "不支持跨层导航", Toast.LENGTH_SHORT).show();
        } else if (type == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_PARAM_ERROR) {
            Toast.makeText(this, "导航参数出错", Toast.LENGTH_SHORT).show();
        } else if (type == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_FAILURE_TOO_CLOSE) {
            Toast.makeText(this, "太近了", Toast.LENGTH_SHORT).show();
        } else if (type == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_DATABASE_ERROR) {
            Toast.makeText(this, "数据库出错", Toast.LENGTH_SHORT).show();
        } else if (type == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_FAILURE_NO_END) {
            Toast.makeText(this, "数据出错，终点不在其对应层的数据中", Toast.LENGTH_SHORT).show();
        } else if (type == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_FAILURE_NO_START) {
            Toast.makeText(this, "数据错误，起点不在其对应层的数据中", Toast.LENGTH_SHORT).show();
        } else if (type == FMNaviAnalyser.FMRouteCalcuResult.ROUTE_FAILURE_NO_FMDBKERNEL) {
            Toast.makeText(this, "底层指针错误", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 在指定位置添加标注物到指定的图层。
     *
     * @param layer 图层
     * @param point 地图坐标
     * @return 标注物
     * cteate at 2016/11/21 下午4:19
     */
    private FMImageMarker addMarker(FMImageLayer layer, FMMapCoord point, FMImageMarkerStyle style) {
        //创建标注物
        FMImageMarker marker = new FMImageMarker(point);
        marker.setStyle(style);
        //指针对象
        long handle = layer.addMarker(marker);
        map.updateMap();
        if (handle <= 0)
            return null;
        return marker;
    }

    /**
     * 清除
     *
     * @author wangkuan
     * cteate at 2016/11/21 下午4:50
     */
    private void clear() {
        //清除所有线
        if (lineLayer != null) {
            lineLayer.removeAll();
        }
        //清除标注物
        if (mlForNaviStart != null) {
            mlForNaviStart.removeAll();
            map.removeLayer(mlForNaviStart);  //移除图层
        }
        if (mlForNaviEnd != null) {
            mlForNaviEnd.removeAll();
            map.removeLayer(mlForNaviEnd);    //移除图层
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        map.onDestory();
        this.finish();
        System.exit(0);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        JSLocateManager.getInstance().stop();
    }


}
