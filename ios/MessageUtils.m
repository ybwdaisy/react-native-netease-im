//
//  MessageUtils.m
//  react-native-netease-im
//
//  Created by ybwdaisy on 2024/8/27.
//

#import "MessageUtils.h"

@implementation MessageUtils

+ (NSMutableArray *)createMessageList:(NSMutableArray<NIMMessage *> *)messages {
    NSMutableArray *newMessages = [NSMutableArray array];
    for (NIMMessage *message in messages) {
        NIMMessage *newMessage = [MessageUtils createMessage:message];
        [newMessages addObject:newMessage];
    }
    return newMessages;
}

+ (NSMutableDictionary *)createMessage:(NIMMessage *)message {
    NSMutableDictionary *newMessage = [NSMutableDictionary dictionary];
    
    [newMessage setObject:[NSString stringWithFormat:@"%@", message.messageId] forKey:@"msgId"];
    NSString *msgType = [MessageUtils convertNIMMessageTypeToString:message.messageType];
    [newMessage setObject:[NSString stringWithFormat:@"%@", msgType] forKey:@"msgType"];
    [newMessage setObject: [NSNumber numberWithBool:message.isOutgoingMsg] forKey:@"isOutgoing"];
    [newMessage setObject:[NSString stringWithFormat:@"%f", message.timestamp] forKey:@"timeString"];
    [newMessage setObject:[NSString stringWithFormat:@"%@", message.text ? message.text : @""] forKey:@"text"];
    [newMessage setObject:[NSString stringWithFormat:@"%@", message.session.sessionId] forKey:@"sessionId"];
    [newMessage setObject:[NSString stringWithFormat:@"%ld", message.session.sessionType] forKey:@"sessionType"];
    
    // fromUser
    NIMUser *user = [[[NIMSDK sharedSDK] userManager] userInfo:message.from];
    NSMutableDictionary *fromUser = [NSMutableDictionary dictionary];
    [fromUser setObject:[NSString stringWithFormat:@"%@", message.from] forKey:@"_id"];
    [fromUser setObject:[NSString stringWithFormat:@"%@", user.userInfo.avatarUrl] forKey:@"avatar"];
    NSString *name = user.alias;
    if (user.alias.length) {
        name = user.alias;
    } else if (user.userInfo.nickName.length) {
        name = user.userInfo.nickName;
    } else {
        name = user.userId;
    }
    [fromUser setObject:[NSString stringWithFormat:@"%@", name] forKey:@"name"];
    [newMessage setObject:fromUser forKey:@"fromUser"];
    
    // status
    NSString *status = [MessageUtils convertNIMMessageDeliveryStateToString:message.deliveryState];
    NSString *isFriend = [message.localExt objectForKey:@"isFriend"];
    if ([isFriend length]) {
        if ([isFriend isEqualToString:@"NO"]) {
            status = @"send_failed";
        }
    }
    [newMessage setObject:status forKey:@"status"];
    
    // extend
    switch (message.messageType) {
        case NIMMessageTypeImage: {
            NIMImageObject *object = message.messageObject;
            NSMutableDictionary *imgObj = [NSMutableDictionary dictionary];
            if (object.url != nil) {
                [imgObj setObject:[NSString stringWithFormat:@"%@", object.url] forKey:@"url"];
            }
            if (object.thumbPath != nil) {
                [imgObj setObject:[NSString stringWithFormat:@"%@", object.thumbPath] forKey:@"thumbPath"];
            }
            [imgObj setObject:[NSString stringWithFormat:@"%@", object.displayName] forKey:@"displayName"];
            [imgObj setObject:[NSString stringWithFormat:@"%f", [object size].height] forKey:@"height"];
            [imgObj setObject:[NSString stringWithFormat:@"%f", [object size].width] forKey:@"width"];
            [newMessage setObject:imgObj forKey:@"extend"];
        }
        case NIMMessageTypeAudio: {
            NIMAudioObject *object = message.messageObject;
            NSMutableDictionary *voiceObj = [NSMutableDictionary dictionary];
            if (object.url != nil) {
                [voiceObj setObject:[NSString stringWithFormat:@"%@", object.url] forKey:@"url"];
            }
            [voiceObj setObject:[NSString stringWithFormat:@"%@", object.path] forKey:@"path"];
            [voiceObj setObject:[NSNumber numberWithInteger: object.duration] forKey:@"duration"];
            [newMessage setObject:voiceObj forKey:@"extend"];
        }
        case NIMMessageTypeVideo: {
            NIMVideoObject *object = message.messageObject;
            NSMutableDictionary *videoObj = [NSMutableDictionary dictionary];
            if (object.url != nil) {
                [videoObj setObject:[NSString stringWithFormat:@"%@", object.url] forKey:@"url"];
            }
            if (object.path != nil) {
                [videoObj setObject:[NSString stringWithFormat:@"%@", object.path] forKey:@"path"];
            }
            if (object.coverPath != nil) {
                [videoObj setObject:[NSString stringWithFormat:@"%@", object.coverPath] forKey:@"thumbPath"];
            }
            [videoObj setObject:[NSString stringWithFormat:@"%f", object.coverSize.height] forKey:@"height"];
            [videoObj setObject:[NSString stringWithFormat:@"%f", object.coverSize.width] forKey:@"width"];
            [videoObj setObject:[NSString stringWithFormat:@"%ld", object.duration] forKey:@"duration"];
            [videoObj setObject:[NSString stringWithFormat:@"%@", object.displayName] forKey:@"displayName"];
            [videoObj setObject:[NSString stringWithFormat:@"%lld", object.fileLength] forKey:@"size"];
            if ([[NSFileManager defaultManager] fileExistsAtPath: object.path]) {
                [videoObj setObject:[NSString stringWithFormat:@"%@", object.path] forKey:@"mediaPath"];
            }
            [newMessage setObject:videoObj forKey:@"extend"];
        }
        case NIMMessageTypeLocation: {
            NIMLocationObject *object = message.messageObject;
            NSMutableDictionary *locationObj = [NSMutableDictionary dictionary];
            [locationObj setObject:[NSString stringWithFormat:@"%f", object.latitude] forKey:@"latitude"];
            [locationObj setObject:[NSString stringWithFormat:@"%f", object.longitude] forKey:@"longitude"];
            [locationObj setObject:[NSString stringWithFormat:@"%@", object.title] forKey:@"title"];
            [newMessage setObject:locationObj forKey:@"extend"];
        }
        case NIMMessageTypeTip: {
            NSMutableDictionary *tipObj = [NSMutableDictionary dictionary];
            [tipObj setObject:message.text forKey:@"tipMsg"];
            [newMessage setObject:tipObj forKey:@"extend"];
        }
        case NIMMessageTypeCustom: {
            NIMCustomObject *customObject = message.messageObject;
            if ([customObject.attachment isKindOfClass:[CustomAttachment class]]) {
                CustomAttachment *attachment = (CustomAttachment *)customObject.attachment;
                if (attachment.type == CustomMessageTypeCustom) {
                    [newMessage setObject:attachment.data forKey:@"extend"];
                }
            }
        }
        default:
            break;
    }
    
    return newMessage;
}

+ (NSMutableDictionary *)createRecentContact:(NSMutableArray<NIMRecentSession *> *)recents {
    NSMutableArray *newRecents = [NSMutableArray array];
    NSInteger unreadCount = 0;
    for (NIMRecentSession *recent in recents) {
        unreadCount = unreadCount + recent.unreadCount;
        
        NSMutableDictionary *newRecent = [NSMutableDictionary dictionary];
        
        [newRecent setObject:[NSString stringWithFormat:@"%@", recent.session.sessionId] forKey:@"contactId"];
        [newRecent setObject:[NSString stringWithFormat:@"%zd", recent.session.sessionType] forKey:@"sessionType"];
        [newRecent setObject:[NSString stringWithFormat:@"%zd", recent.unreadCount] forKey:@"unreadCount"];
        [newRecent setObject:[NSString stringWithFormat:@"%@", [MessageUtils getUserName:recent.session.sessionId]] forKey:@"name"];
        [newRecent setObject:[NSString stringWithFormat:@"%@", recent.lastMessage.from] forKey:@"account"];
        [newRecent setObject:[NSString stringWithFormat:@"%zd", recent.lastMessage.messageType] forKey:@"msgType"];
        [newRecent setObject:[NSString stringWithFormat:@"%zd", recent.lastMessage.deliveryState] forKey:@"msgStatus"];
        [newRecent setObject:[NSString stringWithFormat:@"%@", recent.lastMessage.messageId] forKey:@"messageId"];
        [newRecent setObject:[NSString stringWithFormat:@"%@", [MessageUtils getContent:recent.lastMessage]] forKey:@"content"];
        [newRecent setObject:[NSString stringWithFormat:@"%@", [MessageUtils getShowTime:recent.lastMessage.timestamp showDetail:NO]] forKey:@"time"];
        [newRecent setObject:[NSString stringWithFormat:@"%@", [MessageUtils getUserAvatar:recent.session.sessionId]] forKey:@"imagePath"];
        [newRecent setObject:[MessageUtils getExt:recent.session.sessionId] forKey:@"ext"];
        
        [newRecents addObject:newRecent];
    }
    NSMutableDictionary *result = [[NSMutableDictionary alloc] init];
    [result setValue:newRecents forKey:@"recents"];
    [result setValue:@(unreadCount) forKey:@"unreadCount"];
    return result;
}

+ (NSMutableArray *)createSystemMsg:(NSMutableArray<NIMSystemNotification *> *)notifications {
    NSMutableArray *newNotifications = [NSMutableArray array];
    for (NIMSystemNotification *notification in notifications) {
        NSMutableDictionary *newNotification = [NSMutableDictionary dictionary];
        
        NSMutableDictionary *verifyInfo = [MessageUtils getVerifyInfo:notification];
        NSString *isVerify = [verifyInfo objectForKey:@"isVerify"];
        NSString *verifyText = [verifyInfo objectForKey:@"verifyText"];
        
        [newNotification setObject:[NSString stringWithFormat:@"%@", isVerify] forKey:@"isVerify"];
        [newNotification setObject:[NSString stringWithFormat:@"%@", verifyText] forKey:@"verifyText"];
        [newNotification setObject:[NSString stringWithFormat:@"%lld", notification.notificationId] forKey:@"messageId"];
        [newNotification setObject:[NSString stringWithFormat:@"%ld", notification.type] forKey:@"type"];
        [newNotification setObject:[NSString stringWithFormat:@"%@", notification.targetID] forKey:@"targetId"];
        [newNotification setObject:[NSString stringWithFormat:@"%@", notification.sourceID] forKey:@"fromAccount"];
        [newNotification setObject:[NSString stringWithFormat:@"%@", notification.postscript] forKey:@"content"];
        [newNotification setObject:[NSString stringWithFormat:@"%@", [MessageUtils getUserName:notification.sourceID]] forKey:@"name"];
        [newNotification setObject:[NSString stringWithFormat:@"%@", [MessageUtils getUserAvatar:notification.sourceID]] forKey:@"avatar"];
        [newNotification setObject:[NSString stringWithFormat:@"%ld", notification.handleStatus] forKey:@"status"];
        [newNotification setObject:[NSString stringWithFormat:@"%f", notification.timestamp] forKey:@"time"];
        
        [newNotifications addObject:newNotification];
    }
    
    return newNotifications;
}

#pragma mark Message Utils
+ (NSString *)convertNIMMessageDeliveryStateToString:(NIMMessageDeliveryState)state {
    switch (state) {
       case NIMMessageDeliveryStateFailed:
            return @"send_failed";
       case NIMMessageDeliveryStateDelivering:
            return @"send_going";
       case NIMMessageDeliveryStateDeliveried:
            return @"send_succeed";
       default:
            return @"send_failed";
    }
}

+ (NSString *)convertNIMMessageTypeToString:(NIMMessageType)type {
    switch (type) {
        case NIMMessageTypeText:
            return @"text";
        case NIMMessageTypeImage:
            return @"image";
        case NIMMessageTypeAudio:
            return @"audio";
        case NIMMessageTypeVideo:
            return @"video";
        case NIMMessageTypeLocation:
            return @"location";
        case NIMMessageTypeNotification:
            return @"notification";
        case NIMMessageTypeFile:
            return @"file";
        case NIMMessageTypeTip:
            return @"tip";
        case NIMMessageTypeRobot:
            return @"robot";
        case NIMMessageTypeCustom:
            return @"custom";
        default:
            return @"custom";
    }
}

#pragma mark Recent Sessions Utils
+ (NSString *)getUserName:(NSString *)userId {
    NIMUser *user = [[[NIMSDK sharedSDK] userManager] userInfo:userId];
    return user.userInfo.nickName;
}

+ (NSString *)getUserAvatar:(NSString *)userId {
    NIMUser *user = [[[NIMSDK sharedSDK] userManager] userInfo:userId];
    return user.userInfo.thumbAvatarUrl;
}

+ (NSString *)getExt:(NSString *)userId {
     NIMUser *user = [[NIMSDK sharedSDK].userManager userInfo:userId];
     NIMUserInfo * userInfo = user.userInfo;
     return userInfo.ext;
}

+ (NSString *)getContent:(NIMMessage *)lastMessage {
    NSString *text = @"";
    switch (lastMessage.messageType) {
        case NIMMessageTypeText:
            text = lastMessage.text;
            break;
        case NIMMessageTypeAudio:
            text = @"[语音]";
            break;
        case NIMMessageTypeImage:
            text = @"[图片]";
            break;
        case NIMMessageTypeVideo:
            text = @"[视频]";
            break;
        case NIMMessageTypeLocation:
            text = @"[位置]";
            break;
        case NIMMessageTypeNotification:
            text = @"[未知消息]";
            break;
        case NIMMessageTypeFile:
            text = @"[文件]";
            break;
        case NIMMessageTypeTip:
            text = lastMessage.text;
            break;
        case NIMMessageTypeCustom:
            text = [MessageUtils getCustomText:lastMessage];
            break;
        default:
            text = @"[未知消息]";
            break;
    }
    BOOL isSelf = lastMessage.from == [[[NIMSDK sharedSDK] loginManager] currentAccount];
    if (isSelf || lastMessage.messageType == NIMMessageTypeTip) {
        return text;
    } else {
        NSString *nickName = [MessageUtils getUserName:lastMessage.from];
        return nickName.length ? [nickName stringByAppendingFormat:@":%@", text] : @"";
    }
}

+ (NSString *)getCustomText:(NIMMessage *)message {
    NIMCustomObject *customObject = message.messageObject;
    CustomAttachment *attachment = customObject.attachment;
    if (attachment.type == CustomMessageTypeCustom) {
        NSString *recentContent = [attachment.data objectForKey:@"recentContent"];
        return recentContent;
    }
    return @"[未知消息]";
}

+ (NSString *)getShowTime:(NSTimeInterval)timestamp showDetail:(BOOL)showDetail {
    NSDate * nowDate = [NSDate date];
    NSDate * msgDate = [NSDate dateWithTimeIntervalSince1970:timestamp];
    NSString *result = nil;
    NSCalendarUnit components = (NSCalendarUnit)(NSCalendarUnitYear|NSCalendarUnitMonth|NSCalendarUnitDay|NSCalendarUnitWeekday|NSCalendarUnitHour | NSCalendarUnitMinute);
    NSDateComponents *nowDateComponents = [[NSCalendar currentCalendar] components:components fromDate:nowDate];
    NSDateComponents *msgDateComponents = [[NSCalendar currentCalendar] components:components fromDate:msgDate];
    
    NSInteger hour = msgDateComponents.hour;
    double OnedayTimeIntervalValue = 24*60*60;

    result = [MessageUtils getPeriodOfTime:hour withMinute:msgDateComponents.minute];
    if (hour > 12) {
        hour = hour - 12;
    }
    
    if(nowDateComponents.day == msgDateComponents.day) {
        result = [[NSString alloc] initWithFormat:@"%@ %zd:%02d", result, hour, (int)msgDateComponents.minute];
    } else if(nowDateComponents.day == (msgDateComponents.day + 1)) {
        result = showDetail?  [[NSString alloc] initWithFormat:@"昨天%@ %zd:%02d", result, hour, (int)msgDateComponents.minute] : @"昨天";
    } else if(nowDateComponents.day == (msgDateComponents.day + 2)) {
        result = showDetail? [[NSString alloc] initWithFormat:@"前天%@ %zd:%02d",result,hour,(int)msgDateComponents.minute] : @"前天";
    } else if([nowDate timeIntervalSinceDate:msgDate] < 7 * OnedayTimeIntervalValue) {
        NSString *weekDay = [MessageUtils weekdayStr:msgDateComponents.weekday];
        result = showDetail? [weekDay stringByAppendingFormat:@"%@ %zd:%02d", result, hour, (int)msgDateComponents.minute] : weekDay;
    } else {
        NSString *day = [NSString stringWithFormat:@"%zd-%zd-%zd", msgDateComponents.year, msgDateComponents.month, msgDateComponents.day];
        result = showDetail? [day stringByAppendingFormat:@"%@ %zd:%02d", result, hour, (int)msgDateComponents.minute]:day;
    }
    return result;
}

+ (NSString *)getPeriodOfTime:(NSInteger)time withMinute:(NSInteger)minute {
    NSInteger totalMin = time *60 + minute;
    NSString *showPeriodOfTime = @"";
    if (totalMin > 0 && totalMin <= 5 * 60) {
        showPeriodOfTime = @"凌晨";
    } else if (totalMin > 5 * 60 && totalMin < 12 * 60) {
        showPeriodOfTime = @"上午";
    } else if (totalMin >= 12 * 60 && totalMin <= 18 * 60) {
        showPeriodOfTime = @"下午";
    } else if ((totalMin > 18 * 60 && totalMin <= (23 * 60 + 59)) || totalMin == 0) {
        showPeriodOfTime = @"晚上";
    }
    return showPeriodOfTime;
}

+ (NSString*)weekdayStr:(NSInteger)dayOfWeek {
    static NSDictionary *daysOfWeekDict = nil;
    daysOfWeekDict = @{@(1):@"星期日",
                       @(2):@"星期一",
                       @(3):@"星期二",
                       @(4):@"星期三",
                       @(5):@"星期四",
                       @(6):@"星期五",
                       @(7):@"星期六",};
    return [daysOfWeekDict objectForKey:@(dayOfWeek)];
}

#pragma mark System Notification Utils
+ (NSMutableDictionary *)getVerifyInfo:(NIMSystemNotification *)notification {
    NSString *isVerify = @"0";
    NSString *verifyText = @"未知请求";
    NIMTeam *team = [[[NIMSDK sharedSDK] teamManager] teamById:notification.targetID];
    
    switch (notification.type) {
        case NIMSystemNotificationTypeTeamApply: {
            isVerify = @"1";
            verifyText = [NSString stringWithFormat:@"申请加入群 %@", team.teamName];
            break;
        }
        case NIMSystemNotificationTypeTeamApplyReject: {
            verifyText = [NSString stringWithFormat:@"群 %@ 拒绝你加入", team.teamName];
            break;
        }
        case NIMSystemNotificationTypeTeamInvite: {
            isVerify = @"1";
            verifyText = [NSString stringWithFormat:@"群 %@ 邀请你加入", team.teamName];
            break;
        }
        case NIMSystemNotificationTypeTeamIviteReject: {
            verifyText = [NSString stringWithFormat:@"拒绝了群 %@ 邀请", team.teamName];
            break;
        }
        case NIMSystemNotificationTypeFriendAdd: {
            id object = notification.attachment;
            if ([object isKindOfClass:[NIMUserAddAttachment class]]) {
                NIMUserOperation operation = [(NIMUserAddAttachment *)object operationType];
                switch (operation) {
                    case NIMUserOperationAdd:
                        verifyText = @"已添加你为好友";
                        break;
                    case NIMUserOperationRequest:
                        isVerify = @"1";
                        verifyText = [notification.postscript length]? notification.postscript : @"请求添加你为好友";
                        break;
                    case NIMUserOperationVerify:
                        verifyText = @"通过了你的好友请求";
                        break;
                    case NIMUserOperationReject:
                        verifyText = @"拒绝了你的好友请求";
                        break;
                    default:
                        break;
                }
            }
        }
            break;
        default:
            break;
    }
    
    NSMutableDictionary *result = [NSMutableDictionary dictionary];
    [result setValue:isVerify forKey:@"isVerify"];
    [result setValue:verifyText forKey:@"verifyText"];
    
    return result;
    
}


@end
