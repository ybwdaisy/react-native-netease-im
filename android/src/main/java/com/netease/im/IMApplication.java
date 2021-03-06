package com.netease.im;

import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.location.LocationProvider;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.DrawableRes;

import com.netease.im.common.ImageLoaderKit;
import com.netease.im.common.sys.SystemUtil;
import com.netease.im.contact.DefalutUserInfoProvider;
import com.netease.im.contact.DefaultContactProvider;
import com.netease.im.login.LoginService;
import com.netease.im.session.SessionUtil;
import com.netease.im.session.extension.CustomAttachParser;
import com.netease.im.uikit.LoginSyncDataStatusObserver;
import com.netease.im.uikit.cache.DataCacheManager;
import com.netease.im.uikit.common.util.log.LogUtil;
import com.netease.im.uikit.common.util.media.ImageUtil;
import com.netease.im.uikit.common.util.storage.StorageType;
import com.netease.im.uikit.common.util.storage.StorageUtil;
import com.netease.im.uikit.common.util.sys.ScreenUtil;
import com.netease.im.uikit.contact.core.ContactProvider;
import com.netease.im.uikit.contact.core.query.PinYin;
import com.netease.im.uikit.session.helper.MessageHelper;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.SDKOptions;
import com.netease.nimlib.sdk.StatusBarNotificationConfig;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.mixpush.MixPushConfig;
import com.netease.nimlib.sdk.mixpush.MixPushService;
import com.netease.nimlib.sdk.msg.MessageNotifierCustomization;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.model.CustomNotification;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.RevokeMsgNotification;
import com.netease.nimlib.sdk.uinfo.UserInfoProvider;


/**
 * Created by dowin on 2017/4/28.
 */

public class IMApplication {


    // context
    private static Context context;

    private static Class mainActivityClass;
    @DrawableRes
    private static int notify_msg_drawable_id;
    // ?????????????????????
    private static UserInfoProvider userInfoProvider;

    // ????????????????????????
    private static ContactProvider contactProvider;

    // ???????????????????????????
    private static LocationProvider locationProvider;

    // ????????????????????????????????????
    private static ImageLoaderKit imageLoaderKit;
    private static StatusBarNotificationConfig statusBarNotificationConfig;
    private static boolean DEBUG = false;

    public static void init(Context context) {
        NIMClient.initSDK();
        // init pinyin
        PinYin.init(context);
        PinYin.validate();
        NIMClient.getService(MixPushService.class).enable(true);
        // ?????????Kit??????
        initKit();
    }

    public static void initConfig(Context context, Class mainActivityClass, @DrawableRes int notify_msg_drawable_id, ImPushConfig miPushConfig, String appKey) {
        IMApplication.context = context.getApplicationContext();
        IMApplication.mainActivityClass = mainActivityClass;
        IMApplication.notify_msg_drawable_id = notify_msg_drawable_id;
        NIMClient.config(context, getLoginInfo(), getOptions(context, miPushConfig, appKey));
    }

    public static void setDebugAble(boolean debugAble) {
        DEBUG = debugAble;
        LogUtil.setDebugAble(debugAble);
    }

    private static Observer<CustomNotification> notificationObserver = new Observer<CustomNotification>() {
        @Override
        public void onEvent(CustomNotification customNotification) {
            NotificationManager notificationManager = (NotificationManager) IMApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
            SessionUtil.receiver(notificationManager, customNotification);
        }
    };

    private static boolean inMainProcess(Context context) {
        String packageName = context.getPackageName();
        String processName = SystemUtil.getProcessName(context);
        return packageName.equals(processName);
    }


    public static Context getContext() {
        return context;
    }

    public static int getNotify_msg_drawable_id() {
        return notify_msg_drawable_id;
    }

    public static Class getMainActivityClass() {
        return mainActivityClass;
    }

    private static LoginInfo getLoginInfo() {
        return LoginService.getInstance().getLoginInfo(context);
    }

    public static String getSdkStorageRooPath() {
        return Environment.getExternalStorageDirectory() + "/" + context.getPackageName() + "/nim";
    }

    private static SDKOptions getOptions(Context context, ImPushConfig miPushConfig, String appKey) {
        SDKOptions options = new SDKOptions();

        // ???????????????????????????????????????SDK????????????????????????????????????
        initStatusBarNotificationConfig(options, context);

        // ?????? app key
        options.appKey = appKey;

        // ??????????????????????????????log??????????????????

        options.sdkStorageRootPath = getSdkStorageRooPath();

        // ???????????????????????????
        options.databaseEncryptKey = "NETEASE";

        // ??????????????????????????????????????????
        options.preloadAttach = true;

        // ???????????????????????????????????????
        options.thumbnailSize = ImageUtil.getImageMaxEdge();

        // ?????????????????????
        options.userInfoProvider = new DefalutUserInfoProvider(context);

        // ???????????????????????????????????????????????????????????????SDK???????????????
        options.messageNotifierCustomization = messageNotifierCustomization;

        // ???????????????????????????
        options.sessionReadAck = true;
        //???????????? SDK ??????????????????
        options.checkManifestConfig = DEBUG;
        //reducedIM ????????? IM ??????
        //asyncInitSDK ???????????? SDK ?????????
        //teamNotificationMessageMarkUnread ????????????????????????????????????????????????????????????
        //sdkStorageRootPath ????????????????????????????????????

        // ??????: https://faq.yunxin.163.com/kb/main/#/item/KB0373
        options.disableAwake = true;

        // ????????????
        if(miPushConfig!=null) {
            MixPushConfig pushConfig = new MixPushConfig();
            pushConfig.xmAppId = miPushConfig.xmAppId;
            pushConfig.xmAppKey = miPushConfig.xmAppKey;
            pushConfig.xmCertificateName = miPushConfig.xmCertificateName;
            pushConfig.hwCertificateName = miPushConfig.hwCertificateName;
            options.mixPushConfig = pushConfig;
        }

        return options;
    }

    // ???????????????????????????????????????????????? StatusBarNotificationConfig
    private static StatusBarNotificationConfig loadStatusBarNotificationConfig(Context context) {
        StatusBarNotificationConfig config = new StatusBarNotificationConfig();
        // ????????????????????????????????????
        config.notificationEntrance = mainActivityClass;
        config.notificationSmallIconId = notify_msg_drawable_id;

        // ???????????????uri?????????
        config.notificationSound = "android.resource://" + context.getPackageName() + "/raw/msg";

        // ???????????????
        config.ledARGB = Color.GREEN;
        config.ledOnMs = 1000;
        config.ledOffMs = 1500;

        // save cache???????????????????????????
        setStatusBarNotificationConfig(config);
        return config;
    }

    private static void initStatusBarNotificationConfig(SDKOptions options, Context context) {
        // load ????????????????????????
        StatusBarNotificationConfig config = loadStatusBarNotificationConfig(context);

        // load ????????? StatusBarNotificationConfig ?????????
        StatusBarNotificationConfig userConfig = null;//UserPreferences.getStatusConfig();
        if (userConfig == null) {
            userConfig = config;
        } else {
            // ????????? UserPreferences ???????????????????????? 3.4 ???????????????
            // APP?????? StatusBarNotificationConfig ??????????????????????????????
            userConfig.notificationEntrance = config.notificationEntrance;
            userConfig.notificationFolded = config.notificationFolded;
        }
        // SDK statusBarNotificationConfig ??????
        options.statusBarNotificationConfig = userConfig;
    }

    private static MessageNotifierCustomization messageNotifierCustomization = new MessageNotifierCustomization() {
        @Override
        public String makeNotifyContent(String nick, IMMessage message) {
            return null; // ??????SDK????????????
        }

        @Override
        public String makeTicker(String nick, IMMessage message) {
            return null; // ??????SDK????????????
        }
        @Override
        public String makeRevokeMsgTip(String revokeAccount, IMMessage item) {
            return MessageUtil.getRevokeTipContent(item, revokeAccount);
        }
    };


    /*********************/
    public static void initKit() {
        NIMClient.getService(MsgService.class).registerCustomAttachmentParser(new CustomAttachParser());
        initUserInfoProvider(userInfoProvider);
        initContactProvider(contactProvider);

        imageLoaderKit = new ImageLoaderKit(context, null);

        // init data cache
        LoginSyncDataStatusObserver.getInstance().registerLoginSyncDataStatus(true);  // ????????????????????????????????????
        DataCacheManager.observeSDKDataChanged(true);
        if (!TextUtils.isEmpty(getLoginInfo().getAccount())) {
            DataCacheManager.buildDataCache(); // build data cache on auto login
        }

        // init tools
        StorageUtil.init(context, null);
        ScreenUtil.init(context);

        // ???????????????????????????
        registerMsgRevokeObserver();

        // init log
        String path = StorageUtil.getDirectoryByDirType(StorageType.TYPE_LOG);
        LogUtil.init(path, Log.DEBUG);
    }

    public static boolean isApkDebugable(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void registerMsgRevokeObserver() {
        NIMClient.getService(MsgServiceObserve.class).observeRevokeMessage(new Observer<RevokeMsgNotification>() {
            @Override
            public void onEvent(RevokeMsgNotification message) {
                if (message == null && message.getMessage() == null) {
                    return;
                }

                MessageHelper.getInstance().onRevokeMessage(message.getMessage());
            }
        }, true);
    }

    // ??????????????????????????????
    private static void initUserInfoProvider(UserInfoProvider userInfoProvider) {

        if (userInfoProvider == null) {
            userInfoProvider = new DefalutUserInfoProvider(context);
        }

        IMApplication.userInfoProvider = userInfoProvider;
    }

    // ?????????????????????????????????
    private static void initContactProvider(ContactProvider contactProvider) {

        if (contactProvider == null) {
            contactProvider = new DefaultContactProvider();
        }

        IMApplication.contactProvider = contactProvider;
    }

    public static UserInfoProvider getUserInfoProvider() {
        return userInfoProvider;
    }

    public static ContactProvider getContactProvider() {
        return contactProvider;
    }

    public static ImageLoaderKit getImageLoaderKit() {
        return imageLoaderKit;
    }

    public static void setStatusBarNotificationConfig(StatusBarNotificationConfig statusBarNotificationConfig) {
        IMApplication.statusBarNotificationConfig = statusBarNotificationConfig;
    }

    public static StatusBarNotificationConfig getNotificationConfig() {
        return statusBarNotificationConfig;
    }
}
