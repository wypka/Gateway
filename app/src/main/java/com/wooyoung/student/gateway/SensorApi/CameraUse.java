package com.wooyoung.student.gateway.SensorApi;

import android.view.SurfaceHolder;

import java.io.File;
import java.io.IOException;

/**
 * Created by student on 2017-12-12.
 */

public interface CameraUse {
    String createFileName() throws IOException;
    void capture(int second);
    void uploadImage(File file);

}
