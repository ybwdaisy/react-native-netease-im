//
//  SessionViewController.m
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/27.
//

#import "SessionViewController.h"

@interface SessionViewController () <NIMLoginManagerDelegate, NIMChatManagerDelegate, NIMConversationManagerDelegate, NIMSystemNotificationManagerDelegate>

@property (nonatomic, strong, readwrite) NSString *sessionId;
@property (nonatomic, readwrite) NIMSessionType *sessionType;
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
    _sessionType = [type integerValue];
    _session = [NIMSession session:_sessionId type:*(_sessionType)];
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

- (void)updateCustomMessage:(NSString *)messageId withAttachment:(NSDictionary *)attachment {
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
            loginSyncDataStatus = @"1";
            break;
        case NIMLoginStepSyncOK:
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

@end
