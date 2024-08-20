package com.ybwdaisy;

import android.content.Context;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.SDKOptions;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.lifecycle.SdkLifecycleObserver;
import com.netease.nimlib.sdk.mixpush.MixPushConfig;
import com.netease.nimlib.sdk.msg.MsgService;
import com.ybwdaisy.Attachment.CustomAttachParser;

public class IMApplication {

	private static Context context;

	public static void init(Context context, MixPushConfig mixPushConfig, String appKey) {
		context = context.getApplicationContext();
		LoginInfo loginInfo = LoginService.getInstance().getLoginInfo(context);
		SDKOptions sdkOptions = getOptions(appKey, mixPushConfig);
		NIMClient.init(context, loginInfo, sdkOptions);
		//注册附件解析器
		NIMClient.getService(MsgService.class).registerCustomAttachmentParser(new CustomAttachParser());
		buildCache();
		registerObserver();
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

	private static void registerObserver() {
		NIMClient.getService(SdkLifecycleObserver.class).observeMainProcessInitCompleteResult(new Observer<Boolean>() {
			@Override
			public void onEvent(Boolean aBoolean) {
				if (aBoolean != null && aBoolean) {
					ReactCache.emit(MessageConstant.Event.observeSDKInit, null);
				}
			}
		}, true);
	}
}
