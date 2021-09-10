
# React Native 的网易云信插件

### 1. 安装

```bash
npm install @ybwdaisy/react-native-netease-im 或者 yarn add @ybwdaisy/react-native-netease-im 
cd ios
pod install
```

### 2. 配置
#### 2.1 android 配置

在 `android/app/src/main/java/<你的包名>/MainApplication.java`中添加如下两行：

```
...
import com.netease.im.IMApplication;
import com.netease.im.ImPushConfig;

public class MainApplication extends Application implements ReactApplication {

  @Override
  public void onCreate() {
    // 推送配置，没有可传null
    ImPushConfig config = new ImPushConfig();
    // 小米证书配置，没有可不填
    config.xmAppId = "";
    config.xmAppKey = "";
    config.xmCertificateName = "";
    // 华为推送配置，没有可不填
    config.hwCertificateName = "";
    // 初始化
    IMApplication.initConfig(this, MainActivity.class, R.drawable.ic_stat_notify_msg, config, yourNeteaseimAppId);
  }
}
```

#### 2.2 iOS 配置


在你工程的`AppDelegate.m`文件中添加如下代码：

```
#import <NIMSDK/NIMSDK.h>
#import "NTESSDKConfigDelegate.h"
#import "DWCustomAttachmentDecoder.h"

@interface AppDelegate ()
@property (nonatomic,strong) NTESSDKConfigDelegate *sdkConfigDelegate;
@end

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
  ...
  [self setupNIMSDK];
  [self registerAPNs];
  if (launchOptions) {
    NSDictionary * remoteNotification = [launchOptions objectForKey:UIApplicationLaunchOptionsRemoteNotificationKey];
    if (remoteNotification) {
      [self performSelector:@selector(clickSendObserve:) withObject:remoteNotification afterDelay:0.5];
    }
  }
  ...
}

- (void)clickSendObserve:(NSDictionary *)dict{
  [[NSNotificationCenter defaultCenter] postNotificationName:@"ObservePushNotification" object:@{@"dict":dict,@"type":@"launch"}];
}
- (void)setupNIMSDK
{
  // 在注册 NIMSDK appKey 之前先进行配置信息的注册，如是否使用新路径,是否要忽略某些通知，是否需要多端同步未读数
  self.sdkConfigDelegate = [[NTESSDKConfigDelegate alloc] init];
  NIMSDKConfig *config = [NIMSDKConfig sharedConfig];
  [config setDelegate:self.sdkConfigDelegate];
  [config setShouldSyncUnreadCount:YES];
  // 注册 NIMSDK
  NIMSDKOption *option = [NIMSDKOption optionWithAppKey:yourNeteaseimAppId];
  option.apnsCername = yourNeteaseimCerName;
  // badge 回调
  [[NIMSDK sharedSDK] registerWithOption:option];
  [[[NIMSDK sharedSDK] apnsManager] registerBadgeCountHandler:^NSUInteger{
    return 0; // 根据情况返回全局 badge 值
  }];
  // 注册自定义消息的解析器
  [NIMCustomObject registerCustomDecoder: [[DWCustomAttachmentDecoder alloc]init]];
}

- (void)registerAPNs
{
  if (@available(iOS 11.0, *))
  {
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    [center requestAuthorizationWithOptions:(UNAuthorizationOptionBadge | UNAuthorizationOptionSound | UNAuthorizationOptionAlert) completionHandler:^(BOOL granted, NSError * _Nullable error) {}];
  } else {
    [[UIApplication sharedApplication] registerForRemoteNotifications];
    UIUserNotificationType types = UIUserNotificationTypeBadge | UIUserNotificationTypeSound | UIUserNotificationTypeAlert;
    UIUserNotificationSettings *settings = [UIUserNotificationSettings settingsForTypes:types categories:nil];
    [[UIApplication sharedApplication] registerUserNotificationSettings:settings];
  }
  // 注册push权限，用于显示本地推送
  [[UIApplication sharedApplication] registerUserNotificationSettings:[UIUserNotificationSettings settingsForTypes:(UIUserNotificationTypeSound | UIUserNotificationTypeAlert | UIUserNotificationTypeBadge) categories:nil]];
}

```

### 3. 使用

```
import { NimUtils, NimFriend, NimSession, NimSystemMsg, NimTeam } from '@ybwdaisy/react-native-netease-im';
```

### 4. 事件
```
observeRecentContact 最近会话
observeOnlineStatus 在线状态
observeFriend 联系人/好友
observeTeam 群组
observeBlackList 黑名单
observeReceiveMessage 接收消息
observeReceiveSystemMsg 系统通知
observeUnreadCountChange 未读消息数
observeMsgStatus 发送消息状态变化
observeAudioRecord 录音状态
observeDeleteMessage 撤销后删除消息
observeAttachmentProgress 未读数变化
observeOnKick 被踢出下线

```
