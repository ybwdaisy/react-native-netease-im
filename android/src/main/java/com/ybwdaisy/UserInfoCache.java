package com.ybwdaisy;

import android.text.TextUtils;

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
	private Map<String, List<RequestCallback<NimUserInfo>>> requestUserInfoMap = new ConcurrentHashMap<>();

	static class InstanceHolder {
		final static UserInfoCache instance = new UserInfoCache();
	}

	public static UserInfoCache getInstance() {
		return InstanceHolder.instance;
	}

	public void build() {
		List<NimUserInfo> users = NIMClient.getService(UserService.class).getAllUserInfo();
		updateUsers(users);
		registerObserver(true);
	}

	public void clear() {
		account2UserMap.clear();
		registerObserver(false);
	}

	// ******************************* 业务接口（获取缓存信息） *********************************
	public List<NimUserInfo> getAllUsersOfMyFriend() {
		List<String> accounts = FriendCache.getInstance().getMyFriendAccounts();
		List<NimUserInfo> users = new ArrayList<>();
		List<String> unknownAccounts = new ArrayList<>();
		for (String account : accounts) {
			if (hasUser(account)) {
				users.add(getUserInfo(account));
			} else {
				unknownAccounts.add(account);
			}
		}

		if (!unknownAccounts.isEmpty()) {
			getUserInfoFromRemote(unknownAccounts, null);
		}

		return users;
	}

	public NimUserInfo getUserInfo(String account) {
		if (!hasUser(account)) {
			getUserInfoFromRemote(account, null);
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

	public void getUserInfoFromRemote(final String account, final RequestCallback<NimUserInfo> callback) {
		if (TextUtils.isEmpty(account)) {
			return;
		}

		if (requestUserInfoMap.containsKey(account)) {
			if (callback != null) {
				requestUserInfoMap.get(account).add(callback);
			}
			return; // 已经在请求中，不要重复请求
		} else {
			List<RequestCallback<NimUserInfo>> cbs = new ArrayList<>();
			if (callback != null) {
				cbs.add(callback);
			}
			requestUserInfoMap.put(account, cbs);
		}

		List<String> accounts = new ArrayList<>(1);
		accounts.add(account);

		NIMClient.getService(UserService.class).fetchUserInfo(accounts).setCallback(new RequestCallbackWrapper<List<NimUserInfo>>() {

			@Override
			public void onResult(int code, List<NimUserInfo> users, Throwable exception) {
				if (exception != null && callback != null) {
					callback.onException(exception);
					return;
				}

				NimUserInfo user = null;
				List<RequestCallback<NimUserInfo>> cbs = requestUserInfoMap.get(account);
				boolean hasCallback = cbs != null && cbs.size() > 0;
				if (code == ResponseCode.RES_SUCCESS && users != null && !users.isEmpty()) {
					user = users.get(0);
				}

				// 处理回调
				if (hasCallback) {
					for (RequestCallback<NimUserInfo> cb : cbs) {
						if (code == ResponseCode.RES_SUCCESS) {
							cb.onSuccess(user);
						} else {
							cb.onFailed(code);
						}
					}
				}

				requestUserInfoMap.remove(account);
			}
		});
	}

	public void getUserInfoFromRemote(List<String> accounts, final RequestCallback<List<NimUserInfo>> callback) {
		NIMClient.getService(UserService.class).fetchUserInfo(accounts).setCallback(new RequestCallback<List<NimUserInfo>>() {
			@Override
			public void onSuccess(List<NimUserInfo> users) {
				// 这里不需要更新缓存，由监听用户资料变更（添加）来更新缓存
				if (callback != null) {
					callback.onSuccess(users);
				}
			}

			@Override
			public void onFailed(int code) {
				if (callback != null) {
					callback.onFailed(code);
				}
			}

			@Override
			public void onException(Throwable exception) {
				if (callback != null) {
					callback.onException(exception);
				}
			}
		});
	}

	private void updateUsers(final List<NimUserInfo> users) {
		if (users == null || users.isEmpty()) {
			return;
		}

		for (NimUserInfo u : users) {
			account2UserMap.put(u.getAccount(), u);
		}
	}

	// ******************************* 事件监听 *********************************
	public void registerObserver(boolean register) {
		NIMClient.getService(UserServiceObserve.class).observeUserInfoUpdate(userInfoUpdateObserver, register);
	}

	private final Observer<List<NimUserInfo>> userInfoUpdateObserver = new Observer<List<NimUserInfo>>() {
		@Override
		public void onEvent(List<NimUserInfo> users) {
			updateUsers(users);
			ReactCache.emit(MessageConstant.Event.observeUserInfoUpdate, users);
		}
	};
}
