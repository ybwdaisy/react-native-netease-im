package com.ybwdaisy;

import android.text.TextUtils;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.friend.FriendService;
import com.netease.nimlib.sdk.friend.FriendServiceObserve;
import com.netease.nimlib.sdk.friend.model.BlackListChangedNotify;
import com.netease.nimlib.sdk.friend.model.Friend;
import com.netease.nimlib.sdk.friend.model.FriendChangedNotify;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class FriendCache {
	final static String TAG = "FriendCache";
	private Set<String> friendAccountSet = new CopyOnWriteArraySet<>();
	private Map<String, Friend> friendMap = new ConcurrentHashMap<>();

	private List<FriendDataChangedObserver> friendObservers = new ArrayList<>();

	static class InstanceHolder {
		final static FriendCache instance = new FriendCache();
	}

	public static FriendCache getInstance() {
		return FriendCache.InstanceHolder.instance;
	}

	public void build() {
		List<Friend> friends = NIMClient.getService(FriendService.class).getFriends();
		for (Friend f : friends) {
			friendMap.put(f.getAccount(), f);
		}

		List<String> accounts = NIMClient.getService(FriendService.class).getFriendAccounts();
		if (accounts == null || accounts.isEmpty()) {
			return;
		}
		List<String> blacks = NIMClient.getService(FriendService.class).getBlackList();
		accounts.removeAll(blacks);
		accounts.remove(LoginService.getInstance().getAccount());
		friendAccountSet.addAll(accounts);

		registerObservers(true);
	}

	public void clear() {
		friendAccountSet.clear();
		friendMap.clear();
		registerObservers(false);
	}

	// ******************************* 业务接口（获取缓存信息） *********************************

	public List<String> getMyFriendAccounts() {
		List<String> accounts = new ArrayList<>(friendAccountSet.size());
		accounts.addAll(friendAccountSet);

		return accounts;
	}

	public int getMyFriendCounts() {
		return friendAccountSet.size();
	}

	public Friend getFriendByAccount(String account) {
		if (TextUtils.isEmpty(account)) {
			return null;
		}

		return friendMap.get(account);
	}

	public boolean isMyFriend(String account) {
		return friendAccountSet.contains(account);
	}

	// ******************************* 事件监听 *********************************

	public void registerObservers(boolean register) {
		NIMClient.getService(FriendServiceObserve.class).observeFriendChangedNotify(friendChangedNotifyObserver, register);
		NIMClient.getService(FriendServiceObserve.class).observeBlackListChangedNotify(blackListChangedNotifyObserver, register);
	}

	private final Observer<FriendChangedNotify> friendChangedNotifyObserver = new Observer<FriendChangedNotify>() {
		@Override
		public void onEvent(FriendChangedNotify friendChangedNotify) {
			List<Friend> addedOrUpdatedFriends = friendChangedNotify.getAddedOrUpdatedFriends();
			List<String> myFriendAccounts = new ArrayList<>(addedOrUpdatedFriends.size());
			List<String> friendAccounts = new ArrayList<>(addedOrUpdatedFriends.size());
			List<String> deletedFriendAccounts = friendChangedNotify.getDeletedFriends();

			String account;
			for (Friend f : addedOrUpdatedFriends) {
				account = f.getAccount();
				friendMap.put(account, f);
				friendAccounts.add(account);

				if (NIMClient.getService(FriendService.class).isInBlackList(account)) {
					continue;
				}

				myFriendAccounts.add(account);
			}

			// 更新我的好友关系
			if (!myFriendAccounts.isEmpty()) {
				friendAccountSet.addAll(myFriendAccounts);
			}

			// 通知好友关系更新
			if (!friendAccounts.isEmpty()) {
				for (FriendDataChangedObserver o : friendObservers) {
					o.onAddedOrUpdatedFriends(friendAccounts);
				}
			}

			// 处理被删除的好友关系
			if (!deletedFriendAccounts.isEmpty()) {
				friendAccountSet.removeAll(deletedFriendAccounts);

				for (String a : deletedFriendAccounts) {
					friendMap.remove(a);
				}

				for (FriendDataChangedObserver o : friendObservers) {
					o.onDeletedFriends(deletedFriendAccounts);
				}
			}
		}
	};

	private final Observer<BlackListChangedNotify> blackListChangedNotifyObserver = new Observer<BlackListChangedNotify>() {
		@Override
		public void onEvent(BlackListChangedNotify blackListChangedNotify) {
			List<String> addedAccounts = blackListChangedNotify.getAddedAccounts();
			List<String> removedAccounts = blackListChangedNotify.getRemovedAccounts();

			if (!addedAccounts.isEmpty()) {
				// 拉黑，即从好友名单中移除
				friendAccountSet.removeAll(addedAccounts);

				for (FriendDataChangedObserver o : friendObservers) {
					o.onAddUserToBlackList(addedAccounts);
				}

				// 拉黑，要从最近联系人列表中删除该好友
				for (String account : addedAccounts) {
					NIMClient.getService(MsgService.class).deleteRecentContact2(account, SessionTypeEnum.P2P);
				}
			}

			if (!removedAccounts.isEmpty()) {
				// 移出黑名单，判断是否加入好友名单
				for (String account : removedAccounts) {
					if (NIMClient.getService(FriendService.class).isMyFriend(account)) {
						friendAccountSet.add(account);
					}
				}

				// 通知观察者
				for (FriendDataChangedObserver o : friendObservers) {
					o.onRemoveUserFromBlackList(removedAccounts);
				}
			}
		}
	};

	public void registerFriendDataChangedObserver(FriendDataChangedObserver o, boolean register) {
		if (o == null) {
			return;
		}

		if (register) {
			if (!friendObservers.contains(o)) {
				friendObservers.add(o);
			}
		} else {
			friendObservers.remove(o);
		}
	}

	public interface FriendDataChangedObserver {
		void onAddedOrUpdatedFriends(List<String> accounts);

		void onDeletedFriends(List<String> accounts);

		void onAddUserToBlackList(List<String> account);

		void onRemoveUserFromBlackList(List<String> account);
	}
}
