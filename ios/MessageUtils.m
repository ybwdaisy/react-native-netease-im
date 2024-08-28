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

@end
