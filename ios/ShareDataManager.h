//
//  ShareDataManager.h
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/27.
//

#import <Foundation/Foundation.h>
#import "MessageConstant.h"

typedef void(^onSuccess)(NSString *eventName, id data);

@interface ShareDataManager : NSObject

+ (instancetype)shared;
@property(nonatomic, strong) onSuccess eventBlock;
@property(nonatomic, strong) NSString *onlineStatus;
@property(nonatomic, strong) NSString *loginSyncDataStatus;
@property(nonatomic, strong) NSMutableArray *messageList;
@property(nonatomic, strong) NSMutableArray *receiveMessages;
@property(nonatomic, strong) NSMutableDictionary *recentContact;
@property(nonatomic, assign) NSInteger sysUnreadCount;
@property(nonatomic, assign) NSMutableDictionary *attachmentProgress;

@end
