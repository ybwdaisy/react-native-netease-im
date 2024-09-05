//
//  SessionViewController.m
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/27.
//

#import "SessionViewController.h"

@interface SessionViewController () <NIMLoginManagerDelegate, NIMChatManagerDelegate, NIMConversationManagerDelegate, NIMSystemNotificationManagerDelegate>

@property (nonatomic, strong, readwrite) NSString *sessionId;
@property (nonatomic, strong, readwrite) NSString *sessionType;
@property (nonatomic, strong, readwrite) NIMSession *session;

@end

@implementation SessionViewController

+ (instancetype)sessionManager {
    static SessionViewController *instance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        instance = [[self alloc] init];
    });
    return instance;
}

+ (BOOL)requiresMainQueueSetup {
    return NO;
}

- (void)viewDidLoad {
    [super viewDidLoad];
}

- (instancetype)init {
    self = [super init];
    if (self) {
        [[[NIMSDK sharedSDK] loginManager] addDelegate:self];
        [[[NIMSDK sharedSDK] chatManager] addDelegate:self];
        [[[NIMSDK sharedSDK] conversationManager] addDelegate:self];
        [[[NIMSDK sharedSDK] systemNotificationManager] addDelegate:self];
    }
    return self;
}

- (void)startSession:(NSString *)sessionId withType:(NSString *)type {
    _sessionId = sessionId;
    _sessionType = type;
    _session = [NIMSession session:sessionId type:[type integerValue]];
}

- (void)stopSession {
    [[[NIMSDK sharedSDK] loginManager] removeDelegate:self];
    [[[NIMSDK sharedSDK] chatManager] removeDelegate:self];
    [[[NIMSDK sharedSDK] conversationManager] removeDelegate:self];
    [[[NIMSDK sharedSDK] systemNotificationManager] removeDelegate:self];
}

- (void)sendTextMessage:(NSString *)text {
    NIMMessage *message = [[NIMMessage alloc] init];
    message.text = text;
    message.apnsContent = text;
    
    [self setApnsPayload:message];
    [[[NIMSDK sharedSDK] chatManager] sendMessage:message toSession:_session error:nil];
}

- (void)sendImageMessage:(NSString *)path withDisplayName:(NSString *)displayName {
    UIImage *image = [[UIImage alloc]initWithContentsOfFile:path];
    NIMImageObject *imageObject = [[NIMImageObject alloc] initWithImage:image];
    NIMImageOption *option  = [[NIMImageOption alloc] init];
    option.compressQuality = 0.8;
    imageObject.option = option;
    
    NSDateFormatter *dateFormatter = [[NSDateFormatter alloc] init];
    [dateFormatter setDateFormat:@"yyyy-MM-dd HH:mm"];
    NSString *dateString = [dateFormatter stringFromDate:[NSDate date]];
    imageObject.displayName = displayName ? displayName : [NSString stringWithFormat:@"图片发送于%@", dateString];
    
    NIMMessage *message = [[NIMMessage alloc] init];
    message.messageObject = imageObject;
    message.apnsContent = @"发来了一张图片";

    [self setApnsPayload:message];
    [[[NIMSDK sharedSDK] chatManager] sendMessage:message toSession:_session error:nil];
    
}

- (void)sendCustomMessage:(NSDictionary *)dict {
    NIMCustomObject *customObject = [[NIMCustomObject alloc] init];
    CustomAttachment *attachment = [[CustomAttachment alloc] init];
    attachment.type = CustomMessageTypeCustom;
    attachment.data = dict;
    customObject.attachment = attachment;
    
    NIMMessage *message = [[NIMMessage alloc] init];
    message.messageObject = customObject;
    message.apnsContent = [dict objectForKey:@"pushContent"];
    
    [self setApnsPayload:message];
    [[[NIMSDK sharedSDK] chatManager] sendMessage:message toSession:_session error:nil];
}

- (void)updateCustomMessage:(nonnull NSString *)messageId withAttachment:(nonnull NSDictionary *)attachment {
    NSArray *messageIds = [[NSArray alloc]initWithObjects:messageId, nil];
    NSArray *messages = [[[NIMSDK sharedSDK] conversationManager] messagesInSession:_session messageIds:messageIds];
    NIMMessage *message = messages[0];
    
    CustomAttachment *customAttachment = [[CustomAttachment alloc] init];
    customAttachment.type = CustomMessageTypeCustom;
    customAttachment.data = attachment;
    
    NIMCustomObject *customObject = [[NIMCustomObject alloc] init];
    customObject.attachment = customAttachment;
    message.messageObject = customObject;
    
    [[[NIMSDK sharedSDK] conversationManager] updateMessage:message forSession:_session completion:nil];
}

- (void)resendMessage:(nonnull NSString *)messageId {
    NSArray *messageIds = [[NSArray alloc]initWithObjects:messageId, nil];
    NSArray *messages = [[[NIMSDK sharedSDK] conversationManager] messagesInSession:_session messageIds:messageIds];
    NIMMessage *message = messages[0];
    
    if (message.isReceivedMsg) {
        [[[NIMSDK sharedSDK] chatManager] fetchMessageAttachment:message error:nil];
    } else {
        [[[NIMSDK sharedSDK] chatManager] resendMessage:message error:nil];
    }
}

- (void)deleteMessage:(nonnull NSString *)messageId {
    NSArray *messageIds = [[NSArray alloc]initWithObjects:messageId, nil];
    NSArray *messages = [[[NIMSDK sharedSDK] conversationManager] messagesInSession:_session messageIds:messageIds];
    NIMMessage *message = messages[0];
    
    [[[NIMSDK sharedSDK] conversationManager] deleteMessage:message];
}

- (void)clearMessage {
    NIMDeleteMessagesOption *option = [[NIMDeleteMessagesOption alloc] init];
    option.removeSession = NO;

    [[[NIMSDK sharedSDK] conversationManager] deleteAllmessagesInSession:_session option:option];
}

- (BOOL)isMyFriend {
    return [[[NIMSDK sharedSDK] userManager] isMyFriend:_session.sessionId];
}

- (NSInteger)getTotalUnreadCount {
    return [[[NIMSDK sharedSDK] conversationManager] allUnreadCount];
}

- (void)clearAllUnreadCount {
    [[[NIMSDK sharedSDK] conversationManager] markAllMessagesRead];
}

- (void)queryMessageListEx:(NSString *)messageId withLimit:(NSInteger)limit success:(Success)success error:(Errors)error {
    [[[NIMSDK sharedSDK] conversationManager] markAllMessagesReadInSession:_session];
    NIMMessage *anchorMessage = nil;
    if (messageId) {
        NSArray *messageIds = [[NSArray alloc]initWithObjects:messageId, nil];
        NSArray *messages = [[[NIMSDK sharedSDK] conversationManager] messagesInSession:_session messageIds:messageIds];
        anchorMessage = messages[0];
    }
    
    [[[NIMSDK sharedSDK] conversationManager] messagesInSession:_session message:anchorMessage limit:limit completion:^(NSError * _Nullable err, NSArray<NIMMessage *> * _Nullable messages) {
        if (err != nil || messages == nil) {
            error(@"获取历史消息失败");
            return;
        }
        NSMutableArray *finalMessages = [MessageUtils createMessageList:messages];
        success(finalMessages);
    }];
    
}

- (void)queryRecentContacts:(Success)success error:(Errors)error {
    NSMutableArray *allRecentSessions = [[[NIMSDK sharedSDK] conversationManager] allRecentSessions];
    if (allRecentSessions == nil) {
        error(@"获取最近会话失败");
        return;
    }
    NSMutableDictionary *sessionDict = [MessageUtils createRecentContact:allRecentSessions];
    success(sessionDict);
}

- (void)deleteRecentContact:(nonnull NSString *)sessionId {
    NSArray *allRecentSessions = [[[NIMSDK sharedSDK] conversationManager] allRecentSessions];
    for (NIMRecentSession *recent in allRecentSessions) {
        if ([recent.session.sessionId isEqualToString:sessionId]) {
            [[[NIMSDK sharedSDK] conversationManager] deleteRecentSession:recent];
            // TODO: 是否需要清除所有消息
//            NIMDeleteMessagesOption *option = [[NIMDeleteMessagesOption alloc]init];
//            option.removeSession = YES;
//            [[[NIMSDK sharedSDK] conversationManager] deleteAllmessagesInSession:recent.session option:option];
        }
    }
}


#pragma mark - Utils
- (void)setApnsPayload:(NIMMessage *)message {
    NSMutableDictionary *payload = [NSMutableDictionary dictionary];

    NSMutableDictionary *apsField = [NSMutableDictionary dictionary];
    [apsField setValue:@1 forKey:@"content-available"];
    [payload setValue:apsField forKey:@"apsField"];
    
    NSMutableDictionary *sessionInfo = [NSMutableDictionary dictionary];
    NSString *sessionId = [[[NIMSDK sharedSDK] loginManager] currentAccount];
    NSString *sessionType = [NSString stringWithFormat:@"%zd", _session.sessionType];
    [sessionInfo setValue:sessionId forKey:@"sessionId"];
    [sessionInfo setValue:sessionType forKey:@"sessionType"];
    [payload setValue:sessionInfo forKey:@"session"];
    
    message.apnsPayload = payload;
}

#pragma mark - NIMLoginManagerDelegate
- (void)onLogin:(NIMLoginStep)step {
    NSString *onlineStatus = nil;
    NSString *loginSyncDataStatus = nil;
    switch (step) {
        case NIMLoginStepLinking:
            onlineStatus = @"1";
            break;
        case NIMLoginStepLinkOK:
            onlineStatus = @"2";
            break;;
        case NIMLoginStepLinkFailed:
            onlineStatus = @"3";
            break;
        case NIMLoginStepLogining:
            onlineStatus = @"4";
            break;
        case NIMLoginStepLoginOK:
            onlineStatus = @"5";
            break;
        case NIMLoginStepLoginFailed:
            onlineStatus = @"6";
            break;
        case NIMLoginStepSyncing:
            onlineStatus = @"7";
            loginSyncDataStatus = @"1";
            break;
        case NIMLoginStepSyncOK:
            onlineStatus = @"8";
            loginSyncDataStatus = @"2";
            break;
        case NIMLoginStepLoseConnection:
            onlineStatus = @"9";
            break;
        case NIMLoginStepNetChanged:
            onlineStatus = @"10";
            break;
        case NIMLoginStepLogout:
            onlineStatus = @"11";
            break;
        default:
            break;
    }
    
    if (onlineStatus != nil) {
        [ShareDataManager shared].onlineStatus = onlineStatus;
    }
    
    if (loginSyncDataStatus != nil) {
        [ShareDataManager shared].loginSyncDataStatus = loginSyncDataStatus;
    }
    
}


#pragma mark - NIMChatManagerDelegate
- (void)willSendMessage:(NIMMessage *)message {
    NSMutableArray *messages = [[NSMutableArray alloc] initWithObjects:message, nil];
    NSMutableArray *newMessages = [MessageUtils createMessageList:messages];
    [ShareDataManager shared].messageList = newMessages;
}

- (void)sendMessage:(NIMMessage *)message progress:(float)progress {
    NSMutableArray *messages = [[NSMutableArray alloc] initWithObjects:message, nil];
    NSMutableArray *newMessages = [MessageUtils createMessageList:messages];
    [ShareDataManager shared].messageList = newMessages;
    
    NSMutableDictionary *attachmentProgress = [NSMutableDictionary dictionary];
    [attachmentProgress setValue:message.messageId forKey:@"uuid"];
    [attachmentProgress setValue:@(progress) forKey:@"progress"];
    [ShareDataManager shared].attachmentProgress = attachmentProgress;
}

- (void)sendMessage:(NIMMessage *)message didCompleteWithError:(nullable NSError *)error {
    NSMutableArray *messages = [[NSMutableArray alloc] initWithObjects:message, nil];
    NSMutableArray *newMessages = [MessageUtils createMessageList:messages];
    [ShareDataManager shared].messageList = newMessages;
}

- (void)onRecvMessages:(NSArray<NIMMessage *> *)messages {
    NSMutableArray *receiveMessages = [MessageUtils createMessageList:messages];
    [ShareDataManager shared].receiveMessages = receiveMessages;
}

#pragma mark NIMConversationManagerDelegate
- (void)didAddRecentSession:(NIMRecentSession *)recentSession totalUnreadCount:(NSInteger)totalUnreadCount {
    [self updateRecent:recentSession totalUnreadCount:totalUnreadCount];
}

- (void)didUpdateRecentSession:(NIMRecentSession *)recentSession totalUnreadCount:(NSInteger)totalUnreadCount {
    [self updateRecent:recentSession totalUnreadCount:totalUnreadCount];
}

- (void)didRemoveRecentSession:(NIMRecentSession *)recentSession totalUnreadCount:(NSInteger)totalUnreadCount {
    [self updateRecent:recentSession totalUnreadCount:totalUnreadCount];
}

- (void)updateRecent:(NIMRecentSession *)recentSession totalUnreadCount:(NSInteger)totalUnreadCount {
    NSMutableArray *recents = [[NSMutableArray alloc] initWithObjects:recentSession, nil];
    NSMutableDictionary *result = [MessageUtils createRecentContact:recents];
    [ShareDataManager shared].recentContact = result;
}


#pragma mark NIMSystemNotificationManagerDelegate
- (void)onReceiveSystemNotification:(NIMSystemNotification *)notification {
    NSMutableArray *notifications= [[NSMutableArray alloc] initWithObjects:notification, nil];
    NSMutableArray *result = [MessageUtils createSystemMsg:notifications];
    [ShareDataManager shared].systemNotifications = result;
}

- (void)onSystemNotificationCountChanged:(NSInteger)unreadCount {
    [ShareDataManager shared].sysUnreadCount = unreadCount;
}

@end
