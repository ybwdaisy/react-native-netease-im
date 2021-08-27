#### 2021-08-27
- feat: 支持自定义消息等
#### 2021-07-21
- fix: android fetchUserInfo 报 java.lang.NullPointerException 错误

#### 2021-07-07
- feat: iOS 发送消息时，推送消息支持后台消息

#### 2021-06-18
- feat: 修改 SDK 初始化方式

#### 2021-01-14
- feat: Android 禁止后台进程唤醒 UI 进程
#### 2020-10-14
- feat: NIMClient.init 使用传入的 appKey
- feat: iOS 重发消息不校验是否是好友

#### 2020-09-29
- feat: android&ios增加获取用户资料中扩展字段的方法

#### 2020-09-24
- build: 将官方库同步更新到 3.0.0

#### 2020-09-10
- feat: 增加批量获取用户资料
- feat: 增加通过 messageId 精确查询消息方法
- feat: 将 android 消息发送成功状态码改成 send_succeed，与 iOS 一致

#### 2020-08-18
- feat: 新增将所有联系人的未读数清零

#### 2020-08-05
- feat: 发送消息前不校验是否是好友，即能给陌生人发消息

#### 2020-07-20
- feat: iOS 登陆后隐藏推送详情
  - ios/RNNeteaseIm/RNNeteaseIm/RNNeteaseIm.m
  ```
    // 设置推送消息为不显示推送详情
    NIMPushNotificationSetting *setting = [NIMSDK sharedSDK].apnsManager.currentSetting;
    setting.type = NIMPushNotificationDisplayTypeNoDetail;
    [[[NIMSDK sharedSDK] apnsManager] updateApnsSetting:setting completion:^(NSError *error) {
        if (error) {
            NSLog(@"updateApnsSetting error: [%@]", error);
        }
        resolve(account);
    }];
  ```

#### 2020-06-16
- feat: android 默认隐藏推送详情
  - /netease/im/IMApplication.java
  ```
    config.hideContent = true;
  ```
