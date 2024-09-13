package com.ybwdaisy.neteaseim;

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;

public class RNNeteaseImModule extends ReactContextBaseJavaModule implements ActivityEventListener {
    private final static String TAG = "RNNeteaseImModule";
    private final ReactApplicationContext reactContext;
    private final LoginService loginService;
    private final SessionService sessionService;

    public RNNeteaseImModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        ReactCache.setReactContext(reactContext);
        loginService = LoginService.getInstance();
        sessionService = SessionService.getInstance();
    }

    @NonNull
    @Override
    public String getName() {
        return "RNNeteaseIm";
    }

    @ReactMethod
    public void login(String account, String token, final Promise promise) {
        LoginInfo loginInfo = new LoginInfo(account, token);
        loginService.login(loginInfo, new RequestCallback<LoginInfo>() {
            @Override
            public void onSuccess(LoginInfo result) {
                Log.i(TAG, "login success: " + result);
                promise.resolve(result.getAccount());
            }

            @Override
            public void onFailed(int code) {
                String msg;
                if (code == 302 || code == 404) {
                    msg = "帐号或密码错误";
                } else {
                    msg = "登录失败:" + code;
                }
                Log.i(TAG, "login error: " + msg);
                promise.reject(Integer.toString(code), msg);
            }

            @Override
            public void onException(Throwable exception) {
                promise.reject(Integer.toString(ResponseCode.RES_EXCEPTION), "系统异常");
            }
        });
    }

    @ReactMethod
    public void logout() {
        loginService.logout();
    }

    @ReactMethod
    public void startSession(String sessionId, String sessionType) {
        sessionService.startSession(sessionId, sessionType);
    }

    @ReactMethod
    public void stopSession() {
        sessionService.stopSession();
    }

    @ReactMethod
    public void sendTextMessage(String content) {
        sessionService.sendTextMessage(content);
    }

    @ReactMethod
    public void sendImageMessage(String file, String displayName) {
        sessionService.sendImageMessage(file, displayName);
    }

    @ReactMethod
    public void sendCustomMessage(ReadableMap attachment) {
        sessionService.sendCustomMessage(attachment);
    }

    @ReactMethod
    public void updateCustomMessage(String messageId, ReadableMap attachment) {
        sessionService.updateCustomMessage(messageId, attachment);
    }

    @ReactMethod
    public void resendMessage(String messageId) {
        sessionService.resendMessage(messageId);
    }

    @ReactMethod
    public void deleteMessage(String messageId) {
        sessionService.deleteMessage(messageId);
    }

    @ReactMethod
    public void clearMessage() {
        sessionService.clearMessage();
    }

    @ReactMethod
    public void isMyFriend(final Promise promise) {
        boolean isMyFriend = sessionService.isMyFriend();
        promise.resolve(isMyFriend);
    }

    @ReactMethod
    public void getTotalUnreadCount(final Promise promise) {
        int unreadCount = sessionService.getTotalUnreadCount();
        promise.resolve(unreadCount);
    }

    @ReactMethod
    public void clearAllUnreadCount() {
        sessionService.clearAllUnreadCount();
    }

    @ReactMethod
    public void queryMessageListEx(String messageId, final int limit, final Promise promise) {
        sessionService.queryMessageListEx(messageId, limit, promise);
    }

    @ReactMethod
    public void queryRecentContacts(final Promise promise) {
        sessionService.queryRecentContacts(promise);
    }

    @ReactMethod
    public void deleteRecentContact(String account) {
        sessionService.deleteRecentContact(account);
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        //
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (reactContext.getCurrentActivity() != null
                && intent != null &&
                !NIMClient.getStatus().wontAutoLogin()
        ) {
            reactContext.getCurrentActivity().setIntent(intent);
            WritableMap map = Arguments.createMap();
            if (intent.hasExtra("EXTRA_JUMP_P2P")) {
                Intent data = intent.getParcelableExtra("data");
                String account = data != null ? data.getStringExtra("account") : "";
                if (!TextUtils.isEmpty(account)) {
                    WritableMap session = Arguments.createMap();
                    session.putString("sessionType", Integer.toString(SessionTypeEnum.P2P.getValue()));
                    session.putString("sessionId", account);
                    UserInfoCache userInfoCache = UserInfoCache.getInstance();
                    String sessionName = userInfoCache.getUserName(account);
                    session.putString("sessionName", sessionName);
                    map.putMap("session", session);
                    ReactCache.emit(MessageConstant.Event.observeBackgroundPushEvent, map);
                }
            }
        }
    }
}
