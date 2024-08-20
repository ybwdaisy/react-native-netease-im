package com.ybwdaisy;

import android.text.TextUtils;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.netease.nimlib.sdk.msg.attachment.AudioAttachment;
import com.netease.nimlib.sdk.msg.attachment.ImageAttachment;
import com.netease.nimlib.sdk.msg.attachment.LocationAttachment;
import com.netease.nimlib.sdk.msg.attachment.MsgAttachment;
import com.netease.nimlib.sdk.msg.attachment.VideoAttachment;
import com.netease.nimlib.sdk.msg.constant.MsgDirectionEnum;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.msg.constant.MsgTypeEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.ybwdaisy.Attachment.CustomAttachment;
import com.ybwdaisy.Attachment.DefaultCustomAttachment;

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


}
