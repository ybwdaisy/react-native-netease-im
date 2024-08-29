//
//  MessageUtils.h
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/27.
//

#import <Foundation/Foundation.h>
#import <NIMSDK/NIMSDK.h>
#import "CustomAttachment.h"

@interface MessageUtils : NSObject

+ (NSMutableArray *)createMessageList:(NSMutableArray<NIMMessage *> *)messages;

+ (NSMutableDictionary *)createMessage:(NIMMessage *)message;

+ (NSMutableArray *)createRecentSessionList:(NSMutableArray<NIMRecentSession *> *)recents;

+ (NSMutableDictionary *)createRecentSession:(NIMRecentSession *)recent;

@end
