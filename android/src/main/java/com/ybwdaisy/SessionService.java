package com.ybwdaisy;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
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
import com.netease.nimlib.sdk.msg.model.MemberPushOption;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SessionService {
	private final static String TAG = "SessionService";
	private SessionTypeEnum sessionTypeEnum = SessionTypeEnum.None;
	private String sessionId;
	private boolean hasRegister;
	private MsgService msgService;
	private static ReactContext reactContext;
	private final static String MESSAGE_EXTEND = MessageConstant.Message.EXTEND;

	private SessionService() {}

	static class InstanceHolder {
		final static SessionService instance = new SessionService();
	}

	public static SessionService getInstance() {
		return InstanceHolder.instance;
	}

	public static void setReactContext(ReactContext context) {
		reactContext = context;
	}

	public static ReactContext getReactContext() {
		return reactContext;
	}

	Observer<IMMessage> messageStatusObserver = new Observer<IMMessage>() {
		@Override
		public void onEvent(IMMessage message) {
			onMessageStatusChange(message);
		}
	};

	Observer<List<IMMessage>> incomingMessageObserver = new Observer<List<IMMessage>>() {
		@Override
		public void onEvent(List<IMMessage> messages) {
			onIncomingMessage(messages);
		}
	};

	public void startSession(String sessionId, String type) {
		this.sessionId = sessionId;
		this.sessionTypeEnum = getSessionType(type);
		registerObservers(true);
		getMsgService().setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_NONE,
				SessionTypeEnum.None);
	}

	public void stopSession() {
		registerObservers(false);
		getMsgService().setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_NONE,
				SessionTypeEnum.None);
	}

	public void sendTextMessage(String content) {
		IMMessage textMessage = MessageBuilder.createTextMessage(sessionId, sessionTypeEnum, content);
		setPushConfig(textMessage);
		getMsgService().sendMessage(textMessage, false).setCallback(new RequestCallback<Void>() {
			@Override
			public void onSuccess(Void result) {

			}

			@Override
			public void onFailed(int code) {

			}

			@Override
			public void onException(Throwable exception) {

			}
		});
		onMessageStatusChange(textMessage);
	}

	public void sendImageMessage(String file, String displayName) {
		file = Uri.parse(file).getPath();
		File f = new File(file);
		IMMessage imageMessage = MessageBuilder.createImageMessage(sessionId, sessionTypeEnum, f, TextUtils.isEmpty(displayName) ? f.getName() : displayName);
		setPushConfig(imageMessage);
		getMsgService().sendMessage(imageMessage, false).setCallback(new RequestCallback<Void>() {
			@Override
			public void onSuccess(Void result) {

			}

			@Override
			public void onFailed(int code) {

			}

			@Override
			public void onException(Throwable exception) {

			}
		});
		onMessageStatusChange(imageMessage);
	}

	public MsgService getMsgService() {
		if (msgService == null) {
			synchronized (SessionService.class) {
				if (msgService == null) {
					msgService = NIMClient.getService(MsgService.class);
				}
			}
		}
		return msgService;
	}

	private void registerObservers(boolean register) {
		if (hasRegister && register) {
			return;
		}
		hasRegister = register;
		MsgServiceObserve service = NIMClient.getService(MsgServiceObserve.class);
		service.observeMsgStatus(messageStatusObserver, register);
		service.observeReceiveMessage(incomingMessageObserver, register);
	}

	private void onMessageStatusChange(IMMessage message) {
		if (isMyMessage(message)) {
			List<IMMessage> list = new ArrayList<>(1);
			list.add(message);
			Object msgList = createMessageList(list);
			emitEvent(MessageConstant.Event.observeMsgStatus, msgList);
		}
	}

	public void onIncomingMessage(@NonNull List<IMMessage> messages) {
		if (messages.isEmpty()) {
			return;
		}
		boolean needRefresh = false;
		List<IMMessage> addedListItems = new ArrayList<>(messages.size());
		for (IMMessage message: messages) {
			if (isMyMessage(message)) {
				addedListItems.add(message);
				needRefresh = true;
			}
		}
		if (needRefresh) {
			sortMessages(addedListItems);
		}
		Object data = createMessageList(addedListItems);
		emitEvent(MessageConstant.Event.observeReceiveMessage, data);
	}

	/****************************** 工具方法 ***********************************/

	public void setPushConfig(IMMessage message) {
		message.setPushContent(message.getContent());
		Map<String, Object> payload = new HashMap<>();
		Map<String, Object> body = new HashMap<>();

		body.put("sessionType", String.valueOf(message.getSessionType().getValue()));
		if (message.getSessionType() == SessionTypeEnum.P2P) {
			body.put("sessionId", LoginService.getInstance().getAccount());
		}
		// TODO: 获取用户名
		body.put("sessionName", LoginService.getInstance().getAccount());
		payload.put("sessionBody", body);
		message.setPushPayload(payload);
	}

	public static SessionTypeEnum getSessionType(String type) {
		SessionTypeEnum sessionTypeEnum = SessionTypeEnum.None;
		try {
			sessionTypeEnum = SessionTypeEnum.typeOfValue(Integer.parseInt(type));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		return sessionTypeEnum;
	}

	public boolean isMyMessage(IMMessage message) {
		return message.getSessionType() == sessionTypeEnum
				&& message.getSessionId() != null
				&& message.getSessionId().equals(sessionId);
	}

	public static void emitEvent(String eventName, Object data) {
		try {
			reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(eventName, data);
		} catch (Exception e) {
			e.printStackTrace();
		}
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

	public static Object createMessageList(List<IMMessage> messageList) {
		WritableArray writableArray = Arguments.createArray();

		if (messageList != null) {
			int size = messageList.size();
			for (int i = 0; i < size; i++) {

				IMMessage item = messageList.get(i);
				if (item != null) {
					WritableMap itemMap = createMessage(item);
					if (itemMap != null) {
						writableArray.pushMap(itemMap);
					}
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

		itemMap.putString(MessageConstant.Message.IS_OUTGOING, Integer.toString(item.getDirect().getValue()));
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
			// TODO
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
			} else if (item.getMsgType() == MsgTypeEnum.custom) {//自定义消息
				// TODO
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

	private void sortMessages(List<IMMessage> list) {
		if (list.size() == 0) {
			return;
		}
		Collections.sort(list, comparator);
	}


}
