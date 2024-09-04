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
typedef void(^Errors)(id error);

@interface SessionViewController : UIViewController

+ (instancetype)sessionManager;

- (void)startSession:(NSString *)sessionId withType:(NSString *)type;
- (void)stopSession;

- (void)sendTextMessage:(NSString *)text;
- (void)sendImageMessage:(NSString *)path withDisplayName:(NSString *)displayName;
- (void)sendCustomMessage:(NSDictionary *)dict;
- (void)updateCustomMessage:(nonnull NSString *)messageId withAttachment:(nonnull NSDictionary *)attachment;
- (void)resendMessage:(nonnull NSString *)messageId;
- (void)deleteMessage:(nonnull NSString *)messageId;
- (void)clearMessage;
- (Boolean)isMyFriend;
- (NSInteger)getTotalUnreadCount;
- (void)clearAllUnreadCount;
- (void)queryMessageListEx:(NSString *)messageId withLimit:(NSInteger)limit success:(Success)success error:(Errors)error;
- (void)queryRecentContacts:(Success)success error:(Errors)error;
- (void)deleteRecentContact:(nonnull NSString *)sessionId;

@end

