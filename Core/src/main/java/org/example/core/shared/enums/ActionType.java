package org.example.core.shared.enums;

/**
 * Định nghĩa danh mục các hành động (Actions) được hỗ trợ trong hệ thống truyền thông Client-Server.
 * Việc sử dụng Enum thay cho String giúp kiểm soát chặt chẽ các loại yêu cầu và tránh lỗi runtime.
 */
public enum ActionType {
    // Auth Actions
    LOGIN,
    REGISTER,
    LOGOUT,

    // Item Actions
    CREATE_ITEM,
    UPDATE_ITEM_FULL,
    DELETE_ITEM,
    GET_PENDING_ITEMS,
    GET_APPROVED_ITEMS,

    // Auction Actions
    CREATE_AUCTION,
    GET_ACTIVE_AUCTIONS,
    REGISTER_AUTOBID,
    CANCEL_AUTOBID,
    GET_PENDING_AUCTIONS,
    APPROVE_AUCTION,


    // Bidding Actions
    PLACE_BID,
    GET_BID_HISTORY,

    // User
    DEPOSIT,
    GET_PENDING_PAYMENTS,
    GET_PAID_HISTORY,
    UPDATE_ROLE,
    JOIN_ROOM,
    LEAVE_ROOM,

    //Financial actions
    PAY_ITEM,
    PAY_ALL,
    GET_MARKET_HISTORY,


    // Dashboard Actions
    GET_ADMIN_DASHBOARD_STATS,
    GET_SELLER_DASHBOARD,

    //Admin
    ADMIN_GET_ALL_AUCTIONS,
    ADMIN_PROCESS_ITEM,
    ADMIN_GET_ALL_PENDING_ITEMS,
    ADMIN_GET_ALL_USERS,
    ADMIN_BAN_USER,
    ADMIN_CANCEL_AUCTION
}