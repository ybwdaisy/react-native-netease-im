package com.ybwdaisy;

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
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
import com.ybwdaisy.Attachment.DefaultCustomAttachment;

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
	private volatile MsgService msgService;

	static class InstanceHolder {
		final static SessionService instance = new SessionService();
	}

	public static SessionService getInstance() {
		return InstanceHolder.instance;
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
		getMsgService().sendMessage(textMessage, false);
		onMessageStatusChange(textMessage);
	}

	public void sendImageMessage(String file, String displayName) {
		file = Uri.parse(file).getPath();
		File f = new File(file);
		IMMessage imageMessage = MessageBuilder.createImageMessage(sessionId, sessionTypeEnum, f, TextUtils.isEmpty(displayName) ? f.getName() : displayName);
		setPushConfig(imageMessage);
		getMsgService().sendMessage(imageMessage, false);
		onMessageStatusChange(imageMessage);
	}

	public void sendCustomMessage(ReadableMap attachment) {
		DefaultCustomAttachment defaultCustomAttachment = new DefaultCustomAttachment();
		defaultCustomAttachment.setCustomData(attachment);
		String pushContent = attachment.getString("pushContent");
		IMMessage customMessage  = MessageBuilder.createCustomMessage(sessionId, sessionTypeEnum, pushContent, defaultCustomAttachment);
		setPushConfig(customMessage);
		getMsgService().sendMessage(customMessage, false);
		onMessageStatusChange(customMessage);
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
			Object msgList = ReactCache.createMessageList(list);
			ReactCache.emit(MessageConstant.Event.observeMsgStatus, msgList);
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
			ReactCache.sortMessages(addedListItems);
		}
		Object data = ReactCache.createMessageList(addedListItems);
		ReactCache.emit(MessageConstant.Event.observeReceiveMessage, data);
	}

	/****************************** 工具方法 ***********************************/

	public void setPushConfig(IMMessage message) {
		message.setPushContent(message.getContent());
		Map<String, Object> payload = new HashMap<>();
		Map<String, Object> body = new HashMap<>();

		body.put("sessionType", String.valueOf(message.getSessionType().getValue()));
		if (message.getSessionType() == SessionTypeEnum.P2P) {
			String sessionId = LoginService.getInstance().getAccount();
			String sessionName = UserInfoCache.getInstance().getUserName(sessionId);
			body.put("sessionId", sessionId);
			body.put("sessionName", sessionName);
		}

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

}
