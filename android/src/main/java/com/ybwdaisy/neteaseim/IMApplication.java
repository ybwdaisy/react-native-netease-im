package com.ybwdaisy.neteaseim;

import android.content.Context;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.SDKOptions;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.mixpush.MixPushConfig;
import com.netease.nimlib.sdk.msg.MsgService;
import com.ybwdaisy.neteaseim.Attachment.CustomAttachParser;

import java.util.Map;

public class IMApplication {

	private static Context context;

	public static void init(Context context, String appKey, MixPushConfig mixPushConfig, Map<String, Object> pushPayload) {
		context = context.getApplicationContext();
		LoginInfo loginInfo = LoginService.getInstance().getLoginInfo(context);
		SDKOptions sdkOptions = getOptions(appKey, mixPushConfig);
		NIMClient.init(context, loginInfo, sdkOptions);
		//设置推送参数
		SessionService.getInstance().setPushPayload(pushPayload);
		//注册附件解析器
		NIMClient.getService(MsgService.class).registerCustomAttachmentParser(new CustomAttachParser());
		//缓存
		buildCache();
	}

	private static SDKOptions getOptions(String appKey, MixPushConfig mixPushConfig) {
		SDKOptions options = new SDKOptions();
		options.appKey = appKey;
		options.databaseEncryptKey = "GENEBOX";
		options.mixPushConfig = mixPushConfig;
		return options;
	}

	private static void buildCache() {
		UserInfoCache.getInstance().build();
		FriendCache.getInstance().build();
	}
}
