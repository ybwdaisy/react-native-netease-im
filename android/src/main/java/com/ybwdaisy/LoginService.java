package com.ybwdaisy;

import android.content.Context;

import com.netease.nimlib.sdk.AbortableFuture;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.AuthServiceObserver;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.auth.constant.LoginSyncStatus;

public class LoginService {
	final static String TAG = "LoginService";
	private String account;
	private String token;
	private AbortableFuture<LoginInfo> loginInfoFuture;

	private LoginService() {}

	static class InstanceHolder {
		final static LoginService instance = new LoginService();
	}

	public static LoginService getInstance() {
		return InstanceHolder.instance;
	}

	public LoginInfo getLoginInfo(Context context) {
		LoginInfo info = new LoginInfo(account, token);
		return info;
	}

	public String getAccount() {
		return account;
	}

	public void login(final LoginInfo loginInfo, final RequestCallback<LoginInfo> callback) {
		NIMClient.getService(AuthService.class).openLocalCache(loginInfo.getAccount());
		loginInfoFuture = NIMClient.getService(AuthService.class).login(loginInfo);
		loginInfoFuture.setCallback(new RequestCallback<LoginInfo>() {
			@Override
			public void onSuccess(LoginInfo loginInfo) {
				account = loginInfo.getAccount();
				token = loginInfo.getToken();
				if (callback != null) {
					callback.onSuccess(loginInfo);
				}
				loginInfoFuture = null;
			}

			@Override
			public void onFailed(int code) {
				if (callback != null) {
					callback.onFailed(code);
				}
				loginInfoFuture = null;
			}

			@Override
			public void onException(Throwable exception) {
				if (callback != null) {
					callback.onException(exception);
				}
				loginInfoFuture = null;
			}
		});
		registerObserver(true);
	}
	public void logout() {
		NIMClient.getService(AuthService.class).logout();//退出服务
	}

	private void registerObserver(Boolean register) {
		NIMClient.getService(AuthServiceObserver.class).observeOnlineStatus(new Observer<StatusCode>() {
			public void onEvent(StatusCode status) {
				String desc = status.getDesc();
				if (status.wontAutoLogin()) {
					//
				}
			}
		}, register);

		NIMClient.getService(AuthServiceObserver.class).observeLoginSyncDataStatus(new Observer<LoginSyncStatus>() {
			public void onEvent(LoginSyncStatus status) {
				if (status == LoginSyncStatus.BEGIN_SYNC) {
					//
				} else if (status == LoginSyncStatus.SYNC_COMPLETED) {
					//
				}
			}
		}, register);
	}
}
