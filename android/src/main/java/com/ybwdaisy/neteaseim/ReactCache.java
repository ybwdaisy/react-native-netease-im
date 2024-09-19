package com.ybwdaisy.neteaseim;

import android.text.TextUtils;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.friend.FriendService;
import com.netease.nimlib.sdk.friend.model.AddFriendNotify;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.attachment.AudioAttachment;
import com.netease.nimlib.sdk.msg.attachment.ImageAttachment;
import com.netease.nimlib.sdk.msg.attachment.LocationAttachment;
import com.netease.nimlib.sdk.msg.attachment.MsgAttachment;
import com.netease.nimlib.sdk.msg.attachment.VideoAttachment;
import com.netease.nimlib.sdk.msg.constant.MsgDirectionEnum;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.constant.SystemMessageStatus;
import com.netease.nimlib.sdk.msg.constant.SystemMessageType;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.msg.model.SystemMessage;
import com.ybwdaisy.neteaseim.Attachment.CustomAttachment;
import com.ybwdaisy.neteaseim.Attachment.DefaultCustomAttachment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ReactCache {
	private final static String TAG = "ReactCache";
	private static ReactContext reactContext;
	private final static String MESSAGE_EXTEND = MessageConstant.Message.EXTEND;

	public static void setReactContext(ReactContext reactContext) {
		ReactCache.reactContext = reactContext;
	}

	public static ReactContext getReactContext() {
		return reactContext;
	}

	public static void emit(String eventName, Object data) {
		Log.i(TAG, "eventName: " + eventName + " data: " + data);
		try {
			reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, data);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ******************************* 处理消息 *********************************
	public static Object createMessageList(List<IMMessage> messageList) {
		WritableArray writableArray = Arguments.createArray();

		if (messageList != null) {
			int size = messageList.size();
			for (int i = 0; i < size; i++) {
				IMMessage item = messageList.get(i);
				if (item != null) {
					WritableMap itemMap = createMessage(item);
					writableArray.pushMap(itemMap);
				}
			}
		}
		return writableArray;
	}

	public static WritableMap createMessage(IMMessage item) {
		WritableMap itemMap = Arguments.createMap();
		itemMap.putString(MessageConstant.Message.MSG_ID, item.getUuid());

		itemMap.putString(MessageConstant.Message.MSG_TYPE, getMessageType(item));
		itemMap.putString(MessageConstant.Message.TIME_STRING, Long.toString(item.getTime() / 1000));
		itemMap.putString(MessageConstant.Message.SESSION_ID, item.getSessionId());
		itemMap.putString(MessageConstant.Message.SESSION_TYPE, Integer.toString(item.getSessionType().getValue()));

		itemMap.putBoolean(MessageConstant.Message.IS_OUTGOING, item.getDirect() == MsgDirectionEnum.Out);
		itemMap.putString(MessageConstant.Message.STATUS, getMessageStatus(item.getStatus()));
		itemMap.putString(MessageConstant.Message.ATTACH_STATUS, Integer.toString(item.getAttachStatus().getValue()));
		itemMap.putString(MessageConstant.Message.IS_REMOTE_READ, boolean2String(receiveReceiptCheck(item)));

		WritableMap user = Arguments.createMap();
		String fromAccount = item.getFromAccount();
		String avatar = null;

		String fromNick = null;
		String displayName = null;
		try {
			fromNick = item.getFromNick();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!TextUtils.isEmpty(fromAccount)) {
			UserInfoCache userInfoCache = UserInfoCache.getInstance();
			displayName = TextUtils.isEmpty(fromNick) ? userInfoCache.getUserDisplayName(fromAccount) : fromNick;
			avatar = userInfoCache.getAvatar(fromAccount);
		}
		user.putString(MessageConstant.User.DISPLAY_NAME, displayName);
		user.putString(MessageConstant.User.USER_ID, fromAccount);
		user.putString(MessageConstant.User.AVATAR_PATH, avatar);
		itemMap.putMap(MessageConstant.Message.FROM_USER, user);

		MsgAttachment attachment = item.getAttachment();
		String text = "";

		if (attachment != null) {
			if (item.getMsgType() == MsgTypeEnum.image) {
				WritableMap imageObj = Arguments.createMap();
				if (attachment instanceof ImageAttachment) {
					ImageAttachment imageAttachment = (ImageAttachment) attachment;
					if (item.getDirect() == MsgDirectionEnum.Out) {
						imageObj.putString(MessageConstant.MediaFile.THUMB_PATH, imageAttachment.getPath());
					} else {
						imageObj.putString(MessageConstant.MediaFile.THUMB_PATH, imageAttachment.getThumbPath());
					}
					imageObj.putString(MessageConstant.MediaFile.PATH, imageAttachment.getPath());
					imageObj.putString(MessageConstant.MediaFile.URL, imageAttachment.getUrl());
					imageObj.putString(MessageConstant.MediaFile.DISPLAY_NAME, imageAttachment.getDisplayName());
					imageObj.putString(MessageConstant.MediaFile.HEIGHT, Integer.toString(imageAttachment.getHeight()));
					imageObj.putString(MessageConstant.MediaFile.WIDTH, Integer.toString(imageAttachment.getWidth()));
				}
				itemMap.putMap(MESSAGE_EXTEND, imageObj);
			} else if (item.getMsgType() == MsgTypeEnum.audio) {
				WritableMap audioObj = Arguments.createMap();
				if (attachment instanceof AudioAttachment) {
					AudioAttachment audioAttachment = (AudioAttachment) attachment;
					audioObj.putString(MessageConstant.MediaFile.PATH, audioAttachment.getPath());
					audioObj.putString(MessageConstant.MediaFile.THUMB_PATH, audioAttachment.getThumbPath());
					audioObj.putString(MessageConstant.MediaFile.URL, audioAttachment.getUrl());
					audioObj.putString(MessageConstant.MediaFile.DURATION, Long.toString(audioAttachment.getDuration()));
				}
				itemMap.putMap(MESSAGE_EXTEND, audioObj);
			} else if (item.getMsgType() == MsgTypeEnum.video) {
				WritableMap videoDic = Arguments.createMap();
				if (attachment instanceof VideoAttachment) {
					VideoAttachment videoAttachment = (VideoAttachment) attachment;
					videoDic.putString(MessageConstant.MediaFile.URL, videoAttachment.getUrl());
					videoDic.putString(MessageConstant.MediaFile.PATH, videoAttachment.getPath());
					videoDic.putString(MessageConstant.MediaFile.DISPLAY_NAME, videoAttachment.getDisplayName());
					videoDic.putString(MessageConstant.MediaFile.HEIGHT, Integer.toString(videoAttachment.getHeight()));
					videoDic.putString(MessageConstant.MediaFile.WIDTH, Integer.toString(videoAttachment.getWidth()));
					videoDic.putString(MessageConstant.MediaFile.DURATION, Long.toString(videoAttachment.getDuration()));
					videoDic.putString(MessageConstant.MediaFile.SIZE, Long.toString(videoAttachment.getSize()));
					videoDic.putString(MessageConstant.MediaFile.THUMB_PATH, videoAttachment.getThumbPath());
				}
				itemMap.putMap(MESSAGE_EXTEND, videoDic);
			} else if (item.getMsgType() == MsgTypeEnum.location) {
				WritableMap locationObj = Arguments.createMap();
				if (attachment instanceof LocationAttachment) {
					LocationAttachment locationAttachment = (LocationAttachment) attachment;
					locationObj.putString(MessageConstant.Location.LATITUDE, Double.toString(locationAttachment.getLatitude()));
					locationObj.putString(MessageConstant.Location.LONGITUDE, Double.toString(locationAttachment.getLongitude()));
					locationObj.putString(MessageConstant.Location.ADDRESS, locationAttachment.getAddress());
				}
				itemMap.putMap(MESSAGE_EXTEND, locationObj);
			} else if (item.getMsgType() == MsgTypeEnum.notification) {
				text = item.getContent();
			} else if (item.getMsgType() == MsgTypeEnum.custom) {
				if (attachment instanceof DefaultCustomAttachment) {
					DefaultCustomAttachment defaultCustomAttachment = (DefaultCustomAttachment) attachment;
					itemMap.putMap(MESSAGE_EXTEND, defaultCustomAttachment.toReactNative());
				}
			}
		} else {
			text = item.getContent();
		}
		if (item.getMsgType() == MsgTypeEnum.text) {
			text = item.getContent();
		} else if (item.getMsgType() == MsgTypeEnum.tip) {
			if (TextUtils.isEmpty(item.getContent())) {
				Map<String, Object> content = item.getRemoteExtension();
				if (content != null && !content.isEmpty()) {
					text = (String) content.get("content");
				}
				content = item.getLocalExtension();
				if (content != null && !content.isEmpty()) {
					text = (String) content.get("content");
				}
				if (TextUtils.isEmpty(text)) {
					text = "未知通知提醒";
				}
			} else {
				text = item.getContent();
			}
		}
		itemMap.putString(MessageConstant.Message.MSG_TEXT, text);

		return itemMap;
	}

	private static Comparator<IMMessage> comparator = new Comparator<IMMessage>() {

		@Override
		public int compare(IMMessage o1, IMMessage o2) {
			long time = o1.getTime() - o2.getTime();
			return time == 0 ? 0 : (time < 0 ? -1 : 1);
		}
	};

	public static void sortMessages(List<IMMessage> list) {
		if (list.isEmpty()) {
			return;
		}
		Collections.sort(list, comparator);
	}

	static String getMessageType(IMMessage item) {
		String type = MessageConstant.MsgType.CUSTOM;
		switch (item.getMsgType()) {
			case text:
				type = MessageConstant.MsgType.TEXT;
				break;
			case image:
				type = MessageConstant.MsgType.IMAGE;
				break;
			case audio:
				type = MessageConstant.MsgType.VOICE;
				break;
			case video:
				type = MessageConstant.MsgType.VIDEO;
				break;
			case location:
				type = MessageConstant.MsgType.LOCATION;
				break;
			case file:
				type = MessageConstant.MsgType.FILE;
				break;
			case notification:
				type = MessageConstant.MsgType.NOTIFICATION;
				break;
			case tip:
				type = MessageConstant.MsgType.TIP;
				break;
			case robot:
				type = MessageConstant.MsgType.ROBOT;
				break;
			case custom:
				type = MessageConstant.MsgType.CUSTOM;
				break;
			default:
				type = MessageConstant.MsgType.CUSTOM;
				break;
		}

		return type;
	}

	static String getMessageStatus(MsgStatusEnum statusEnum) {
		switch (statusEnum) {
			case draft:
				return MessageConstant.MsgStatus.SEND_DRAFT;
			case sending:
				return MessageConstant.MsgStatus.SEND_SENDING;
			case success:
				return MessageConstant.MsgStatus.SEND_SUCCESS;
			case fail:
				return MessageConstant.MsgStatus.SEND_FAILE;
			case read:
				return MessageConstant.MsgStatus.RECEIVE_READ;
			case unread:
				return MessageConstant.MsgStatus.RECEIVE_UNREAD;
			default:
				return MessageConstant.MsgStatus.SEND_DRAFT;
		}
	}

	private static boolean receiveReceiptCheck(final IMMessage msg) {
		if (msg != null) {
			if (msg.getSessionType() == SessionTypeEnum.P2P
					&& msg.getDirect() == MsgDirectionEnum.Out
					&& msg.getMsgType() != MsgTypeEnum.tip
					&& msg.getMsgType() != MsgTypeEnum.notification
					&& msg.isRemoteRead()) {
				return true;
			} else {
				return msg.isRemoteRead();
			}
		}
		return false;
	}

	static String boolean2String(boolean bool) {
		return bool ? Integer.toString(1) : Integer.toString(0);
	}

	// ******************************* 处理会话 *********************************
	public static Object createRecentList(List <RecentContact> recents) {
		WritableArray array = Arguments.createArray();
		int unreadCount = 0;
		if (recents != null && !recents.isEmpty()) {
			for (RecentContact contact : recents) {
				unreadCount += contact.getUnreadCount();

				WritableMap map = Arguments.createMap();
				String contactId = contact.getContactId();
				map.putString(MessageConstant.Contact.CONTACT_ID, contactId);
				map.putInt(MessageConstant.Contact.UNREAD_COUNT, contact.getUnreadCount());
				map.putString(MessageConstant.Contact.SESSION_TYPE, Integer.toString(contact.getSessionType().getValue()));
				map.putString(MessageConstant.Contact.MSG_TYPE, Integer.toString(contact.getMsgType().getValue()));
				map.putString(MessageConstant.Contact.MSG_STATUS, getMessageStatus(contact.getMsgStatus()));
				map.putString(MessageConstant.Contact.MESSAGE_ID, contact.getRecentMessageId());
				map.putString(MessageConstant.Contact.FROM_ACCOUNT, contact.getFromAccount());

				UserInfoCache userInfoCache = UserInfoCache.getInstance();
				map.putString(MessageConstant.Contact.NAME, userInfoCache.getUserDisplayName(contactId));
				map.putString(MessageConstant.Contact.IMAGE_PATH, userInfoCache.getAvatar(contactId));
				map.putString(MessageConstant.Contact.EXT, userInfoCache.getExtension(contactId));

				String content = contact.getContent();
				switch (contact.getMsgType()) {
					case text:
						content = contact.getContent();
						break;
					case image:
						content = "[图片]";
						break;
					case video:
						content = "[视频]";
						break;
					case audio:
						content = "[语音消息]";
						break;
					case location:
						content = "[位置]";
						break;
					case tip:
						List<String> uuids = new ArrayList<>();
						uuids.add(contact.getRecentMessageId());
						List<IMMessage> messages = NIMClient.getService(MsgService.class).queryMessageListByUuidBlock(uuids);
						if (messages != null && messages.size() > 0) {
							content = messages.get(0).getContent();
						}
						break;
					default:
						break;
				}
				map.putString(MessageConstant.Contact.TIME, TimeUtil.getTimeShowString(contact.getTime(), true));
				CustomAttachment attachment = null;
				try {
					if (contact.getMsgType() == MsgTypeEnum.custom) {
						attachment = (CustomAttachment) contact.getAttachment();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (attachment != null) {
					map.putString(MessageConstant.Contact.TYPE, attachment.getType());
					if (attachment instanceof DefaultCustomAttachment) {
						content = ((DefaultCustomAttachment) attachment).getRecentContent();
						if (TextUtils.isEmpty(content)) {
							content = "[未知消息]";
						}
					}
				}
				map.putString(MessageConstant.Contact.CONTENT, content);
				array.pushMap(map);
			}
		}
		WritableMap writableMap = Arguments.createMap();
		writableMap.putArray(MessageConstant.Contact.RECENTS, array);
		writableMap.putInt(MessageConstant.Contact.UNREAD_COUNT, unreadCount);
		return writableMap;
	}

	public static Object createSystemMsg(List<SystemMessage> sysItems) {
		WritableArray writableArray = Arguments.createArray();

		if (sysItems != null && sysItems.size() > 0) {
			UserInfoCache userInfoCache = UserInfoCache.getInstance();
			for (SystemMessage message : sysItems) {
				WritableMap map = Arguments.createMap();
				boolean verify = isVerifyMessageNeedDeal(message);
				map.putString("messageId", Long.toString(message.getMessageId()));
				map.putString("type", Integer.toString(message.getType().getValue()));
				map.putString("targetId", message.getTargetId());
				map.putString("fromAccount", message.getFromAccount());
				String avatar = userInfoCache.getAvatar(message.getFromAccount());
				map.putString("avatar", avatar);
				map.putString("name", userInfoCache.getUserDisplayNameEx(message.getFromAccount()));//alias
				map.putString("time", Long.toString(message.getTime() / 1000));
				map.putString("isVerify", boolean2String(verify));
				map.putString("status", Integer.toString(message.getStatus().getValue()));
				map.putString("verifyText", getVerifyNotificationText(message));
				if (verify) {
					if (message.getStatus() != SystemMessageStatus.init) {
						map.putString("verifyResult", getVerifyNotificationDealResult(message));
					}
				}
				writableArray.pushMap(map);
			}
		}
		return writableArray;
	}

	private static String getVerifyNotificationText(SystemMessage message) {
		StringBuilder sb = new StringBuilder();
		String fromAccount = UserInfoCache.getInstance().getUserDisplayNameYou(message.getFromAccount());
		if (message.getType() == SystemMessageType.AddFriend) {
			AddFriendNotify attachData = (AddFriendNotify) message.getAttachObject();
			if (attachData != null) {
				if (attachData.getEvent() == AddFriendNotify.Event.RECV_ADD_FRIEND_DIRECT) {
					sb.append("已添加你为好友");
				} else if (attachData.getEvent() == AddFriendNotify.Event.RECV_AGREE_ADD_FRIEND) {
					sb.append("通过了你的好友请求");
				} else if (attachData.getEvent() == AddFriendNotify.Event.RECV_REJECT_ADD_FRIEND) {
					sb.append("拒绝了你的好友请求");
				} else if (attachData.getEvent() == AddFriendNotify.Event.RECV_ADD_FRIEND_VERIFY_REQUEST) {
					sb.append(TextUtils.isEmpty(message.getContent()) ? "请求添加好友" : message.getContent());
				}
			}
		}

		return sb.toString();
	}

	private static boolean isVerifyMessageNeedDeal(SystemMessage message) {
		if (message.getType() == SystemMessageType.AddFriend) {
			if (message.getAttachObject() != null) {
				AddFriendNotify attachData = (AddFriendNotify) message.getAttachObject();
				if (attachData.getEvent() == AddFriendNotify.Event.RECV_ADD_FRIEND_DIRECT ||
						attachData.getEvent() == AddFriendNotify.Event.RECV_AGREE_ADD_FRIEND ||
						attachData.getEvent() == AddFriendNotify.Event.RECV_REJECT_ADD_FRIEND) {
					return false; // 对方直接加你为好友，对方通过你的好友请求，对方拒绝你的好友请求
				} else if (attachData.getEvent() == AddFriendNotify.Event.RECV_ADD_FRIEND_VERIFY_REQUEST) {
					return true; // 好友验证请求
				}
			}
			return false;
		} else if (message.getType() == SystemMessageType.TeamInvite || message.getType() == SystemMessageType.ApplyJoinTeam) {
			return true;
		} else {
			return false;
		}
	}

	private static String getVerifyNotificationDealResult(SystemMessage message) {
		if (message.getStatus() == SystemMessageStatus.passed) {
			return "已同意";
		} else if (message.getStatus() == SystemMessageStatus.declined) {
			return "已拒绝";
		} else if (message.getStatus() == SystemMessageStatus.ignored) {
			return "已忽略";
		} else if (message.getStatus() == SystemMessageStatus.expired) {
			return "已过期";
		} else {
			return "未处理";
		}
	}

}
