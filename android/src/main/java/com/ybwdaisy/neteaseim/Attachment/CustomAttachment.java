package com.ybwdaisy.neteaseim.Attachment;

import com.alibaba.fastjson.JSONObject;
import com.facebook.react.bridge.WritableMap;
import com.netease.nimlib.sdk.msg.attachment.MsgAttachment;

public abstract class CustomAttachment implements MsgAttachment {
	protected String type;

	CustomAttachment(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void fromJson(JSONObject data) {
		if (data != null) {
			parseData(data);
		}
	}

	@Override
	public String toJson(boolean send) {
		return CustomAttachParser.packData(type, packData());
	}

	protected abstract void parseData(JSONObject data);
	protected abstract JSONObject packData();
	protected abstract WritableMap toReactNative();
}
