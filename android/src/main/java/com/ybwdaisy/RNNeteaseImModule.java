package com.ybwdaisy;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.auth.LoginInfo;

public class RNNeteaseImModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private LoginService loginService;
    private SessionService sessionService;

    public RNNeteaseImModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        ReactCache.setReactContext(reactContext);
        loginService = LoginService.getInstance();
        sessionService = SessionService.getInstance();
    }

    @Override
    public String getName() {
        return "RNNeteaseIm";
    }

    @ReactMethod
    public void login(String contactId, String token, final Promise promise) {
        LoginInfo loginInfo = new LoginInfo(contactId, token);

        loginService.login(loginInfo, new RequestCallback<LoginInfo>() {
            @Override
            public void onSuccess(LoginInfo result) {
                promise.resolve(loginInfo.getAccount());
            }

            @Override
            public void onFailed(int code) {
                String msg;
                if (code == 302 || code == 404) {
                    msg = "帐号或密码错误";
                } else {
                    msg = "登录失败:" + code;
                }
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
    public boolean isMyFriend() {
        return sessionService.isMyFriend();
    }

    @ReactMethod
    public int getTotalUnreadCount() {
        return sessionService.getTotalUnreadCount();
    }

    @ReactMethod
    public void clearAllUnreadCount() {
        sessionService.clearAllUnreadCount();
    }

    @ReactMethod
    public void queryRecentContacts(final Promise promise) {
        sessionService.queryRecentContacts(promise);
    }

    @ReactMethod
    public void deleteRecentContact(String account) {
        sessionService.deleteRecentContact(account);
    }

    @ReactMethod
    public void queryMessageListEx(String messageId, final int limit, final Promise promise) {
        sessionService.queryMessageListEx(messageId, limit, promise);
    }
}
