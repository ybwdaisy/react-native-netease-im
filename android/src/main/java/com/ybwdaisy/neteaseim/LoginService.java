package com.ybwdaisy.neteaseim;

import android.content.Context;
import android.util.Log;

import com.netease.nimlib.sdk.AbortableFuture;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.AuthServiceObserver;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.auth.constant.LoginSyncStatus;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.SystemMessageObserver;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.msg.model.SystemMessage;

import java.util.ArrayList;
import java.util.List;

public class LoginService {
	private final static String TAG = "LoginService";
	private String account;
	private String token;

	static class InstanceHolder {
		final static LoginService instance = new LoginService();
	}

	public static LoginService getInstance() {
		return InstanceHolder.instance;
	}

	public String getAccount() {
		return account;
	}

	public LoginInfo getLoginInfo() {
		return new LoginInfo(account, token);
	}

	public void login(final LoginInfo loginInfo, final RequestCallback<LoginInfo> callback) {
		NIMClient.getService(AuthService.class).login(loginInfo).setCallback(new RequestCallback<LoginInfo>() {
			@Override
			public void onSuccess(LoginInfo result) {
				account = result.getAccount();
				token = result.getToken();
				if (callback != null) {
					callback.onSuccess(result);
				}
				// 获取最近联系人列表，提前缓存用户信息
				startSyncRecentContacts();
				registerObserver(true);
			}

			@Override
			public void onFailed(int code) {
				if (callback != null) {
					callback.onFailed(code);
				}
				registerObserver(false);
			}

			@Override
			public void onException(Throwable exception) {
				if (callback != null) {
					callback.onException(exception);
				}
				registerObserver(false);
			}
		});
	}
	public void logout() {
		NIMClient.getService(AuthService.class).logout();//退出服务
		//清除缓存
		UserInfoCache.getInstance().clear();
		FriendCache.getInstance().clear();
		//清除账号
		account = null;
		token = null;
		registerObserver(false);
	}
	
	private void startSyncRecentContacts() {
		NIMClient.getService(MsgService.class).queryRecentContacts().setCallback(new RequestCallbackWrapper<List<RecentContact>>() {
			@Override
			public void onResult(int code, List<RecentContact> recentContacts, Throwable exception) {
				Object recents = ReactCache.createRecentList(recentContacts);
				Log.i(TAG, "recents: " + recents);
			}
			@Override
			public void onFailed(int code) {
				Log.i(TAG, "获取最近会话失败");
			}
		});
	}

	private void registerObserver(Boolean register) {
		// 登录状态
		NIMClient.getService(AuthServiceObserver.class).observeOnlineStatus(new Observer<StatusCode>() {
			public void onEvent(StatusCode status) {
				Log.i(TAG, "onlineStatus: " + status);
				ReactCache.emit(MessageConstant.Event.observeOnlineStatus, status.getValue());
			}
		}, register);

		NIMClient.getService(AuthServiceObserver.class).observeLoginSyncDataStatus(new Observer<LoginSyncStatus>() {
			public void onEvent(LoginSyncStatus status) {
				Log.i(TAG, "loginSyncStatus: " + status);
				ReactCache.emit(MessageConstant.Event.observeLoginSyncDataStatus, status);
			}
		}, register);

		// 系统消息
		NIMClient.getService(SystemMessageObserver.class).observeUnreadCountChange(new Observer<Integer>() {
			@Override
			public void onEvent(Integer unreadCount) {
				ReactCache.emit(MessageConstant.Event.observeSysUnreadCount, unreadCount);
			}
		}, register);

		NIMClient.getService(SystemMessageObserver.class).observeReceiveSystemMsg(new Observer<SystemMessage>() {
			@Override
			public void onEvent(SystemMessage systemMessage) {
				List<SystemMessage> list = new ArrayList<>();
				list.add(systemMessage);
				ReactCache.emit(MessageConstant.Event.observeReceiveSystemMsg, ReactCache.createSystemMsg(list));
			}
		}, register);
	}
}
