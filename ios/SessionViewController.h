//
//  SessionViewController.h
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/27.
//

#import "UIKit/UIKit.h"
#import <NIMSDK/NIMSDK.h>
#import "ShareDataManager.h"
#import "MessageUtils.h"
#import "CustomAttachment.h"

typedef void(^Success)(id param);
typedef void(^Errors)(id erro);

@interface SessionViewController : UIViewController

+ (instancetype)sessionManager;

- (void)startSession:(NSString *)sessionId withType:(NSString *)type;
- (void)stopSession;

- (void)sendTextMessage:(NSString *)text;
- (void)sendImageMessage:(NSString *)path withDisplayName:(NSString *)displayName;
- (void)sendCustomMessage:(NSDictionary *)dict;
- (void)updateCustomMessage:(NSString *)messageId withAttachment:(NSDictionary *)attachment;
- (void)resendMessage:(NSString *)messageId;
- (void)deleteMessage:(NSString *)messageId;
- (void)clearMessage;
- (Boolean)isMyFriend;
- (NSInteger)getTotalUnreadCount;
- (void)clearAllUnreadCount;
- (void)queryRecentContacts;
- (void)deleteRecentContact:(NSString *)account;
- (void)queryMessageListEx:(NSString *)messageId withLimit:(NSInteger)limit success:(Success)success error:(Errors)error;

@end

