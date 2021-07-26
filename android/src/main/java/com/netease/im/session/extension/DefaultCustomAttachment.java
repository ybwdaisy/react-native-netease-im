package com.netease.im.session.extension;

import com.alibaba.fastjson.JSONObject;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.netease.im.ReactNativeJson;

import org.json.JSONException;

public class DefaultCustomAttachment extends CustomAttachment {

    final static String KEY_RECENT = "recentContent";
    private String recentContent; // 最近会话显示的内容
    private String customData; // 自定义数据

    public DefaultCustomAttachment() {
        super(CustomAttachmentType.Custom);
    }

    @Override
    protected void parseData(JSONObject data) {
        this.recentContent = data.getString(KEY_RECENT);
        this.customData = data.toJSONString();
    }

    @Override
    protected JSONObject packData() {
        JSONObject data = null;
        try {
            data = JSONObject.parseObject(customData);
            if(data == null){
                data = new JSONObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public void setCustomData(ReadableMap map) {
        try {
            JSONObject object = ReactNativeJson.convertMapToJson(map);
            if (object == null) {
                object = new JSONObject();
            }
            this.customData = object.toJSONString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setRecentContent(String recentContent) {
        this.recentContent = recentContent;
    }

    public String getRecentContent() {
        return recentContent;
    }

    @Override
    public WritableMap toReactNative() {
        WritableMap map = new WritableNativeMap();
        JSONObject data = JSONObject.parseObject(customData);
        try {
            map = ReactNativeJson.convertJsonToMap(data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return map;
    }


}
