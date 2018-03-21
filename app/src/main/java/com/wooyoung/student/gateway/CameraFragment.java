package com.wooyoung.student.gateway;


import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.wooyoung.student.gateway.Model.Sensor;
import com.wooyoung.student.gateway.SensorApi.CameraUse;
import com.wooyoung.student.gateway.SensorApi.SensorApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



/**
 * A simple {@link Fragment} subclass.
 */
public class CameraFragment extends Fragment implements CameraUse,SurfaceHolder.Callback{

    Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;
    public CameraFragment() {

    }
    @Override
    public void onResume() {
        super.onResume();
        camera = camera.open(0);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(camera != null){
            camera.release();
            camera = null;
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            activityCallback = (CameraListner)context;
        }catch(ClassCastException e){
            throw new ClassCastException(context.toString()+"must implement ToolbarListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        surfaceView = (SurfaceView)inflater.inflate(R.layout.fragment_camera, container, false);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        return surfaceView;
    }
    @Override
    public String createFileName() {
        String timeStamp = new SimpleDateFormat("yyyymmdd_HHmmss")
                .format(new Date());
        String imageFileName = "JPEG_"+ timeStamp + ".jpg";
        return imageFileName;
    }
    Camera.PictureCallback saveJpg = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] bytes, Camera camera) {
            String fname = createFileName();
            File dir = Environment.getExternalStorageDirectory();
            File file = new File(dir, fname);
            try(OutputStream os = new FileOutputStream(file)) {
                //사진파일을 만든다.
                os.write(bytes);

                activityCallback.onCameraCapture(file);
                uploadImage(file);
            } catch(Exception e) {
                Log.d("에러 ", "파일 저정 실패 ", e);
            }
        }
    };
    public void sendValue(Sensor sensor){
        SensorApi service = SensorApi.retrofit.create(SensorApi.class);
        Call<Boolean> call = service.insert(sensor);
        try {
            Boolean result = call.execute().body();
        } catch (IOException e) {
            Log.d("실패",e.getMessage());
        }
    }
    @Override
    public void capture(int second) {
        int time = second * 1000;
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                camera.takePicture(null, null, saveJpg);
            }
        };
        timer.schedule(timerTask, time);

    }

    static final String MULTIPART = "multi_part/form_data";
    @Override
    public void uploadImage(File file) {
        SensorApi service = SensorApi.retrofit.create(SensorApi.class);
        RequestBody rbFile = RequestBody.create(
                MediaType.parse(MULTIPART),//파일타입
                file//담는파일
        );
        MultipartBody.Part body =
                MultipartBody.Part.createFormData(
                        "camFile", //받는 파일명
                        file.getName(), //오리지날 파일이름
                        rbFile //파일생성정보
                );
        Call<ResponseBody> call = service.upload(body);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Log.v("upload","sucess");
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.v("upload error",t.getMessage());
            }
        });
    }

    @Override //surface 생성시
    public void surfaceCreated(SurfaceHolder holder) {
        if(camera != null) {
            try {
                int int_cameraID = 0;
            /* 카메라가 여러개 일 경우 그 수를 가져옴  */
               /* int numberOfCameras = Camera.getNumberOfCameras();
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();

                for(int i=0; i < numberOfCameras; i++)
                {
                    Camera.getCameraInfo(i, cameraInfo);
                    // 전면카메라
                    //                if(cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT)
                    //                    int_cameraID = i;
                    // 후면카메라
                    if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK)
                        int_cameraID = i;
                }
                */
                camera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                Log.d("에러",  "surfaceCreated", e);
            }
        }
    }

    @Override //미리보기 활성
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> sizeList = parameters.getSupportedPictureSizes();
        Camera.Size s = getPreviewSize(sizeList);
        parameters.setPreviewSize(s.width/4, s.height/4);
        camera.setParameters(parameters);
    }
    protected Camera.Size getPreviewSize(List<Camera.Size> list) {
        Camera.Size bestSize = list.get(0);
        int lagestArea = bestSize.width * bestSize.height;

        for(Camera.Size size : list) {
            int area = size.width * size.height;
            if(area > lagestArea) {
                bestSize = size;
                lagestArea = area;
            }
        }
        return bestSize;
    }
    @Override //카메라 끄기.
    public void surfaceDestroyed(SurfaceHolder holder) {stopPreveiw();}
    @Override
    public void onDetach() {
        super.onDetach();
        activityCallback = null;
    }
    public void startPreveiw() {
        if(camera != null) {
            camera.startPreview();
        }
    }

    public void stopPreveiw() {
        if(camera != null) {
            camera.stopPreview();
        }
    }
    // *********************************************************************************************
    // 액티비에 값 전송 인터페이스
    // *********************************************************************************************
    CameraListner activityCallback;
    public interface CameraListner{
        void onCameraCapture(File imageFile);
    }
}
