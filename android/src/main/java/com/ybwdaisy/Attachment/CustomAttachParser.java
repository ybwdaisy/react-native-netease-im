package com.ybwdaisy.Attachment;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netease.nimlib.sdk.msg.attachment.MsgAttachment;
import com.netease.nimlib.sdk.msg.attachment.MsgAttachmentParser;

import org.json.JSONException;

public class CustomAttachParser implements MsgAttachmentParser {
	@Override
	public MsgAttachment parse(String attach) {
		CustomAttachment attachment = null;
		JSONObject object = JSON.parseObject(attach);
		String type = object.getString("type");
		JSONObject data = object.getJSONObject("data");
		attachment = new DefaultCustomAttachment();
		attachment.fromJson(data);

		return attachment;
	}

	public static String packData(String type, JSONObject data) {
		JSONObject object = new JSONObject();
		object.put("type", type);
		if (data != null) {
			object.put("data", data);
		}
		return object.toJSONString();
	}
}
