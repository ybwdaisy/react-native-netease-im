package com.ybwdaisy.neteaseim;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.constant.DeleteTypeEnum;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.AttachmentProgress;
import com.netease.nimlib.sdk.msg.model.CustomMessageConfig;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.QueryDirectionEnum;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.ybwdaisy.neteaseim.Attachment.DefaultCustomAttachment;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SessionService {
	private final static String TAG = "SessionService";
	private SessionTypeEnum sessionTypeEnum = SessionTypeEnum.None;
	private String sessionId;
	private boolean hasRegister;
	MsgService msgService;
	Map<String, Object> pushPayload;

	static class InstanceHolder {
		final static SessionService instance = new SessionService();
	}

	public static SessionService getInstance() {
		return InstanceHolder.instance;
	}

	public Map<String, Object> getPushPayload() {
		return pushPayload;
	}

	public void setPushPayload(Map<String, Object> pushPayload) {
		this.pushPayload = pushPayload;
	}

	public void startSession(String sessionId, String type) {
		Log.i(TAG, "startSession sessionId: " + sessionId);
		this.sessionId = sessionId;
		this.sessionTypeEnum = getSessionType(type);
		Log.i(TAG, "startSession login status: " + NIMClient.getStatus().wontAutoLogin());
		registerObservers(true);
		getMsgService().setChattingAccount(sessionId, sessionTypeEnum);
	}

	public void stopSession() {
		registerObservers(false);
		getMsgService().setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_NONE,
				SessionTypeEnum.None);
	}

	public void sendTextMessage(String content) {
		IMMessage textMessage = MessageBuilder.createTextMessage(sessionId, sessionTypeEnum, content);
		doSendMessage(textMessage, false);
	}

	public void sendImageMessage(String file, String displayName) {
		file = file.replace("file://", "");
		file = Uri.parse(file).getPath();
		File newFile = new File(file);
		String fileName = TextUtils.isEmpty(displayName) ? newFile.getName() : displayName;
		IMMessage imageMessage = MessageBuilder.createImageMessage(sessionId, sessionTypeEnum, newFile, fileName);
		doSendMessage(imageMessage, false);
	}

	public void sendCustomMessage(ReadableMap attachment) {
		DefaultCustomAttachment defaultCustomAttachment = new DefaultCustomAttachment();
		String recentContent = attachment.getString("recentContent");
		defaultCustomAttachment.setRecentContent(recentContent);
		defaultCustomAttachment.setCustomData(attachment);
		String pushContent = attachment.getString("pushContent");
		IMMessage customMessage  = MessageBuilder.createCustomMessage(sessionId, sessionTypeEnum, pushContent, defaultCustomAttachment);
		doSendMessage(customMessage, false);
	}

	public void updateCustomMessage(String messageId, ReadableMap attachment) {
		queryMessageById(messageId, new MessageQueryListener() {
			@Override
			public int onResult(int code, IMMessage message) {
				if (message != null) {
					DefaultCustomAttachment defaultCustomAttachment = new DefaultCustomAttachment();
					defaultCustomAttachment.setCustomData(attachment);
					message.setAttachment(defaultCustomAttachment);
					getMsgService().updateIMMessage(message);
				}
				return code;
			}
		});
	}

	public void resendMessage(String messageId) {
		queryMessageById(messageId, new MessageQueryListener() {
			@Override
			public int onResult(int code, IMMessage message) {
				if (message != null) {
					message.setStatus(MsgStatusEnum.sending);
					getMsgService().deleteChattingHistory(message);
					doSendMessage(message, true);
				}
				return code;
			}
		});
	}

	public void deleteMessage(String messageId) {
		queryMessageById(messageId, new MessageQueryListener() {
			@Override
			public int onResult(int code, IMMessage message) {
				if (message != null) {
					getMsgService().deleteChattingHistory(message);
				}
				return code;
			}
		});
	}

	public void clearMessage() {
		getMsgService().clearChattingHistory(sessionId, sessionTypeEnum);
	}

	public int getTotalUnreadCount() {
		return getMsgService().getTotalUnreadCount();
	}

	public void clearAllUnreadCount() {
		getMsgService().clearAllUnreadCount();
	}

	public void doSendMessage(IMMessage message, boolean resend) {
		CustomMessageConfig config = new CustomMessageConfig();
		message.setConfig(config);
		setPushConfig(message);
		Log.i(TAG, "doSendMessage login status: " + NIMClient.getStatus());
		getMsgService().sendMessage(message, resend).setCallback(new RequestCallback<Void>() {
			@Override
			public void onSuccess(Void result) {
				Log.i(TAG, "doSendMessage success: " + result);
			}

			@Override
			public void onFailed(int code) {
				Log.i(TAG, "doSendMessage fail: " + code);
			}

			@Override
			public void onException(Throwable exception) {
				Log.e(TAG, "doSendMessage exception: " + exception.getLocalizedMessage());
			}
		});
	}

	public boolean isMyFriend() {
		return FriendCache.getInstance().isMyFriend(sessionId);
	}

	public void queryMessageById(String messageId, MessageQueryListener messageQueryListener) {
		if (messageQueryListener == null) {
			return;
		}
		if (TextUtils.isEmpty(messageId)) {
			messageQueryListener.onResult(-1, null);
			return;
		}
		List<String> uuids = new ArrayList<>();
		uuids.add(messageId);
		getMsgService().queryMessageListByUuid(uuids).setCallback(new RequestCallbackWrapper<List<IMMessage>>() {
			@Override
			public void onResult(int code, List<IMMessage> messageList, Throwable exception) {
				if (messageList == null || messageList.isEmpty()) {
					messageQueryListener.onResult(code, null);
					return;
				}
				messageQueryListener.onResult(code, messageList.get(0));
			}
		});
	}

	public void queryMessageListEx(String messageId, final int limit, final Promise promise) {
		Log.i(TAG, "messageId: " + messageId);
		queryMessageById(messageId, new MessageQueryListener() {
			@Override
			public int onResult(int code, IMMessage message) {
				IMMessage anchor = message;
				if (anchor == null) {
					anchor = MessageBuilder.createEmptyMessage(sessionId, sessionTypeEnum, 0);
				}
				getMsgService().queryMessageListEx(anchor, QueryDirectionEnum.QUERY_OLD, limit, true).setCallback(new RequestCallbackWrapper<List<IMMessage>>() {
					@Override
					public void onResult(int code, List<IMMessage> messageList, Throwable exception) {
						if (code == ResponseCode.RES_SUCCESS) {
							Object result = ReactCache.createMessageList(messageList);
							promise.resolve(result);
						} else {
							promise.reject("-1", "获取历史消息失败: " + code);
						}
					}

					public void onFailed(int code) {
						promise.reject("-1", "获取历史消息失败: " + code);
					}
				});
				return code;
			}
		});
	}

	public void queryRecentContacts(final Promise promise) {
		getMsgService().queryRecentContacts().setCallback(new RequestCallbackWrapper<List<RecentContact>>() {
			@Override
			public void onResult(int code, List<RecentContact> recentContacts, Throwable exception) {
				promise.resolve(ReactCache.createRecentList(recentContacts));
			}
			@Override
			public void onFailed(int code) {
				promise.reject(String.valueOf(code), "获取最近会话失败");
			}
		});
	}

	public void deleteRecentContact(String account) {
		getMsgService().deleteRecentContact(account, sessionTypeEnum, DeleteTypeEnum.LOCAL_AND_REMOTE, false);
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
		service.observeMsgStatus(new Observer<IMMessage>() {
			@Override
			public void onEvent(IMMessage message) {
				onMessageStatusChange(message);
			}
		}, register);

		service.observeReceiveMessage(new Observer<List<IMMessage>>() {
			@Override
			public void onEvent(List<IMMessage> messages) {
				onIncomingMessage(messages);
			}
		}, register);

		service.observeRecentContact(new Observer<List<RecentContact>>() {
			@Override
			public void onEvent(List<RecentContact> recentContacts) {
				onRecentContact(recentContacts);
			}
		}, register);

		service.observeAttachmentProgress(new Observer<AttachmentProgress>() {
			@Override
			public void onEvent(AttachmentProgress attachmentProgress) {
				onAttachmentProgress(attachmentProgress);
			}
		}, register);
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

	public void onRecentContact(List<RecentContact> recentContacts) {
		ReactCache.emit(MessageConstant.Event.observeRecentContact, ReactCache.createRecentList(recentContacts));
	}

	public void onAttachmentProgress(AttachmentProgress attachmentProgress) {
		WritableMap result = Arguments.createMap();
		result.putString("uuid", attachmentProgress.getUuid());
		BigDecimal transferred = new BigDecimal(attachmentProgress.getTransferred());
		BigDecimal total = new BigDecimal(attachmentProgress.getTotal());
		Double progress = transferred.divide(total, 10, RoundingMode.HALF_UP).doubleValue();
		result.putDouble("progress", progress);
		ReactCache.emit(MessageConstant.Event.observeAttachmentProgress, result);
	}

	/****************************** 工具方法 ***********************************/

	public void setPushConfig(IMMessage message) {
		message.setPushContent(message.getContent());

		String sessionId = LoginService.getInstance().getAccount();
		String sessionName = UserInfoCache.getInstance().getUserName(sessionId);
		pushPayload.put("sessionId", sessionId);
		pushPayload.put("sessionName", sessionName);
		message.setPushPayload(pushPayload);
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

	public interface MessageQueryListener {
		public int onResult(int code, IMMessage message);
	}

}
