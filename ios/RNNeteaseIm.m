#import "RNNeteaseIm.h"

@implementation RNNeteaseIm

RCT_EXPORT_MODULE()

- (instancetype)init {
    self = [super init];
    if (self) {
        [self setSendEvent];
        
        [[NSNotificationCenter defaultCenter]addObserver:self selector:@selector(clickNotification:) name:@"NETEASE_APNS_NOTIFICATION_ARRIVED_EVENT" object:nil];
        
    }
    return self;
}

+ (void)updateApnsToken:(NSData *)token customContentKey:(nullable NSString *)key {
    [[NIMSDK sharedSDK] updateApnsToken:token customContentKey:key];
}

+ (void)registerAppKey:(nullable NSString *)appKey andApnsCername:(nullable NSString*)apnsCername {
    
    NIMSDKOption *option = [[NIMSDKOption alloc] init];
    option.appKey = appKey;
    option.apnsCername = apnsCername;
    [[NIMSDK sharedSDK] registerWithOption:option];
    // 注册自定义消息的解析器
    [NIMCustomObject registerCustomDecoder:[[CustomAttachmentDecoder alloc] init]];
}

-(NSArray<NSString *> *)supportedEvents {
    return @[
        observeOnlineStatus,
        observeLoginSyncDataStatus,
        observeMsgStatus,
        observeRecentContact,
        observeReceiveMessage,
        observeAttachmentProgress,
        observeSysUnreadCount,
        observeReceiveSystemMsg,
        observeBackgroundPushEvent,
    ];
}

- (void)clickNotification:(NSNotification *)noti{
    NSDictionary *dict = noti.object;
    NSMutableDictionary *param = [NSMutableDictionary dictionaryWithDictionary:[dict objectForKey:@"dict"]];
    NSString *strDict = [param objectForKey:@"session"];
    if ([strDict length]) {
        NSDictionary *dataDict = [self dictFromJson:strDict];
        NSMutableDictionary *mutaDict = [NSMutableDictionary dictionaryWithDictionary:dataDict];
        NSString *strType = [mutaDict objectForKey:@"sessionType"];
        NSString *strSessionId = [mutaDict objectForKey:@"sessionId"];
        NSString *strSessionName = @"";
        if ([strType isEqualToString:@"0"]) {
            NIMUser *user = [[[NIMSDK sharedSDK] userManager] userInfo:strSessionId];
            if ([user.alias length]) {
                strSessionName = user.alias;
            } else {
                NIMUserInfo *userInfo = user.userInfo;
                strSessionName = userInfo.nickName;
            }
        } else {
            NIMTeam *team = [[[NIMSDK sharedSDK] teamManager]teamById:strSessionId];
            strSessionName = team.teamName;
        }
        if (!strSessionName) {
            strSessionName = @"";
        }
        [mutaDict setObject:strSessionName forKey:@"sessionName"];
        [param setObject:mutaDict forKey:@"session"];
        [self sendEventWithName:observeBackgroundPushEvent body:param];
    }
}

- (NSDictionary *)dictFromJson:(NSString *)json{
    NSData* data = [json dataUsingEncoding:NSUTF8StringEncoding];
    __autoreleasing NSError* error = nil;
    id result = [NSJSONSerialization JSONObjectWithData:data options:kNilOptions error:&error];
    if (error != nil) {
        return nil;
    };
    return result;
}

RCT_EXPORT_METHOD(login:(nonnull NSString *)account token:(nonnull NSString *)token resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    [[[NIMSDK sharedSDK] loginManager] login:account token:token completion:^(NSError * _Nullable error) {
        if (!error) {
            NIMPushNotificationSetting *setting = [[[NIMSDK sharedSDK] apnsManager] currentSetting];
            setting.type = NIMPushNotificationDisplayTypeNoDetail;
            [[[NIMSDK sharedSDK] apnsManager] updateApnsSetting:setting completion:^(NSError * _Nullable error) {
                if (!error) {
                    resolve(account);
                } else {
                    reject(@"-2", @"登录失败", nil);
                }
            }];
        } else {
            reject(@"-1", @"登录失败", nil);
        }
    }];
}

RCT_EXPORT_METHOD(logout) {
    [[[NIMSDK sharedSDK] loginManager] logout:^(NSError * _Nullable error) {}];
}

RCT_EXPORT_METHOD(startSession:(nonnull  NSString *)sessionId type:(nonnull  NSString *)type){
    [[SessionViewController sessionManager]startSession:sessionId withType:type];
}

RCT_EXPORT_METHOD(stopSession){
    [[SessionViewController sessionManager]stopSession];
}

RCT_EXPORT_METHOD(sendTextMessage:(nonnull NSString *)text) {
    [[SessionViewController sessionManager]sendTextMessage:text];
}

RCT_EXPORT_METHOD(sendImageMessage:(nonnull NSString *)path displayName:(NSString *)displayName) {
    [[SessionViewController sessionManager]sendImageMessage:path withDisplayName:displayName];
}

RCT_EXPORT_METHOD(sendCustomMessage:(NSDictionary *)dict) {
    [[SessionViewController sessionManager]sendCustomMessage:dict];
}

RCT_EXPORT_METHOD(updateCustomMessage:(NSString *)messageId attachment:(NSDictionary *)attachment) {
    [[SessionViewController sessionManager]updateCustomMessage:messageId withAttachment:attachment];
}

RCT_EXPORT_METHOD(resendMessage:(NSString *)messageId) {
    [[SessionViewController sessionManager]resendMessage:messageId];
}

RCT_EXPORT_METHOD(deleteMessage:(nonnull NSString *)messageId) {
    [[SessionViewController sessionManager]deleteMessage:messageId];
}

RCT_EXPORT_METHOD(clearMessage) {
    [[SessionViewController sessionManager]clearMessage];
}

RCT_EXPORT_METHOD(isMyFriend:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    BOOL isMyFriend = [[SessionViewController sessionManager]isMyFriend];
    resolve(@(isMyFriend));
}

RCT_EXPORT_METHOD(getTotalUnreadCount:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    NSInteger unreadCount = [[SessionViewController sessionManager]getTotalUnreadCount];
    resolve(@(unreadCount));
}

RCT_EXPORT_METHOD(clearAllUnreadCount) {
    [[SessionViewController sessionManager]clearAllUnreadCount];
}

RCT_EXPORT_METHOD(queryMessageListEx:(NSString *)messageId withLimit:(NSInteger)limit resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    [[SessionViewController sessionManager]queryMessageListEx:messageId withLimit:limit success:^(id param) {
        resolve(param);
    } error:^(id error) {
        reject(@"-1", error, nil);
    }];
}

RCT_EXPORT_METHOD(queryRecentContacts:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject) {
    [[SessionViewController sessionManager]queryRecentContacts:^(id param) {
        resolve(param);
    } error:^(id error) {
        reject(@"-1", error, nil);
    }];
}

RCT_EXPORT_METHOD(deleteRecentContact:(NSString *)sessionId) {
    [[SessionViewController sessionManager]deleteRecentContact:sessionId];
}


- (void)setSendEvent {
    [ShareDataManager shared].eventBlock = ^(NSString *eventName, id data) {
        [self sendEventWithName:eventName body:data];
    };
}

@end
