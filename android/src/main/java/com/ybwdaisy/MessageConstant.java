package com.ybwdaisy;

public class MessageConstant {
	public class MsgType {
		public final static String TEXT = "text";
		public final static String VOICE = "voice";
		public final static String IMAGE = "image";
		public final static String VIDEO = "video";
		public final static String FILE = "file";
		public final static String ROBOT = "robot";
		public final static String EVENT = "event";
		public final static String NOTIFICATION = "notification";
		public final static String TIP = "tip";
		public final static String LINK = "url";
		public final static String LOCATION = "location";
		public final static String CUSTOM = "custom";
	}

	public class MsgStatus {

		public final static String SEND_DRAFT = "send_draft";
		public final static String SEND_FAILE = "send_failed";
		public final static String SEND_SENDING = "send_going";
		public final static String SEND_SUCCESS = "send_succeed";
		public final static String RECEIVE_READ = "receive_read";
		public final static String RECEIVE_UNREAD = "receive_unread";
	}

	public class Message {

		public static final String MSG_ID = "msgId";
		public static final String MSG_TYPE = "msgType";
		public static final String IS_OUTGOING = "isOutgoing";
		public static final String TIME_STRING = "timeString";
		public static final String MSG_TEXT = "text";
		public static final String STATUS = "status";

		public static final String FROM_USER = "fromUser";
		public static final String EXTEND = "extend";

		public static final String IS_REMOTE_READ = "isRemoteRead";
		public static final String ATTACH_STATUS = "attachStatus";
		public static final String SESSION_TYPE = "sessionType";
		public static final String SESSION_ID = "sessionId";

		public static final String TIME = "time";
	}

	public class Contact {
		public static final String CONTACT_ID = "contactId";
		public static final String UNREAD_COUNT = "unreadCount";
		public static final String TEAM_TYPE = "teamType";
		public static final String EXT = "ext";
		public static final String MUTE = "mute";
		public static final String IMAGE_PATH = "imagePath";
		public static final String NAME = "name";
		public static final String SESSION_TYPE = "sessionType";
		public static final String MSG_TYPE = "msgType";
		public static final String MSG_STATUS = "msgStatus";
		public static final String MESSAGE_ID = "messageId";
		public static final String FROM_ACCOUNT = "fromAccount";
		public static final String TIME = "time";
		public static final String NICK = "nick";
		public static final String CONTENT = "content";
		public static final String TYPE = "TYPE";
		public static final String RECENTS = "recents";
	}


	public static class User {
		public static final String USER_ID = "_id";
		public static final String DISPLAY_NAME = "name";
		public static final String AVATAR_PATH = "avatar";
	}

	public static class Location {
		public final static String LATITUDE = "latitude";
		public final static String LONGITUDE = "longitude";
		public final static String ADDRESS = "title";
	}

	public static class MediaFile {
		public final static String HEIGHT = "height";
		public final static String WIDTH = "width";
		public final static String DISPLAY_NAME = "displayName";
		public final static String DURATION = "duration";
		public final static String THUMB_PATH = "thumbPath";
		public final static String PATH = "path";
		public final static String URL = "url";

		public final static String SIZE = "size";
	}

	public static class Event {
		public final static String observeSDKInit = "observeSDKInit";//SDK初始化完成
		public final static String observeOnlineStatus = "observeOnlineStatus";//'在线状态'
		public final static String observeLoginSyncDataStatus = "observeLoginSyncDataStatus";//登录状态
		public final static String observeRecentContact = "observeRecentContact";//'最近会话''
		public final static String observeReceiveMessage = "observeReceiveMessage";//'接收消息'
		public final static String observeMsgStatus = "observeMsgStatus";//'发送消息状态变化'
		public final static String observeUserInfoUpdate = "observeUserInfoUpdate"; //'用户信息更新'
		public final static String observeSysUnreadCount = "observeSysUnreadCount";//'系统消息未读数'
		public final static String observeReceiveSystemMsg = "observeReceiveSystemMsg";//'系统消息'
		public final static String observeLaunchPushEvent = "observeLaunchPushEvent";//'前台推送'
		public final static String observeBackgroundPushEvent = "observeBackgroundPushEvent";//'后台推送'
	}
}
