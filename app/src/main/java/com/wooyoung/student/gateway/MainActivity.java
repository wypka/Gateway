package com.wooyoung.student.gateway;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.wooyoung.student.gateway.Model.Sensor;
import com.wooyoung.student.gateway.SensorApi.SensorApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import retrofit2.Call;


public class MainActivity extends AppCompatActivity implements CameraFragment.CameraListner{

    final static int REQUEST_ENABLE_BLE =1;
    final static String TAG = "IOT Project";
    BluetoothAdapter bluetoothAdapter;
    HashMap<String, BluetoothDevice> btDevices = new HashMap<>();
    BluetoothDevice selectedDevice;
    BluetoothSocket socket;
    PrintWriter out;
    Thread workerThread;
    Sensor sensor;
    String fileName ="";


    private void mapping(Sensor sensor) {
        int flameValue = Integer.parseInt(sensor.getFlame());
        if(fileName == ""){
            sensor.setCamFile("noFile");
        }else{
            sensor.setCamFile(fileName);
            Log.d("사진",sensor.getCamFile());
        }
        if(flameValue>100){
            sensor.setFlameAck("y");
        }
        Gson gson = new Gson();
        Log.d("센서값",gson.toJson(sensor));
        image.setText(sensor.getCamFile());
        memId.setText(sensor.getMemId());
        flame.setText(sensor.getFlame());
        temperature.setText(sensor.getTemperature());
        flameAck.setText(sensor.getFlameAck());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년mm월dd일_hh시mm분ss초");
        sendDate.setText(sdf.format(sensor.getSensingTime()).toString());

    }
    boolean previewMode = false;

    @Override
    public void onCameraCapture(File imageFile) {
        fragmentView.setVisibility(View.INVISIBLE);
        CameraFragment f = getCameraFragment();
        f.stopPreveiw();
        previewMode = false;
        fileName=imageFile.getName();
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
    }
    protected CameraFragment getCameraFragment() {
        FragmentManager fm = getSupportFragmentManager();
        CameraFragment fragment =(CameraFragment) fm.findFragmentById(R.id.cameraContainer);
        return fragment;
    }

    class ReciveThread extends Thread{
        int flameValue;
        String message;
        BufferedReader br;
        boolean fireChk;
        //카메라 동작함수
        public void captureStart() {
            if(previewMode) return;//이문장은왜있는지 모르겠다.
            previewMode =  true;
            CameraFragment f = getCameraFragment();
            fragmentView.setVisibility(View.VISIBLE);
            f.startPreveiw();
            f.capture(2);
        }
        //생성자 : Input 스트림을 BufferedReader 로 변환
        ReciveThread(InputStream inputStream){
            br = new BufferedReader(new InputStreamReader(inputStream));
        }
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
        public void run() {
            while(!Thread.currentThread().isInterrupted()){
                try{
                    message = br.readLine();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //ui 갱신
                            sensor = Sensor.parseJson(message);
                            Gson gson = new Gson();
                            mapping(sensor);
                            Log.d(TAG,gson.toJson(sensor));
                            flameValue=Integer.parseInt(sensor.getFlame());
                            fireChk = flameValue > 100;
                            if(fireChk){
                                if(!previewMode){
                                    captureStart();
                                }
                            }
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    sendValue(sensor);
                                    fileName="noFile";
                                }
                            }).start();
                        }
                    });
                }catch (IOException e){
                    finish();
                }
            }
        }


    }



    public void connectToSelectedDevice(String deviceName){
        //블루투스 연결시도
        Toast.makeText(this, deviceName, Toast.LENGTH_SHORT).show();
        //해시맵에서 장치 추출
        selectedDevice = btDevices.get(deviceName);

        //ANR 을 피하기 위해 스레드로 접속 시도
        new Thread(){
            public void run(){
                try {
                    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                    socket = selectedDevice.createRfcommSocketToServiceRecord(uuid);
                    //RFCOMM채널을 통해 연결
                    socket.connect();
                    Log.d(TAG, "블루투스 연결 성공");
                    //데이터 송수신 스트림 얻기
                    OutputStream outputStream = socket.getOutputStream();
                    InputStream inputStream = socket.getInputStream();
                    new ReciveThread(inputStream).start();


                    out = new PrintWriter(new OutputStreamWriter(outputStream));
                }catch (Exception e){
                    Log.d(TAG,"연결실패",e);

                    finish();
                }
            }
        }.start();
    }
    public void selectDevice(){
        Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();
        if(devices.size() ==0 ){
            Toast.makeText(this, "연결된 블루투스가 장비가 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
        //장치명 쌍으로 해시맵구성
        for(BluetoothDevice device : devices){
            btDevices.put(device.getName(), device);
        }

        // 블루투스 장치맵 으로 배열 만들기 AlertDialog에서 사용
        final CharSequence[] items = btDevices.keySet().toArray(
                new CharSequence[btDevices.keySet().size()+1]);

        items[items.length - 1 ] = "취소";

        new AlertDialog.Builder(this)
                .setTitle("블루투스 장치선택")
                .setItems(items, new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        if(i == btDevices.size() ){
                            Toast.makeText(getApplicationContext(), "취소를 선택", Toast.LENGTH_SHORT).show();
                            finish();
                        }else{
                            //장치선택 -연결시도
                            connectToSelectedDevice(items[i].toString());
                        }
                    }
                })
                .setCancelable(false)
                .show();
    }
    // *********************************************************************************************
    // 어플 생성시
    View fragmentView;
    TextView memId ;
    TextView flame ;
    TextView temperature ;
    TextView flameAck ;
    TextView sendDate ;
    TextView image ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        memId = (TextView)findViewById(R.id.memId);
        flame = (TextView)findViewById(R.id.flameValue);
        temperature = (TextView)findViewById(R.id.temperatureValue);
        flameAck = (TextView)findViewById(R.id.flameChk);
        sendDate = (TextView)findViewById(R.id.sendDate);
        image = (TextView)findViewById(R.id.camFile);
        fragmentView = findViewById(R.id.cameraContainer);
        fragmentView.setVisibility(View.INVISIBLE);
        checkBluetooth();
    }
    // *********************************************************************************************
    public void checkBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){ //블루투스를 지원하지 않는경우.
            Toast.makeText(this, "블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }else{ //장치가 블루투스를 지원하는 경우.
            if (!bluetoothAdapter.isEnabled()){
                Intent blEnableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(blEnableIntent, REQUEST_ENABLE_BLE);
            }else{
                selectDevice();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode){
            case  RESULT_OK :
                switch (requestCode) {
                    case  REQUEST_ENABLE_BLE :
                        selectDevice();
                        break;
                }
            default :
                Toast.makeText(this, "블루투스가 비활성화 상태입니다.", Toast.LENGTH_SHORT).show();
                finish();
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            if(workerThread != null){
                workerThread.interrupt();
            }
            socket.close();
        }catch (Exception e){

        }
    }


}
