#import <React/RCTEventEmitter.h>
#import <React/RCTBridgeModule.h>
#import <NIMSDK/NIMSDK.h>
#import "SessionViewController.h"
#import "CustomAttachmentDecoder.h"

#define NETEASE_APNS_NOTIFICATION_ARRIVED_EVENT  @"NETEASE_APNS_NOTIFICATION_ARRIVED_EVENT"

@interface RNNeteaseIm : RCTEventEmitter <RCTBridgeModule>

+ (void)updateApnsToken:(NSData *)token customContentKey:(nullable NSString *)key;
+ (void)registerAppKey:(nullable NSString *)appKey andApnsCername:(nullable NSString*)apnsCername;

@end
