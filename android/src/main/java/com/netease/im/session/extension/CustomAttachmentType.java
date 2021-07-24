package com.netease.im.session.extension;

/**
 * Created by zhoujianghua on 2015/4/9.
 */
public interface CustomAttachmentType {


    String RedPacket = "redpacket";//红包
    String BankTransfer = "transfer";//转账

    String BankTransferSystem= "system";//系统消息
    String RedPacketOpen = "redPacketOpen";//拆红包提醒

    String ProfileCard = "profileCard";//个人名片

    String LinkUrl = "url";//链接
    String AccountNotice = "accountNotice";//账户变动通知
    String Card = "card";//账户变动通知

    String Custom = "custom"; // 自定义消息

}
