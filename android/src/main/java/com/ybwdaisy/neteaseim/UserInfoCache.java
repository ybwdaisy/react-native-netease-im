package com.ybwdaisy.neteaseim;

import android.text.TextUtils;

import com.netease.nimlib.sdk.InvocationFuture;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.friend.model.Friend;
import com.netease.nimlib.sdk.uinfo.UserService;
import com.netease.nimlib.sdk.uinfo.UserServiceObserve;
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserInfoCache {
	private final static String TAG = "UserInfoCache";
	private Map<String, NimUserInfo> account2UserMap = new ConcurrentHashMap<>();

	static class InstanceHolder {
		final static UserInfoCache instance = new UserInfoCache();
	}

	public static UserInfoCache getInstance() {
		return InstanceHolder.instance;
	}

	public void build() {
		List<NimUserInfo> users = NIMClient.getService(UserService.class).getAllUserInfo();
		updateUsers(users);
		registerObservers(true);
	}

	public void registerObservers(boolean register) {
		NIMClient.getService(UserServiceObserve.class).observeUserInfoUpdate(userInfoUpdateObserver, register);
	}

	public void clear() {
		account2UserMap.clear();
		registerObservers(false);
	}

	// ******************************* 业务接口（获取缓存信息） *********************************
	private final Observer<List<NimUserInfo>> userInfoUpdateObserver = new Observer<List<NimUserInfo>>() {
		@Override
		public void onEvent(List<NimUserInfo> nimUserInfos) {
			if (nimUserInfos == null || nimUserInfos.isEmpty()) {
				return;
			}
			for (NimUserInfo u : nimUserInfos) {
				account2UserMap.put(u.getAccount(), u);
			}
		}
	};

	public NimUserInfo getUserInfo(String account) {
		if (!hasUser(account)) {
			getUserInfoFromRemote(account);
			return null;
		}

		return account2UserMap.get(account);
	}

	public boolean hasUser(String account) {
		if (TextUtils.isEmpty(account) || account2UserMap == null) {
			return false;
		}

		return account2UserMap.containsKey(account);
	}

	public String getUserDisplayName(String account) {
		String alias = getAlias(account);
		if (!TextUtils.isEmpty(alias)) {
			return alias;
		}

		return getUserName(account);
	}

	public String getAvatar(String account) {
		NimUserInfo userInfo = getUserInfo(account);
		if (userInfo != null && !TextUtils.isEmpty(userInfo.getAvatar())) {
			return userInfo.getAvatar();
		} else {
			return "";
		}
	}

	public String getExtension(String account) {
		NimUserInfo userInfo = getUserInfo(account);
		if(userInfo != null && !TextUtils.isEmpty(userInfo.getExtension())) {
			return userInfo.getExtension();
		}else{
			return null;
		}
	}

	public String getAlias(String account) {
		Friend friend = FriendCache.getInstance().getFriendByAccount(account);
		if (friend != null && !TextUtils.isEmpty(friend.getAlias())) {
			return friend.getAlias();
		}
		return null;
	}

	public String getUserName(String account) {
		NimUserInfo userInfo = getUserInfo(account);
		if (userInfo != null && !TextUtils.isEmpty(userInfo.getName())) {
			return userInfo.getName();
		} else {
			return account;
		}
	}

	public String getUserDisplayNameEx(String account) {
		if (TextUtils.equals(account, LoginService.getInstance().getAccount())) {
			return "我";
		}

		return getUserDisplayName(account);
	}

	public String getUserDisplayNameYou(String account) {
		if (TextUtils.equals(account, LoginService.getInstance().getAccount())) {
			return "你";
		}

		return getUserDisplayName(account);
	}

	// ******************************* 工具方法 *********************************

	public void getUserInfoFromRemote(final String account) {
		if (TextUtils.isEmpty(account)) {
			return;
		}

		List<String> accounts = new ArrayList<>(1);
		accounts.add(account);
		// 请求完成后会走observeUserInfoUpdate回调
		NIMClient.getService(UserService.class).fetchUserInfo(accounts);
	}

	private void updateUsers(final List<NimUserInfo> users) {
		if (users == null || users.isEmpty()) {
			return;
		}

		for (NimUserInfo u : users) {
			account2UserMap.put(u.getAccount(), u);
		}
	}
}
