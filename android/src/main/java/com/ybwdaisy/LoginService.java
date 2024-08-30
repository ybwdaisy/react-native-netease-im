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
	private AbortableFuture<LoginInfo> loginInfoFuture;

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
				registerObserver(true);
				loginInfoFuture = null;
			}

			@Override
			public void onFailed(int code) {
				if (callback != null) {
					callback.onFailed(code);
				}
				registerObserver(false);
				loginInfoFuture = null;
			}

			@Override
			public void onException(Throwable exception) {
				if (callback != null) {
					callback.onException(exception);
				}
				registerObserver(false);
				loginInfoFuture = null;
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

	private void registerObserver(Boolean register) {
		// 登录状态
		AuthServiceObserver authServiceObserver = NIMClient.getService(AuthServiceObserver.class);
		authServiceObserver.observeOnlineStatus(new Observer<StatusCode>() {
			public void onEvent(StatusCode status) {
				ReactCache.emit(MessageConstant.Event.observeOnlineStatus, status.getValue());
			}
		}, register);

		authServiceObserver.observeLoginSyncDataStatus(new Observer<LoginSyncStatus>() {
			public void onEvent(LoginSyncStatus status) {
				ReactCache.emit(MessageConstant.Event.observeLoginSyncDataStatus, status);
			}
		}, register);

		// 会话消息
		MsgServiceObserve msgServiceObserve = NIMClient.getService(MsgServiceObserve.class);
		msgServiceObserve.observeReceiveMessage(new Observer<List<IMMessage>>() {
			@Override
			public void onEvent(List<IMMessage> imMessages) {
				ReactCache.emit(MessageConstant.Event.observeReceiveMessage, ReactCache.createMessageList(imMessages));
			}
		}, register);
		msgServiceObserve.observeRecentContact(new Observer<List<RecentContact>>() {
			@Override
			public void onEvent(List<RecentContact> recentContacts) {
				if (recentContacts != null && !recentContacts.isEmpty()) {
					ReactCache.emit(MessageConstant.Event.observeRecentContact, ReactCache.createRecentList(recentContacts));
				}
			}
		}, register);

		// 系统消息
		SystemMessageObserver systemMessageObserver = NIMClient.getService(SystemMessageObserver.class);
		systemMessageObserver.observeUnreadCountChange(new Observer<Integer>() {
			@Override
			public void onEvent(Integer unreadCount) {
				ReactCache.emit(MessageConstant.Event.observeSysUnreadCount, unreadCount);
			}
		}, register);
		systemMessageObserver.observeReceiveSystemMsg(new Observer<SystemMessage>() {
			@Override
			public void onEvent(SystemMessage systemMessage) {
				List<SystemMessage> list = new ArrayList<>();
				list.add(systemMessage);
				ReactCache.emit(MessageConstant.Event.observeReceiveSystemMsg, ReactCache.createSystemMsg(list));
			}
		}, register);
	}
}
