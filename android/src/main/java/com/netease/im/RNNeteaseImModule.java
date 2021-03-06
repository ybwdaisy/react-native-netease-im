
package com.netease.im;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;
import com.netease.im.common.ResourceUtil;
import com.netease.im.contact.BlackListObserver;
import com.netease.im.contact.FriendListService;
import com.netease.im.contact.FriendObserver;
import com.netease.im.login.LoginService;
import com.netease.im.login.RecentContactObserver;
import com.netease.im.login.SysMessageObserver;
import com.netease.im.session.AudioMessageService;
import com.netease.im.session.AudioPlayService;
import com.netease.im.session.SessionService;
import com.netease.im.session.SessionUtil;
import com.netease.im.team.TeamListService;
import com.netease.im.team.TeamObserver;
import com.netease.im.uikit.cache.NimUserInfoCache;
import com.netease.im.uikit.cache.SimpleCallback;
import com.netease.im.uikit.cache.TeamDataCache;
import com.netease.im.uikit.common.util.log.LogUtil;
import com.netease.im.uikit.common.util.sys.NetworkUtil;
import com.netease.im.uikit.contact.core.model.ContactDataList;
import com.netease.im.uikit.permission.MPermission;
import com.netease.im.uikit.permission.annotation.OnMPermissionDenied;
import com.netease.im.uikit.permission.annotation.OnMPermissionGranted;
import com.netease.im.uikit.permission.annotation.OnMPermissionNeverAskAgain;
import com.netease.im.uikit.session.helper.MessageHelper;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.RequestCallbackWrapper;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.friend.FriendService;
import com.netease.nimlib.sdk.friend.constant.FriendFieldEnum;
import com.netease.nimlib.sdk.friend.constant.VerifyType;
import com.netease.nimlib.sdk.friend.model.AddFriendData;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.SystemMessageService;
import com.netease.nimlib.sdk.msg.constant.MsgStatusEnum;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.QueryDirectionEnum;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.msg.model.SystemMessage;
import com.netease.nimlib.sdk.nos.NosService;
import com.netease.nimlib.sdk.team.TeamService;
import com.netease.nimlib.sdk.team.constant.TeamBeInviteModeEnum;
import com.netease.nimlib.sdk.team.constant.TeamFieldEnum;
import com.netease.nimlib.sdk.team.constant.TeamInviteModeEnum;
import com.netease.nimlib.sdk.team.constant.TeamMessageNotifyTypeEnum;
import com.netease.nimlib.sdk.team.constant.TeamTypeEnum;
import com.netease.nimlib.sdk.team.constant.TeamUpdateModeEnum;
import com.netease.nimlib.sdk.team.constant.VerifyTypeEnum;
import com.netease.nimlib.sdk.team.model.CreateTeamResult;
import com.netease.nimlib.sdk.team.model.Team;
import com.netease.nimlib.sdk.team.model.TeamMember;
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo;
import com.netease.nimlib.sdk.uinfo.model.UserInfo;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.netease.im.ReceiverMsgParser.getIntent;


public class RNNeteaseImModule extends ReactContextBaseJavaModule implements LifecycleEventListener, ActivityEventListener {

    final static int BASIC_PERMISSION_REQUEST_CODE = 100;
    private final static String TAG = "RNNeteaseIm";
    private final static String NAME = "RNNeteaseIm";
    private final ReactApplicationContext reactContext;
    private AudioMessageService audioMessageService;
    private AudioPlayService audioPlayService;
    FriendListService friendListService;
    FriendObserver friendObserver;
    private Handler handler = new Handler(Looper.getMainLooper());

    public RNNeteaseImModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(this);
        reactContext.addLifecycleEventListener(this);
        ReactCache.setReactContext(reactContext);
        audioMessageService = AudioMessageService.getInstance();
        audioPlayService = AudioPlayService.getInstance();
        friendListService = new FriendListService();
        friendObserver = new FriendObserver();
    }

    @Override
    public void initialize() {
        LogUtil.w(TAG, "initialize");
    }

    @Override
    public void onCatalystInstanceDestroy() {
        LogUtil.w(TAG, "onCatalystInstanceDestroy");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void init(Promise promise) {
        LogUtil.w(TAG, "init");
        promise.resolve("200");
    }

    /**
     * ??????
     *
     * @param contactId
     * @param token
     * @param promise
     */
    @ReactMethod
    public void login(String contactId, String token, final Promise promise) {
        LogUtil.w(TAG, "_id:" + contactId);
        LogUtil.w(TAG, "t:" + token);
//        LogUtil.w(TAG, "md5:" + MD5.getStringMD5(token));

        NIMClient.getService(AuthService.class).openLocalCache(contactId);
        LogUtil.w(TAG, "s:" + NIMClient.getStatus().name());
        LoginService.getInstance().login(new LoginInfo(contactId, token), new RequestCallback<LoginInfo>() {
            @Override
            public void onSuccess(LoginInfo loginInfo) {

                promise.resolve(loginInfo == null ? "" : loginInfo.getAccount());
            }

            @Override
            public void onFailed(int code) {
                String msg;
                if (code == 302 || code == 404) {
                    msg = ResourceUtil.getString(R.string.login_failed);
                } else {
                    msg = ResourceUtil.getString(R.string.login_erro) + code;
                }
                promise.reject(Integer.toString(code), msg);
            }

            @Override
            public void onException(Throwable throwable) {
                promise.reject(Integer.toString(ResponseCode.RES_EXCEPTION), ResourceUtil.getString(R.string.login_exception));

            }
        });
    }

    /**
     * ??????
     */
    @ReactMethod
    public void logout() {
        LogUtil.w(TAG, "logout");
        status = "";
        LoginService.getInstance().logout();

    }

    /**********Friend ??????**************/


    /**
     * ????????????
     *
     * @param promise
     */
    @ReactMethod
    public void startFriendList(final Promise promise) {
        LogUtil.w(TAG, "startFriendList");
        friendObserver.startFriendList();

    }

    /**
     * ????????????
     *
     * @param promise
     */
    @ReactMethod
    public void stopFriendList(final Promise promise) {
        LogUtil.w(TAG, "stopFriendList");
        friendObserver.stopFriendList();
    }

    /**
     * ????????????????????????
     *
     * @param contactId
     * @param promise
     */
    @ReactMethod
    public void getUserInfo(String contactId, final Promise promise) {
        LogUtil.w(TAG, "getUserInfo" + contactId);
        NimUserInfo userInfo = NimUserInfoCache.getInstance().getUserInfo(contactId);
        promise.resolve(ReactCache.createUserInfo(userInfo));
    }

    /**
     * ???????????????????????????
     *
     * @param contactId
     * @param promise
     */
    @ReactMethod
    public void fetchUserInfo(String contactId, final Promise promise) {
        LogUtil.w(TAG, "fetchUserInfo" + contactId);
        NimUserInfoCache.getInstance().getUserInfoFromRemote(contactId, new RequestCallbackWrapper<NimUserInfo>() {
            @Override
            public void onResult(int i, NimUserInfo userInfo, Throwable throwable) {
                promise.resolve(ReactCache.createUserInfo(userInfo));
            }
        });
    }

    /**
     * ?????????????????????????????????
     *
     * @param userIds
     * @param promise
     */
    @ReactMethod
    public void batchFetchUserInfo(ReadableArray userIds, final Promise promise) {
        final WritableArray array = Arguments.createArray();
        NimUserInfoCache.getInstance().getUserInfoFromRemote(array2ListString(userIds), new RequestCallbackWrapper<List<NimUserInfo>>() {
            @Override
            public void onResult(int code, List<NimUserInfo> users, Throwable exception) {
                if (code == ResponseCode.RES_SUCCESS) {
                    for (NimUserInfo user : users) {
                        array.pushMap((WritableMap) ReactCache.createUserInfo(user));
                    }
                    promise.resolve(array);
                }
            }
        });
    }


    /**
     * ????????????
     *
     * @param contactId
     * @param verifyType 1 ????????????
     * @param msg        ??????
     * @param promise
     */
    @ReactMethod
    public void addFriendWithType(final String contactId, String verifyType, String msg, final Promise promise) {
        VerifyType verifyTypeAdd = VerifyType.VERIFY_REQUEST;
        if ("1".equals(verifyType)) {
            verifyTypeAdd = VerifyType.DIRECT_ADD;
        }
        LogUtil.w(TAG, "addFriend" + contactId);
        NIMClient.getService(FriendService.class).addFriend(new AddFriendData(contactId, verifyTypeAdd, msg))
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            String name = NimUserInfoCache.getInstance().getUserName(LoginService.getInstance().getAccount());
                            SessionUtil.sendAddFriendNotification(contactId, name + " ??????????????????");
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    /**
     * ????????????
     *
     * @param contactId
     * @param msg       ??????
     * @param promise
     */
    @ReactMethod
    public void addFriend(final String contactId, String msg, final Promise promise) {
        LogUtil.w(TAG, "addFriend" + contactId);
        NIMClient.getService(FriendService.class).addFriend(new AddFriendData(contactId, VerifyType.VERIFY_REQUEST, msg))
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            String name = NimUserInfoCache.getInstance().getUserName(LoginService.getInstance().getAccount());
                            SessionUtil.sendAddFriendNotification(contactId, name + " ??????????????????");
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    /**
     * ????????????
     *
     * @param contactId
     * @param promise
     */
    @ReactMethod
    public void deleteFriend(String contactId, final Promise promise) {
        LogUtil.w(TAG, "deleteFriend" + contactId);
        NIMClient.getService(FriendService.class).deleteFriend(contactId)
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
        SysMessageObserver sysMessageObserver = SysMessageObserver.getInstance();
        sysMessageObserver.loadMessages(false);
        sysMessageObserver.deleteSystemMessageById(contactId, false);
    }

    /*************Black ?????????***********/

    BlackListObserver blackListObserver = new BlackListObserver();

    /**
     * ?????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void startBlackList(final Promise promise) {
        blackListObserver.startBlackList();
    }

    /**
     * ?????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void stopBlackList(final Promise promise) {
        blackListObserver.stopBlackList();
    }

    /**
     * ?????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void getBlackList(final Promise promise) {
        final List<String> accounts = NIMClient.getService(FriendService.class).getBlackList();
        List<String> unknownAccounts = new ArrayList<>();
        final List<UserInfo> data = new ArrayList<>();
        for (String contactId : accounts) {
            if (!NimUserInfoCache.getInstance().hasUser(contactId)) {
                unknownAccounts.add(contactId);
            } else {
                data.add(NimUserInfoCache.getInstance().getUserInfo(contactId));
            }
        }

        if (!unknownAccounts.isEmpty()) {
            NimUserInfoCache.getInstance().getUserInfoFromRemote(unknownAccounts, new RequestCallbackWrapper<List<NimUserInfo>>() {
                @Override
                public void onResult(int code, List<NimUserInfo> users, Throwable exception) {
                    if (code == ResponseCode.RES_SUCCESS) {
                        data.addAll(users);
                    }
                    promise.resolve(ReactCache.createBlackList(data));
                }
            });
        } else {
            promise.resolve(ReactCache.createBlackList(data));
        }
    }

    /**
     * ???????????????
     *
     * @param contactId
     * @param promise
     */
    @ReactMethod
    public void addToBlackList(String contactId, final Promise promise) {
        blackListObserver.addToBlackList(contactId, new RequestCallbackWrapper<Void>() {
            @Override
            public void onResult(int code, Void aVoid, Throwable throwable) {
                if (code == ResponseCode.RES_SUCCESS) {
                    promise.resolve("" + code);
                } else {
                    promise.reject("" + code, "");
                }
            }
        });
    }

    /**
     * ???????????????
     *
     * @param contactId
     * @param promise
     */
    @ReactMethod
    public void removeFromBlackList(String contactId, final Promise promise) {
        blackListObserver.removeFromBlackList(contactId, new RequestCallbackWrapper<Void>() {
            @Override
            public void onResult(int code, Void aVoid, Throwable throwable) {
                if (code == ResponseCode.RES_SUCCESS) {
                    promise.resolve("" + code);
                } else {
                    promise.reject("" + code, "");
                }
            }
        });
    }

    /************Team ??????************/

    TeamObserver teamObserver = new TeamObserver();

    /**
     * ??????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void startTeamList(Promise promise) {
        teamObserver.startTeamList();
    }

    /**
     * ??????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void stopTeamList(Promise promise) {
        teamObserver.stopTeamList();
    }


    /**
     * ?????????????????????
     *
     * @param teamId
     * @param promise
     */
    @ReactMethod
    public void getTeamInfo(String teamId, Promise promise) {

        Team team = TeamDataCache.getInstance().getTeamById(teamId);
        promise.resolve(ReactCache.createTeamInfo(team));
    }

    /**
     * ??????/?????????????????? ??????
     *
     * @param contactId
     * @param mute
     * @param promise
     */
    @ReactMethod
    public void setMessageNotify(String contactId, String mute, final Promise promise) {
        NIMClient.getService(FriendService.class).setMessageNotify(contactId, string2Boolean(mute))
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            RecentContactObserver.getInstance().refreshMessages(true);
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    /**
     * ??????/?????????????????? ??????
     *
     * @param teamId
     * @param mute
     * @param promise
     */
    @ReactMethod
    public void setTeamNotify(String teamId, String mute, final Promise promise) {

        TeamMessageNotifyTypeEnum typeEnum = TeamMessageNotifyTypeEnum.All;
        if ("0".equals(mute)) {
            typeEnum = TeamMessageNotifyTypeEnum.Mute;
        } else if ("1".equals(mute)) {
            typeEnum = TeamMessageNotifyTypeEnum.All;
        } else if ("2".equals(mute)) {
            typeEnum = TeamMessageNotifyTypeEnum.Manager;
        }
        NIMClient.getService(TeamService.class).muteTeam(teamId, typeEnum)//!string2Boolean(mute)
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            RecentContactObserver.getInstance().refreshMessages(true);
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    /**
     * ???????????????
     *
     * @param teamId
     * @param contactId
     * @param mute
     * @param promise
     */
    @ReactMethod
    public void setTeamMemberMute(String teamId, String contactId, String mute, final Promise promise) {

        NIMClient.getService(TeamService.class).muteTeamMember(teamId, contactId, string2Boolean(mute))
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    /**
     * ???????????????????????????
     *
     * @param teamId
     * @param promise
     */
    @ReactMethod
    public void getTeamMemberList(String teamId, Promise promise) {

        List<TeamMember> teamMemberList = TeamDataCache.getInstance().getTeamMemberList(teamId);
        promise.resolve(ReactCache.createTeamMemberList(teamMemberList));
    }

    /**
     * ????????????????????????
     *
     * @param teamId
     * @param promise
     */
    @ReactMethod
    public void fetchTeamInfo(String teamId, final Promise promise) {
        TeamDataCache.getInstance().fetchTeamById(teamId, new SimpleCallback<Team>() {
            @Override
            public void onResult(boolean success, Team team) {
                if (success && team != null) {
                    promise.resolve(ReactCache.createTeamInfo(team));
                } else {
                    promise.reject("-1", "");
                }
            }
        });
    }

    /**
     * ??????????????????????????????
     *
     * @param teamId
     * @param promise
     */
    @ReactMethod
    public void fetchTeamMemberList(String teamId, final Promise promise) {
        TeamDataCache.getInstance().fetchTeamMemberList(teamId, new SimpleCallback<List<TeamMember>>() {
            @Override
            public void onResult(boolean success, List<TeamMember> result) {
                if (success && result != null) {
                    promise.resolve(ReactCache.createTeamMemberList(result));
                } else {
                    promise.reject("-1", "");
                }
            }
        });
    }

    /**
     * ?????????????????????
     *
     * @param teamId
     * @param contactId
     * @param nick
     * @param promise
     */
    @ReactMethod
    public void updateMemberNick(String teamId, String contactId, String nick, final Promise promise) {
        NIMClient.getService(TeamService.class).updateMemberNick(teamId, contactId, nick)
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            promise.resolve("" + code);
                        } else {
                            promise.reject("-1", "");
                        }
                    }
                });

    }

    /**
     * ??????????????????????????????
     *
     * @param teamId
     * @param contactId
     * @param promise
     */
    @ReactMethod
    public void fetchTeamMemberInfo(String teamId, String contactId, final Promise promise) {
        TeamMember teamMember = TeamDataCache.getInstance().getTeamMember(teamId, contactId);
        if (teamMember != null) {
            promise.resolve(ReactCache.createTeamMemberInfo(teamMember));
        } else {
            // ???????????????
            TeamDataCache.getInstance().fetchTeamMember(teamId, contactId, new SimpleCallback<TeamMember>() {
                @Override
                public void onResult(boolean success, TeamMember member) {
                    if (success && member != null) {
                        promise.resolve(ReactCache.createTeamMemberInfo(member));
                    } else {
                        promise.reject("-1", "");
                    }
                }
            });
        }
    }


    /**
     * ????????????
     * verifyType ???????????? 0 ????????????????????? 1 ??????????????????2 ??????????????????????????????
     * inviteMode ?????????????????? 0??????????????? 1???????????????
     * beInviteMode ?????????????????? 0???????????? 1???????????????
     * teamUpdateMode ????????????????????? 0??????????????? 1???????????????
     *
     * @param fields
     * @param type
     * @param accounts
     * @param promise
     */
    @ReactMethod
    public void createTeam(ReadableMap fields, String type, ReadableArray accounts, final Promise promise) {
        LogUtil.w(TAG, fields + "\n" + type + "\n" + accounts);
        TeamTypeEnum teamTypeEnum = TeamTypeEnum.Advanced;
        try {
            teamTypeEnum = TeamTypeEnum.typeOfValue(Integer.parseInt(type));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        HashMap<TeamFieldEnum, Serializable> fieldsMap = new HashMap<TeamFieldEnum, Serializable>();
        String teamName = teamTypeEnum == TeamTypeEnum.Normal ? "?????????" : "?????????";
        if (fields != null) {
            if (fields.hasKey("name")) {
                teamName = fields.getString("name");
            }
            if (teamTypeEnum == TeamTypeEnum.Advanced) {
                if (fields.hasKey("introduce"))
                    fieldsMap.put(TeamFieldEnum.Introduce, fields.getString("introduce"));
                VerifyTypeEnum verifyTypeEnum = VerifyTypeEnum.Free;
                if (fields.hasKey("verifyType")) {//???????????? 0 ????????????????????? 1 ??????????????????2 ??????????????????????????????
                    try {
                        verifyTypeEnum = VerifyTypeEnum.typeOfValue(Integer.parseInt(fields.getString("verifyType")));

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                fieldsMap.put(TeamFieldEnum.VerifyType, verifyTypeEnum);

                TeamBeInviteModeEnum teamBeInviteModeEnum = TeamBeInviteModeEnum.NoAuth;
                if (fields.hasKey("beInviteMode")) {//?????????????????? 0???????????? 1???????????????
                    try {
                        teamBeInviteModeEnum = TeamBeInviteModeEnum.typeOfValue(Integer.parseInt(fields.getString("beInviteMode")));

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                fieldsMap.put(TeamFieldEnum.BeInviteMode, teamBeInviteModeEnum);

                TeamInviteModeEnum teamInviteModeEnum = TeamInviteModeEnum.All;
                if (fields.hasKey("inviteMode")) {//?????????????????? 0??????????????? 1???????????????
                    try {
                        teamInviteModeEnum = TeamInviteModeEnum.typeOfValue(Integer.parseInt(fields.getString("inviteMode")));

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                fieldsMap.put(TeamFieldEnum.InviteMode, teamInviteModeEnum);
                TeamUpdateModeEnum teamUpdateModeEnum = TeamUpdateModeEnum.All;
                if (fields.hasKey("teamUpdateMode")) {//?????????????????? 0??????????????? 1???????????????
                    try {
                        teamUpdateModeEnum = TeamUpdateModeEnum.typeOfValue(Integer.parseInt(fields.getString("teamUpdateMode")));

                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                fieldsMap.put(TeamFieldEnum.TeamUpdateMode, teamUpdateModeEnum);
            }
        }
        fieldsMap.put(TeamFieldEnum.Name, teamName);
        final String finalTeamName = teamName;
        NIMClient.getService(TeamService.class).createTeam(fieldsMap, teamTypeEnum, "", array2ListString(accounts))
                .setCallback(new RequestCallbackWrapper<CreateTeamResult>() {
                    @Override
                    public void onResult(int code, CreateTeamResult createTeamResult, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {

                            Team team = createTeamResult.getTeam();
                            MessageHelper.getInstance().onCreateTeamMessage(team);
                            WritableMap id = Arguments.createMap();
                            id.putString("teamId", team.getId());
                            promise.resolve(id);
                        } else if (code == 801) {
                            promise.reject("" + code, reactContext.getString(R.string.over_team_member_capacity, 200));
                        } else if (code == 806) {
                            promise.reject("" + code, reactContext.getString(R.string.over_team_capacity));
                        } else {
                            promise.reject("" + code, "??????" + finalTeamName + "??????");
                        }
                    }
                });
    }

    @ReactMethod
    public void upload(String file, final Promise promise) {
        if (TextUtils.isEmpty(file)) {
            return;
        }

        File f = new File(file);
        if (f == null) {
            return;
        }
        NIMClient.getService(NosService.class).upload(f, "image/jpeg").setCallback(new RequestCallbackWrapper<String>() {
            @Override
            public void onResult(int code, String url, Throwable exception) {
                if (code == ResponseCode.RES_SUCCESS) {
                    promise.resolve(url);
                } else {
                    promise.reject("" + code, "" + url);
                }
            }
        });
    }

    /**
     * ???????????????
     * verifyType ???????????? 0 ????????????????????? 1 ??????????????????2 ??????????????????????????????
     * inviteMode ?????????????????? 0??????????????? 1???????????????
     * beInviteMode ?????????????????? 0???????????? 1???????????????
     * teamUpdateMode ????????????????????? 0??????????????? 1???????????????
     *
     * @param teamId
     * @param fieldType:name(????????????) icon(??????) introduce(????????????) announcement(????????????)
     *                             verifyType(????????????) inviteMode(??????????????????) beInviteMode(??????????????????) teamUpdateMode(?????????????????????)
     * @param value
     * @param promise
     */
    @ReactMethod
    public void updateTeam(String teamId, String fieldType, String value, final Promise promise) {

        if (TextUtils.isEmpty(teamId) || TextUtils.isEmpty(fieldType) || TextUtils.isEmpty(value)) {
            promise.reject("-1", "????????????");
            return;
        }
        TeamFieldEnum teamFieldEnum = null;
        Serializable fieldValue = null;
        switch (fieldType) {
            case "name":
                teamFieldEnum = TeamFieldEnum.Name;
                fieldValue = value;
                break;
            case "icon":
                teamFieldEnum = TeamFieldEnum.ICON;
                fieldValue = value;
                break;
            case "introduce":
                teamFieldEnum = TeamFieldEnum.Introduce;
                fieldValue = value;
                break;
            case "announcement":
                teamFieldEnum = TeamFieldEnum.Announcement;
                fieldValue = value;
                break;
            case "verifyType":
                teamFieldEnum = TeamFieldEnum.VerifyType;
                try {
                    fieldValue = VerifyTypeEnum.typeOfValue(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                break;
            case "inviteMode":
                teamFieldEnum = TeamFieldEnum.InviteMode;
                try {
                    fieldValue = TeamInviteModeEnum.typeOfValue(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                break;
            case "beInviteMode":
                teamFieldEnum = TeamFieldEnum.BeInviteMode;
                try {
                    fieldValue = TeamBeInviteModeEnum.typeOfValue(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                break;
            case "teamUpdateMode":
                teamFieldEnum = TeamFieldEnum.TeamUpdateMode;
                try {
                    fieldValue = TeamUpdateModeEnum.typeOfValue(Integer.parseInt(value));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        if (teamFieldEnum == null || fieldValue == null) {
            promise.reject("-1", "????????????");
            return;
        }
        NIMClient.getService(TeamService.class).updateTeam(teamId, teamFieldEnum, fieldValue).setCallback(new RequestCallbackWrapper<Void>() {
            @Override
            public void onResult(int code, Void aVoid, Throwable throwable) {
                if (code == ResponseCode.RES_SUCCESS) {
                    promise.resolve("" + code);
                }
            }
        });
    }

    @ReactMethod
    public void updateTeamFields(String teamId, ReadableMap fields, final Promise promise) {
        Map<TeamFieldEnum, Serializable> fieldsMap = null;
        NIMClient.getService(TeamService.class).updateTeamFields(teamId, fieldsMap).setCallback(new RequestCallbackWrapper<Void>() {
            @Override
            public void onResult(int code, Void aVoid, Throwable throwable) {

            }
        });
    }

    /**
     * ??????????????????
     *
     * @param teamId
     * @param reason
     * @param promise
     */
    @ReactMethod
    public void applyJoinTeam(String teamId, String reason, final Promise promise) {
        NIMClient.getService(TeamService.class).applyJoinTeam(teamId, reason).setCallback(new RequestCallbackWrapper<Team>() {
            @Override
            public void onResult(int code, Team team, Throwable throwable) {
                if (code == ResponseCode.RES_SUCCESS) {
                    promise.resolve("" + code);
                } else {
                    promise.reject("" + code, "");
                }
            }
        });
    }

    /**
     * ????????????
     *
     * @param teamId
     * @param promise
     */
    @ReactMethod
    public void dismissTeam(String teamId, final Promise promise) {
        NIMClient.getService(TeamService.class).dismissTeam(teamId)
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    List<String> array2ListString(ReadableArray accounts) {
        List<String> memberAccounts = new ArrayList<>();
        if (accounts != null && accounts.size() > 0) {
            for (int i = 0; i < accounts.size(); i++) {
                if (accounts.getType(i) == ReadableType.String) {
                    String account = accounts.getString(i);
                    if (TextUtils.isEmpty(account)) {
                        continue;
                    }
                    memberAccounts.add(account);
                }
            }
        }
        return memberAccounts;
    }

    /**
     * ????????????
     *
     * @param teamId
     * @param accounts
     * @param promise
     */
    @ReactMethod
    public void addMembers(String teamId, ReadableArray accounts, final Promise promise) {


        NIMClient.getService(TeamService.class).addMembers(teamId, array2ListString(accounts))
                .setCallback(new RequestCallbackWrapper<List<String>>() {
                    @Override
                    public void onResult(int code, List<String> strings, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            promise.resolve("" + code);
                        } else if (code == ResponseCode.RES_TEAM_INVITE_SUCCESS) {
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    /**
     * ????????????
     *
     * @param teamId
     * @param accounts
     * @param promise
     */
    @ReactMethod
    public void removeMember(String teamId, ReadableArray accounts, final Promise promise) {

        NIMClient.getService(TeamService.class).removeMembers(teamId, array2ListString(accounts))
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    /**
     * ????????????
     *
     * @param teamId
     * @param promise
     */
    @ReactMethod
    public void quitTeam(String teamId, final Promise promise) {
        NIMClient.getService(TeamService.class).quitTeam(teamId)
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    boolean string2Boolean(String bool) {
        return TextUtils.isEmpty(bool) ? false : !"0".equals(bool);
    }

    /**
     * ????????????
     *
     * @param teamId    ???ID
     * @param contactId ??????????????????????????????
     * @param quit      ????????????????????????????????????
     * @param promise
     * @return InvocationFuture ??????????????????????????????????????????????????? quit ??????
     * quit???false???????????????????????????????????????????????????(??????????????? contactId)????????????????????????
     * quit???true: ???????????????
     */
    @ReactMethod
    public void transferTeam(String teamId, String contactId, String quit, final Promise promise) {

        NIMClient.getService(TeamService.class).transferTeam(teamId, contactId, string2Boolean(quit))
                .setCallback(new RequestCallbackWrapper<List<TeamMember>>() {
                    @Override
                    public void onResult(int code, List<TeamMember> teamMembers, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    /**
     * ??????????????????
     *
     * @param teamId
     * @param nick
     * @param promise
     */
    @ReactMethod
    public void updateTeamName(String teamId, String nick, final Promise promise) {
        NIMClient.getService(TeamService.class).updateName(teamId, nick)
                .setCallback(new RequestCallbackWrapper<Void>() {
                    @Override
                    public void onResult(int code, Void aVoid, Throwable throwable) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            promise.resolve("" + code);
                        } else {
                            promise.reject("" + code, "");
                        }
                    }
                });
    }

    /*************Session send message ??????***********/
    /***sessionId,   ??????????????? ID?????????????????????????????????????????????????????????????????? ID***/
    /***   sessionType,   ??????????????????????????????***/
    /**
     * ??????????????????
     *
     * @param content   ????????????
     * @param atUserIds
     * @param promise
     */
    @ReactMethod
    public void sendTextMessage(String content, ReadableArray atUserIds, final Promise promise) {
        LogUtil.w(TAG, "sendTextMessage" + content);

        List<String> atUserIdList = array2ListString(atUserIds);
        sessionService.sendTextMessage(content, atUserIdList, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    public void sendTextMessage(String content, final Promise promise) {
        LogUtil.w(TAG, "sendTextMessage" + content);
        sessionService.sendTextMessage(content, null, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    //2.??????????????????
//    file, // ??????????????????
//    displayName // ???????????????????????????????????? APP ????????????????????? null
    @ReactMethod
    public void sendImageMessage(String file, String displayName, final Promise promise) {
        sessionService.sendImageMessage(file, displayName, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    //3.??????????????????
//    file, // ????????????
//    duration // ??????????????????????????????ms
    @ReactMethod
    public void sendAudioMessage(String file, String duration, final Promise promise) {
        long durationL = 0;
        try {
            durationL = Long.parseLong(duration);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        sessionService.sendAudioMessage(file, durationL, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    //4.??????????????????
//    file, // ????????????
//    duration, // ??????????????????
//    width, // ????????????
//    height, // ????????????
//    displayName // ???????????????????????????
    @ReactMethod
    public void sendVideoMessage(String file, String duration, int width, int height, String displayName, final Promise promise) {
        sessionService.sendVideoMessage(file, duration, width, height, displayName, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    @ReactMethod
    public void sendCustomMessage(ReadableMap attachment) {
        sessionService.sendCustomMessage(attachment, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    @ReactMethod
    public void updateCustomMessage(String messageId, ReadableMap attachment) {
        sessionService.updateCustomMessage(messageId, attachment);
    }

    @ReactMethod
    public void sendRedPacketOpenMessage(String sendId, String hasRedPacket, String serialNo, final Promise promise) {
        sessionService.sendRedPacketOpenMessage(sendId, LoginService.getInstance().getAccount(), hasRedPacket, serialNo, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    @ReactMethod
    public void sendCardMessage(String type, String name, String imgPath, String sessionId, final Promise promise) {
        sessionService.sendCardMessage(type, name, imgPath, sessionId, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });

    }

    //5.?????????????????????
//    attachment, // ?????????????????????
//    config // ????????????????????????????????????
    @ReactMethod
    public void sendRedPacketMessage(String type, String comments, String serialNo, final Promise promise) {
        sessionService.sendRedPacketMessage(type, comments, serialNo, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    //5.?????????????????????
//    attachment, // ?????????????????????
//    config // ????????????????????????????????????
    @ReactMethod
    public void sendBankTransferMessage(String amount, String comments, String serialNo, final Promise promise) {
        sessionService.sendBankTransferMessage(amount, comments, serialNo, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    //6.????????????????????????
//    latitude, // ??????
//    longitude, // ??????
//    address // ??????????????????
    @ReactMethod
    public void sendLocationMessage(String latitude, String longitude, String address, final Promise promise) {
        sessionService.sendLocationMessage(latitude, longitude, address, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    //7.??????????????????
//    content   //????????????
    @ReactMethod
    public void sendTipMessage(String content, final Promise promise) {
        sessionService.sendTipMessage(content, new SessionService.OnSendMessageListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                return 0;
            }
        });
    }

    /**
     * ??????????????????
     *
     * @param messageId
     * @param promise
     */
    @ReactMethod
    public void downloadAttachment(String messageId, final String isThumb, final Promise promise) {
        sessionService.queryMessage(messageId, new SessionService.OnMessageQueryListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                if (message != null) {
                    sessionService.downloadAttachment(message, string2Boolean(isThumb));
                    promise.resolve("????????????");
                } else {
                    promise.resolve("????????????");
                }
                return 0;
            }
        });

    }


    /**
     * ??????????????????
     *
     * @param messageId
     * @param sessionId
     * @param sessionType
     * @param content
     * @param promise
     */
    @ReactMethod
    public void sendForwardMessage(String messageId, final String sessionId, final String sessionType, final String content, final Promise promise) {
        LogUtil.w(TAG, "sendForwardMessage" + content);

        sessionService.queryMessage(messageId, new SessionService.OnMessageQueryListener() {
            @Override
            public int onResult(int code, IMMessage message) {

                int result = sessionService.sendForwardMessage(message, sessionId, sessionType, content, new SessionService.OnSendMessageListener() {
                    @Override
                    public int onResult(int code, IMMessage message) {
                        return 0;
                    }
                });
                if (result == 0) {
                    showTip("???????????????");
                } else if (result == 1) {
                    showTip("??????????????????????????????");
                } else {
                    promise.resolve(ResponseCode.RES_SUCCESS + "");
                }
                return 0;
            }
        });
    }

    /**
     * ????????????
     *
     * @param messageId
     * @param promise
     */
    @ReactMethod
    public void revokeMessage(String messageId, final Promise promise) {
        LogUtil.w(TAG, "revokeMessage" + messageId);
        sessionService.queryMessage(messageId, new SessionService.OnMessageQueryListener() {

            @Override
            public int onResult(int code, IMMessage message) {

                int result = sessionService.revokeMessage(message, new SessionService.OnSendMessageListener() {
                    @Override
                    public int onResult(int code, IMMessage message) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            promise.resolve("" + code);
                        } else if (code == ResponseCode.RES_OVERDUE) {
                            showTip(R.string.revoke_failed);
                        } else {
                            promise.reject("" + code, "");
                        }
                        return 0;
                    }
                });
                if (result == 0) {
                    showTip("???????????????");
                } else if (result == 1) {
                    showTip("??????????????????????????????");
                }
                return 0;
            }
        });

    }

    @ReactMethod
    public void updateAudioMessagePlayStatus(String messageId, final Promise promise) {
        LogUtil.w(TAG, "updateAudioMessagePlayStatus" + messageId);
        sessionService.queryMessage(messageId, new SessionService.OnMessageQueryListener() {

            @Override
            public int onResult(int code, IMMessage message) {
                if (code == ResponseCode.RES_SUCCESS && message != null) {
                    sessionService.updateMessage(message, MsgStatusEnum.read);
                }
                return 0;
            }
        });
    }

    /**
     * ????????????
     *
     * @param messageId
     * @param promise
     */
    @ReactMethod
    public void deleteMessage(String messageId, final Promise promise) {
        LogUtil.w(TAG, "deleteMessage" + messageId);
        sessionService.queryMessage(messageId, new SessionService.OnMessageQueryListener() {

            @Override
            public int onResult(int code, IMMessage message) {
                if (message != null) {
                    sessionService.deleteItem(message, true);
                } else {
                    showTip("???????????????");
                }
                return 0;
            }
        });
    }

    /**
     * ??????????????????
     * ????????????????????????????????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void clearMessage(String sessionId, String sessionType, final Promise promise) {

        SessionTypeEnum sessionTypeEnum = SessionUtil.getSessionType(sessionType);
        NIMClient.getService(MsgService.class).clearChattingHistory(sessionId, sessionTypeEnum);
    }

    /**
     * ??????????????????
     *
     * @param name
     * @param promise
     */
    @ReactMethod
    public void updateMyUserInfo(String name, final Promise promise) {
        String contactId = LoginService.getInstance().getAccount();
        NimUserInfoCache.getInstance().getUserInfoFromRemote(contactId, new RequestCallbackWrapper<NimUserInfo>() {
            @Override
            public void onResult(int i, NimUserInfo userInfo, Throwable throwable) {
            }
        });
    }

    /**
     * ??????????????????
     *
     * @param contactId
     * @param alias
     * @param promise
     */
    @ReactMethod
    public void updateUserInfo(String contactId, String alias, final Promise promise) {
        Map<FriendFieldEnum, Object> map = new HashMap<>();
        map.put(FriendFieldEnum.ALIAS, alias);
        NIMClient.getService(FriendService.class).updateFriendFields(contactId, map).setCallback(new RequestCallbackWrapper<Void>() {
            @Override
            public void onResult(int code, Void aVoid, Throwable throwable) {
                if (code == ResponseCode.RES_SUCCESS) {
                    promise.resolve("" + code);
                } else {
                    promise.reject("" + code, "");
                }
            }
        });
    }

    /**
     * ????????????????????????
     *
     * @param messageId
     * @param promise
     */
    @ReactMethod
    public void resendMessage(String messageId, final Promise promise) {
        LogUtil.w(TAG, "resendMessage" + messageId);
        sessionService.queryMessage(messageId, new SessionService.OnMessageQueryListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                Map<String, Object> map = message.getLocalExtension();
                if (map != null) {
                    if (map.containsKey("resend")) {
                        return -1;
                    }
                }
                promise.resolve("200");
                sessionService.resendMessage(message);

                return 0;
            }
        });

    }

    /**
     * ??????????????????
     *
     * @param rContactId
     * @param promise
     */
    @ReactMethod
    public void deleteRecentContact(String rContactId, Promise promise) {
        LogUtil.w(TAG, "deleteRecentContact" + rContactId);
        boolean result = LoginService.getInstance().deleteRecentContact(rContactId);
        if (result) {
            promise.resolve("" + ResponseCode.RES_SUCCESS);
        } else {
            promise.reject("-1", "");
        }
    }

    @ReactMethod
    public void getRecentContactList(final Promise promise) {
        NIMClient.getService(MsgService.class).queryRecentContacts()
                .setCallback(new RequestCallbackWrapper<List<RecentContact>>() {

                    @Override
                    public void onResult(int code, List<RecentContact> recentContacts, Throwable throwable) {
                        if (recentContacts != null && recentContacts.size() > 0) {
                            promise.resolve(ReactCache.createRecentList(recentContacts, 0));
                        } else {
                            promise.reject("-1", "");
                        }
                    }
                });
    }

    @ReactMethod
    public void getTeamList(String keyword, final Promise promise) {
        TeamListService teamListService = TeamListService.getInstance();
        teamListService.setOnLoadListener(new TeamListService.OnLoadListener() {
            @Override
            public void updateData(ContactDataList datas) {
                promise.resolve(ReactCache.createTeamList(datas));
            }
        });
        teamListService.query(keyword);
    }

    @ReactMethod
    public void getFriendList(String keyword, final Promise promise) {

        friendListService.setOnLoadListener(new FriendListService.OnLoadListener() {
            @Override
            public void updateData(ContactDataList datas) {
                promise.resolve(ReactCache.createFriendSet(datas, true));
            }
        });
        friendListService.query(keyword);
    }

    /************************/

    SessionService sessionService = SessionService.getInstance();

    /**
     * ??????????????????
     *
     * @param sessionId
     * @param type
     * @param promise
     */
    @ReactMethod
    public void startSession(String sessionId, String type, final Promise promise) {
        LogUtil.w(TAG, "startSession" + sessionId);
        if (TextUtils.isEmpty(sessionId)) {

            return;
        }
        sessionService.startSession(handler, sessionId, type);
    }

    /**
     * ??????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void stopSession(final Promise promise) {
        LogUtil.w(TAG, "stopSession");
        sessionService.stopSession();
    }

    /**
     * ??????????????????
     *
     * @param sessionId
     * @param sessionType
     * @param timeLong
     * @param direction   ???????????? old new ??????new
     * @param limit       ???????????????????????????
     * @param asc         ??????????????????????????????????????? true????????????????????????????????????????????? false???????????????????????????
     * @param promise
     */
    @ReactMethod
    public void queryMessageListHistory(String sessionId, String sessionType, String
            timeLong, String direction, int limit, String asc, final Promise promise) {
        LogUtil.w(TAG, "queryMessageListHistory");
        long time = 0;
        try {
            time = Long.parseLong(timeLong);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        SessionTypeEnum sessionTypeEnum = SessionUtil.getSessionType(sessionType);
        QueryDirectionEnum directionEnum = getQueryDirection(direction);
        IMMessage message = MessageBuilder.createEmptyMessage(sessionId, sessionTypeEnum, time);
        NIMClient.getService(MsgService.class).queryMessageListEx(message, directionEnum, limit, string2Boolean(asc))
                .setCallback(new RequestCallbackWrapper<List<IMMessage>>() {

                    @Override
                    public void onResult(int code, List<IMMessage> result, Throwable exception) {
                        if (code == ResponseCode.RES_SUCCESS) {
                            if (result != null && result.size() > 0) {
                                Object a = ReactCache.createMessageList(result);
                                promise.resolve(a);
                                return;

                            }
                        }
                        promise.reject("" + code, "");
                    }
                });
    }

    /**
     * ????????????????????????
     *
     * @param messageId ???????????????ID
     * @param limit     ???????????????????????????
     * @param promise
     */
    @ReactMethod
    public void queryMessageListEx(String messageId, final int limit, final Promise promise) {
        LogUtil.w(TAG, "queryMessageListEx:" + messageId + "(" + limit + ")");
        sessionService.queryMessage(messageId, new SessionService.OnMessageQueryListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                sessionService.queryMessageListEx(message, QueryDirectionEnum.QUERY_OLD, limit, new SessionService.OnMessageQueryListListener() {
                    @Override
                    public int onResult(int code, List<IMMessage> messageList, Set<String> timedItems) {
                        if (messageList == null || messageList.isEmpty()) {
                            promise.reject("" + code, "");
                        } else {
                            Object a = ReactCache.createMessageList(messageList);
                            promise.resolve(a);
                        }
                        return 0;
                    }
                });
                return 0;
            }
        });

    }

    /**
     * ?????? messageId ????????????????????????
     *
     * @param messageId
     * @param promise
     */
    @ReactMethod
    public void queryMessageById(String messageId, final Promise promise) {
        LogUtil.w(TAG, "queryMessageById:" + messageId);
        sessionService.queryMessage(messageId, new SessionService.OnMessageQueryListener() {
            @Override
            public int onResult(int code, IMMessage message) {
                if (message == null) {
                    promise.reject("" + code, "");
                } else {
                    WritableArray writableArray = Arguments.createArray();
                    WritableMap itemMap = ReactCache.createMessage(message);
                    if (itemMap != null) {
                        writableArray.pushMap(itemMap);
                    }
                    promise.resolve(writableArray);
                }
                return 0;
            }
        });

    }

    private QueryDirectionEnum getQueryDirection(String direction) {
        QueryDirectionEnum directionEnum = QueryDirectionEnum.QUERY_NEW;
        if ("old".equals(direction)) {
            directionEnum = QueryDirectionEnum.QUERY_OLD;
        }
        return directionEnum;
    }

    /**
     * ??????????????????
     */
    private final String[] BASIC_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
//            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
//            Manifest.permission.ACCESS_COARSE_LOCATION,
//            Manifest.permission.ACCESS_FINE_LOCATION
    };

    private void requestBasicPermission() {
        MPermission.printMPermissionResult(true, getCurrentActivity(), BASIC_PERMISSIONS);
        MPermission.with(getCurrentActivity())
                .setRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                .permissions(BASIC_PERMISSIONS)
                .request();
    }

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess() {
        Toast.makeText(getCurrentActivity(), "????????????", Toast.LENGTH_SHORT).show();
        MPermission.printMPermissionResult(false, getCurrentActivity(), BASIC_PERMISSIONS);
    }

    @OnMPermissionDenied(BASIC_PERMISSION_REQUEST_CODE)
    @OnMPermissionNeverAskAgain(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionFailed() {
        Toast.makeText(getCurrentActivity(), "?????????????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
        MPermission.printMPermissionResult(false, getCurrentActivity(), BASIC_PERMISSIONS);
    }

    /**
     * *****************************?????? ?????? ******************************************
     **/

    @ReactMethod
    public void onTouchVoice(Promise promise) {
        requestBasicPermission();

    }

    @ReactMethod
    public void startAudioRecord(Promise promise) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                getCurrentActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getCurrentActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                audioMessageService.startAudioRecord(reactContext);
            } else {
                requestBasicPermission();
            }
        } else {
            audioMessageService.startAudioRecord(reactContext);
        }
    }

    @ReactMethod
    public void endAudioRecord(Promise promise) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                getCurrentActivity().getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        audioMessageService.endAudioRecord(sessionService);


    }

    @ReactMethod
    public void cancelAudioRecord(Promise promise) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                getCurrentActivity().getWindow().setFlags(0, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        audioMessageService.cancelAudioRecord();
    }

    /** *******************????????????/?????? ??????/?????? ???????????? ????????????/?????? ???????????? ******/

    /**
     * ????????????
     *
     * @param audioFile
     * @param promise
     */
    @ReactMethod
    public void play(String audioFile, Promise promise) {
        audioPlayService.play(handler, reactContext, audioFile);
    }

    @ReactMethod
    public void playLocal(String resourceFile, String type, Promise promise) {

        Uri uri = Uri.parse(resourceFile);
        LogUtil.w(TAG, "scheme:" + uri.getScheme());
        String filePath = uri.getPath();
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
            if (filePath.indexOf(".") == -1) {
                filePath = filePath + "." + type;
            }
        }
        LogUtil.w(TAG, "path:" + filePath);
        audioPlayService.playAudio(handler, reactContext, AudioManager.STREAM_RING, uri.getScheme(), filePath);
    }

    /**
     * ??????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void stopPlay(Promise promise) {
        audioPlayService.stopPlay(handler, reactContext);
    }

    /**
     * *****************************systemMsg ????????????******************************************
     **/

    SysMessageObserver sysMessageObserver = SysMessageObserver.getInstance();

    /**
     * ????????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void startSystemMsg(Promise promise) {
        sysMessageObserver = SysMessageObserver.getInstance();
        sysMessageObserver.startSystemMsg();
    }

    /**
     * ????????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void stopSystemMsg(Promise promise) {
        if (sysMessageObserver != null)
            sysMessageObserver.stopSystemMsg();

    }

    /**
     * ??????????????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void startSystemMsgUnreadCount(Promise promise) {
        LoginService.getInstance().startSystemMsgUnreadCount();
    }


    /**
     * ??????????????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void stopSystemMsgUnreadCount(Promise promise) {
        LoginService.getInstance().registerSystemMsgUnreadCount(false);
    }

    /**
     * ????????????????????????
     *
     * @param offset
     * @param limit
     * @param promise
     */
    @ReactMethod
    public void querySystemMessagesBlock(String offset, String limit,
                                         final Promise promise) {
        int offsetInt = 0;
        int limitInt = 10;
        try {
            limitInt = Integer.parseInt(limit);
            offsetInt = Integer.parseInt(offset);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        List<SystemMessage> systemMessageList = NIMClient.getService(SystemMessageService.class)
                .querySystemMessagesBlock(offsetInt, limitInt);
        promise.resolve(ReactCache.createSystemMsg(systemMessageList));
    }


    /**
     * ??????/???????????????(???????????????)
     *
     * @param messageId
     * @param targetId
     * @param fromAccount
     * @param pass        ??????/??????
     * @param promise
     */
    @ReactMethod
    public void acceptInvite(String messageId, String targetId, String fromAccount, String pass, String timestamp, final Promise promise) {
        long messageIdLong = 0L;
        try {
            messageIdLong = Long.parseLong(messageId);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (sysMessageObserver != null)
            sysMessageObserver.acceptInvite(messageIdLong, targetId, fromAccount, string2Boolean(pass), timestamp, new RequestCallbackWrapper<Void>() {
                @Override
                public void onResult(int code, Void aVoid, Throwable throwable) {
                    if (code == ResponseCode.RES_SUCCESS) {
                        promise.resolve("" + code);
                    } else {
                        promise.reject("" + code, "");
                    }
                }
            });
    }

    /**
     * ??????/????????????(???????????????)
     *
     * @param messageId
     * @param targetId
     * @param fromAccount
     * @param pass        ??????/??????
     * @param promise
     */
    @ReactMethod
    public void passApply(String messageId, String targetId, String fromAccount, String pass, String timestamp, final Promise promise) {
        long messageIdLong = 0L;
        try {
            messageIdLong = Long.parseLong(messageId);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (sysMessageObserver != null)
            sysMessageObserver.passApply(messageIdLong, targetId, fromAccount, string2Boolean(pass), timestamp, new RequestCallbackWrapper<Void>() {
                @Override
                public void onResult(int code, Void aVoid, Throwable throwable) {
                    if (code == ResponseCode.RES_SUCCESS) {
                        promise.resolve("" + code);
                    } else {
                        promise.reject("" + code, "");
                    }
                }
            });
    }

    /**
     * ??????/????????????????????????
     *
     * @param contactId
     * @param pass
     * @param timestamp
     * @param promise
     */
    @ReactMethod
    public void ackAddFriendRequest(String messageId, final String contactId, String pass, String timestamp, final Promise promise) {
        LogUtil.w(TAG, "ackAddFriendRequest" + contactId);
        long messageIdLong = 0L;
        try {
            messageIdLong = Long.parseLong(messageId);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        final boolean toPass = string2Boolean(pass);
        if (sysMessageObserver != null)
            sysMessageObserver.ackAddFriendRequest(messageIdLong, contactId, string2Boolean(pass), timestamp, new RequestCallbackWrapper<Void>() {
                @Override
                public void onResult(int code, Void aVoid, Throwable throwable) {
                    if (code == ResponseCode.RES_SUCCESS) {
                        if (toPass) {
                            IMMessage message = MessageBuilder.createTextMessage(contactId, SessionTypeEnum.P2P, "????????????????????????????????????????????????");
                            NIMClient.getService(MsgService.class).sendMessage(message, false);
                        }
                        promise.resolve("" + code);
                    } else {
                        promise.reject("" + code, "");
                    }
                }
            });
    }

    /**
     * ??????????????????
     *
     * @param fromAccount
     * @param timestamp
     * @param promise
     */
    @ReactMethod
    public void deleteSystemMessage(String fromAccount, String timestamp, final Promise promise) {
        if (sysMessageObserver != null)
            sysMessageObserver.deleteSystemMessageById(fromAccount, true);
    }

    /**
     * ????????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void clearSystemMessages(final Promise promise) {
        if (sysMessageObserver != null)
            sysMessageObserver.clearSystemMessages();

    }

    /**
     * ?????????????????????????????????
     *
     * @param promise
     */
    @ReactMethod
    public void resetSystemMessageUnreadCount(final Promise promise) {
        NIMClient.getService(SystemMessageService.class).resetSystemMessageUnreadCount();
    }


    @ReactMethod
    public void getCacheSize(final Promise promise) {
        FileCacheUtil.getCacheSie(new FileCacheUtil.OnObserverGet() {
            @Override
            public void onGetCacheSize(String size) {
                promise.resolve(size);
            }
        });
    }

    @ReactMethod
    public void cleanCache(final Promise promise) {
        FileCacheUtil.cleanCache(new FileCacheUtil.OnObserverClean() {

            @Override
            public void onCleanCache(boolean succeeded) {
                promise.resolve("" + succeeded);
            }
        });
    }

    void showTip(final String tip) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(reactContext.getCurrentActivity(), tip, Toast.LENGTH_SHORT).show();
            }
        });

    }

    void showTip(final int tipId) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(reactContext.getCurrentActivity(), reactContext.getString(tipId), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        LogUtil.w(TAG, "onActivityResult:" + requestCode + "-result:" + resultCode);
    }

    @Override
    public void onNewIntent(Intent intent) {

        LogUtil.w(TAG, "onNewIntent:" + intent.getExtras());
//        ReceiverMsgParser.openIntent(intent);
        if (reactContext.getCurrentActivity() != null && ReceiverMsgParser.checkOpen(intent)) {
            intent.putExtras(getIntent());
            reactContext.getCurrentActivity().setIntent(intent);
            ReactCache.emit(ReactCache.observeBackgroundPushEvent, ReceiverMsgParser.getWritableMap(intent));
            launch = null;
        }

    }

    public static String status = "";
    public static Intent launch = null;

    @ReactMethod
    public void getLaunch(Promise promise) {
        if (launch == null) {
            promise.resolve(null);
        } else {
            promise.resolve(ReceiverMsgParser.getWritableMap(launch));
            launch = null;
        }
    }

    @ReactMethod
    public void fetchNetInfo(Promise promise) {
        int networkType = NetworkUtil.getNetworkClass(reactContext);
        String networkString = "";
        switch (networkType) {
            case NetworkUtil.NETWORK_CLASS_2_G:
                networkString = "2g";
                break;
            case NetworkUtil.NETWORK_CLASS_3_G:
                networkString = "3g";
                break;
            case NetworkUtil.NETWORK_CLASS_4_G:
                networkString = "4g";
                break;
            case NetworkUtil.NETWORK_CLASS_WIFI:
                networkString = "wifi";
                break;
            case NetworkUtil.NETWORK_CLASS_UNKNOWN:
                networkString = "unknown";
                break;
        }
        promise.resolve(networkString);
    }

    @ReactMethod
    public void allMessagesRead() {
        NIMClient.getService(MsgService.class).clearAllUnreadCount();
    }

    @Override
    public void onHostResume() {

        LogUtil.w(TAG, "onHostResume:" + status);

        if (!TextUtils.isEmpty(status) && !"onHostPause".equals(status)) {
            if (NIMClient.getStatus().wontAutoLogin()) {
                WritableMap r = Arguments.createMap();
                r.putString("status", status);
                ReactCache.emit(ReactCache.observeOnKick, r);
            }
        }
//        if (NIMClient.getStatus().wontAutoLogin()) {
//            Toast.makeText(IMApplication.getContext(), "??????????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
//        }
        status = "";
    }

    @Override
    public void onHostPause() {
        if (TextUtils.isEmpty(status)) {
            status = "onHostPause";
        }
        LogUtil.w(TAG, "onHostPause");
    }

    @Override
    public void onHostDestroy() {
        LogUtil.w(TAG, "onHostDestroy");
    }
}