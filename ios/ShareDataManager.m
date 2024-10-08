//
//  ShareDataManager.m
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/27.
//

#import "ShareDataManager.h"

@implementation ShareDataManager

+ (instancetype)shared {
    static ShareDataManager *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[self alloc] init];
    });
    return instance;
}

- (instancetype)init {
    self = [super init];
    return self;
}

- (void)setOnlineStatus:(NSString *)onlineStatus {
    _onlineStatus = onlineStatus;
    if (self.eventBlock) {
        self.eventBlock(observeOnlineStatus, onlineStatus);
    }
}

- (void)setLoginSyncDataStatus:(NSString *)loginSyncDataStatus {
    _loginSyncDataStatus = loginSyncDataStatus;
    if (self.eventBlock) {
        self.eventBlock(observeLoginSyncDataStatus, loginSyncDataStatus);
    }
}

- (void)setMessageList:(NSMutableArray *)messageList {
    _messageList = messageList;
    if (self.eventBlock) {
        self.eventBlock(observeMsgStatus, messageList);
    }
}

- (void)setReceiveMessages:(NSMutableArray *)receiveMessages {
    _receiveMessages = receiveMessages;
    if (self.eventBlock) {
        self.eventBlock(observeReceiveMessage, receiveMessages);
    }
}

- (void)setRecentContact:(NSMutableDictionary *)recentContact {
    _recentContact = recentContact;
    if (self.eventBlock) {
        self.eventBlock(observeRecentContact, recentContact);
    }
}

- (void)setSystemNotifications:(NSMutableArray *)systemNotifications {
    _systemNotifications = systemNotifications;
    if (self.eventBlock) {
        self.eventBlock(observeReceiveSystemMsg, systemNotifications);
    }
}

- (void)setSysUnreadCount:(NSInteger)sysUnreadCount {
    _sysUnreadCount = sysUnreadCount;
    if (self.eventBlock) {
        self.eventBlock(observeSysUnreadCount, @(sysUnreadCount));
    }
}

- (void)setAttachmentProgress:(NSMutableDictionary *)attachmentProgress {
    _attachmentProgress = attachmentProgress;
    if (self.eventBlock) {
        self.eventBlock(observeAttachmentProgress, attachmentProgress);
    }
}

@end
