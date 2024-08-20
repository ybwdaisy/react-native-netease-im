package com.ybwdaisy.Attachment;

import com.alibaba.fastjson.JSONObject;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.ybwdaisy.ReactNativeJson;

import org.json.JSONException;

public class DefaultCustomAttachment extends CustomAttachment {
	private String recentContent;
	private String customData;

	public DefaultCustomAttachment() {
		super("custom");
	}

	public void setCustomData(ReadableMap map) {
		try {
			JSONObject object = ReactNativeJson.convertMapToJson(map);
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
	protected void parseData(JSONObject data) {
		this.customData = data.toJSONString();
	}

	@Override
	protected JSONObject packData() {
		JSONObject data = null;
		data = JSONObject.parseObject(customData);
		if (data == null){
			data = new JSONObject();
		}
		return data;
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
