package com.wooyoung.student.gateway.Model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Date;

import static com.wooyoung.student.gateway.R.id.flameValue;


/**
 * Created by student on 2017-12-04.
 */

public class Sensor {
    private String memId;
    private String flame;
    private String temperature; 
    private String flameAck;
    private Date sensingTime;
    private String camFile;


    String jsonString;
    public String getCamFile() {
        return camFile;
    }

    public void setCamFile(String camFile) {
        this.camFile = camFile;
    }


    public Sensor(String memId, String flame, String temperature, String flameAck) {
        this.memId = memId;
        this.flame = flame;
        this.temperature = temperature;
        this.flameAck = flameAck;
        this.sensingTime = new Date();
        this.camFile = "";
    }

    public Sensor(String memId) {
        this.memId = memId;
    }

    public String getMemId() {
        return memId;
    }

    public void setMemId(String memId) {
        this.memId = memId;
    }

    public String getFlame() {
        return flame;
    }

    public void setFlame(String flame) {
        this.flame = flame;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getFlameAck() {
        return flameAck;
    }

    public void setFlameAck(String flameAck) {
        this.flameAck = flameAck;
    }

    public Date getSensingTime() {
        return sensingTime;
    }

    public void setSensingTime(Date sensingTime) {
        this.sensingTime = sensingTime;
    }

    public static Sensor parseJson(String jsonString){
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(jsonString);
        JsonObject jObj = element.getAsJsonObject();
        String memId = jObj.get("memId").getAsString();
        String flame = jObj.get("flame").getAsString();
        String temperature = jObj.get("temperature").getAsString();
        String flameAck = jObj.get("flameAck").getAsString();
        Sensor sensor = new Sensor(memId,flame, temperature,flameAck);
        return sensor;
    }
}
