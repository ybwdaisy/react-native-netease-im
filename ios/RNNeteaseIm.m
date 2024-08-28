#import "RNNeteaseIm.h"

@implementation RNNeteaseIm

RCT_EXPORT_MODULE()

- (instancetype)init {
    self = [super init];
    if (self) {
        [self setSendEvent];
    }
    return self;
}

RCT_EXPORT_METHOD(login:(nonnull NSString *)account token:(nonnull NSString *)token resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
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


- (void)setSendEvent {
    [ShareDataManager shared].eventBlock = ^(NSString *eventName, id data) {
        [self sendEventWithName:eventName body:data];
    };
}

@end
