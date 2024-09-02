//
//  MessageConstant.h
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/27.
//

#import <Foundation/Foundation.h>

@interface MessageConstant : NSObject 

extern NSString * const observeOnlineStatus;         // 在线状态
extern NSString * const observeLoginSyncDataStatus;  // 登录状态
extern NSString * const observeRecentContact;        // 最近会话
extern NSString * const observeReceiveMessage;       // 接收消息
extern NSString * const observeMsgStatus;            // 发送消息状态变化
extern NSString * const observeAttachmentProgress;   // 附件上传下载进度
extern NSString * const observeSysUnreadCount;       // 系统消息未读数
extern NSString * const observeReceiveSystemMsg;     // 系统消息
extern NSString * const observeBackgroundPushEvent;  // 后台推送

@end
