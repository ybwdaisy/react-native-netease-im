#import "RNNeteaseIm.h"

@implementation RNNeteaseIm

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(login:(nonnull NSString *)account token:(nonnull NSString *)token resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject)
{
    [[[NIMSDK sharedSDK] loginManager] login:account token:token completion:^(NSError * _Nullable error) {
        if (!error) {
            NIMPushNotificationSetting *setting = [[[NIMSDK sharedSDK] apnsManager] currentSetting];
            setting.type = NIMPushNotificationProfileDisableAll;
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
    [[[NIMSDK sharedSDK] loginManager] logout:^(NSError * _Nullable error) {
        //
    }];
}

RCT_EXPORT_METHOD(startSession:(nonnull  NSString *)sessionId type:(nonnull  NSString *)type){
    //
}

RCT_EXPORT_METHOD(stopSession){
    //
}


@end
