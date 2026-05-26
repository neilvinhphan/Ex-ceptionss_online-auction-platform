package org.example.server.services;

import org.example.core.models.users.User;
import org.example.server.daos.UserDAO;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    private UserDAO userDAOMock;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userDAOMock = mock(UserDAO.class);
        userService = new UserService(userDAOMock);
    }

    // =========================================================================
    // NHÓM 1: KIỂM THỬ LUỒNG NẠP TIỀN VÀO TÀI KHOẢN (balanceDeposit)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1. Nạp tiền thất bại do ID người dùng không hợp lệ (<= 0)")
    void testBalanceDeposit_InvalidUserId_ThrowsException() {
        Exception exception = assertThrows(Exception.class, () ->
                userService.balanceDeposit(0, new BigDecimal("100000"), "password123")
        );
        assertEquals("Mã số tài khoản định danh người dùng không hợp lệ!", exception.getMessage());
    }

    @Test
    @Order(2)
    @DisplayName("2. Nạp tiền thất bại do số tiền nạp nhỏ hơn hoặc bằng 0")
    void testBalanceDeposit_NegativeAmount_ThrowsException() {
        Exception exception = assertThrows(Exception.class, () ->
                userService.balanceDeposit(2, new BigDecimal("-50000"), "password123")
        );
        assertEquals("Số tiền yêu cầu nạp vào tài khoản ví phải lớn hơn 0 VNĐ!", exception.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("3. Nạp tiền thất bại do bỏ trống mật khẩu xác nhận")
    void testBalanceDeposit_EmptyPassword_ThrowsException() {
        Exception exception = assertThrows(Exception.class, () ->
                userService.balanceDeposit(1, new BigDecimal("50000"), "")
        );
        assertEquals("Vui lòng nhập mật khẩu xác nhận danh tính để phê duyệt nạp tiền.", exception.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("4. Nạp tiền thất bại do mật khẩu xác nhận sai lệch")
    void testBalanceDeposit_WrongPassword_ThrowsException() throws Exception {
        int userId = 1;
        User mockUser = new User();
        mockUser.setBalance(new BigDecimal("200000"));
        mockUser.setPassword(BCrypt.hashpw("securePassword", BCrypt.gensalt()));

        when(userDAOMock.getUserByUserId(userId)).thenReturn(mockUser);

        Exception exception = assertThrows(Exception.class, () ->
                userService.balanceDeposit(userId, new BigDecimal("50000"), "wrongPassword")
        );
        assertEquals("Mật khẩu tài khoản xác nhận không chính xác! Vui lòng kiểm tra lại.", exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("5. Nạp tiền thất bại do lỗi hệ thống từ Database khi update")
    void testBalanceDeposit_DatabaseUpdateFailure_ThrowsException() throws Exception {
        int userId = 1;
        String pass = "password123";

        User mockUser = new User();
        mockUser.setBalance(new BigDecimal("200000"));
        mockUser.setPassword(BCrypt.hashpw(pass, BCrypt.gensalt()));

        when(userDAOMock.getUserByUserId(userId)).thenReturn(mockUser);
        when(userDAOMock.updateBalanceInDB(eq(userId), any(BigDecimal.class))).thenReturn(false);

        Exception exception = assertThrows(Exception.class, () ->
                userService.balanceDeposit(userId, new BigDecimal("50000"), pass)
        );
        assertEquals("Đã xảy ra lỗi trong quá trình cập nhật số dư tài khoản! Vui lòng thử lại sau.", exception.getMessage());
    }

    @Test
    @Order(6)
    @DisplayName("6. Nạp tiền thành công (Cộng dồn số dư cũ và mới hợp lệ)")
    void testBalanceDeposit_Success() throws Exception {
        int userId = 1;
        String pass = "password123";
        BigDecimal depositAmount = new BigDecimal("50000");

        User mockUser = new User();
        mockUser.setBalance(new BigDecimal("200000"));
        mockUser.setPassword(BCrypt.hashpw(pass, BCrypt.gensalt()));

        when(userDAOMock.getUserByUserId(userId)).thenReturn(mockUser);
        BigDecimal expectedBalance = new BigDecimal("250000");
        when(userDAOMock.updateBalanceInDB(userId, expectedBalance)).thenReturn(true);

        BigDecimal resultBalance = userService.balanceDeposit(userId, depositAmount, pass);

        assertNotNull(resultBalance);
        assertEquals(expectedBalance, resultBalance);
        verify(userDAOMock, times(1)).updateBalanceInDB(userId, expectedBalance);
    }

    // =========================================================================
    // NHÓM 2: KIỂM THỬ THAY ĐỔI QUYỀN HẠN VÀ KHÓA/MỞ KHÓA TÀI KHOẢN
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("7. Cập nhật vai trò người dùng (updateRole) thành công")
    void testUpdateRole_Success() throws Exception {
        User mockUser = new User();
        when(userDAOMock.getUserByUserId(10)).thenReturn(mockUser);
        when(userDAOMock.updateRoleInDB(10)).thenReturn(true);

        assertTrue(userService.updateRole(10));
        verify(userDAOMock, times(1)).updateRoleInDB(10);
    }

    @Test
    @Order(8)
    @DisplayName("8. Khóa tài khoản người dùng (banUser) thành công")
    void testBanUser_Success() throws Exception {
        User mockUser = new User();
        when(userDAOMock.getUserByUserId(10)).thenReturn(mockUser);
        when(userDAOMock.banStatus(10)).thenReturn(true);

        assertTrue(userService.banUser(10));
        verify(userDAOMock, times(1)).banStatus(10);
    }

    @Test
    @Order(9)
    @DisplayName("9. Mở khóa tài khoản người dùng (unbanUser) thành công")
    void testUnbanUser_Success() throws Exception {
        User mockUser = new User();
        when(userDAOMock.getUserByUserId(10)).thenReturn(mockUser);
        when(userDAOMock.unbanStatus(10)).thenReturn(true);

        assertTrue(userService.unbanUser(10));
        verify(userDAOMock, times(1)).unbanStatus(10);
    }

    // =========================================================================
    // NHÓM 3: KIỂM THỬ TRUY VẤN THÔNG TIN (GETTERS)
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("10. Lấy thông tin User theo ID thành công")
    void testGetUserById_Success() throws Exception {
        User expectedUser = new User();
        expectedUser.setUserId(5);
        when(userDAOMock.getUserByUserId(5)).thenReturn(expectedUser);

        User result = userService.getUserById(5);
        assertNotNull(result);
        assertEquals(5, result.getUserId());
    }

    @Test
    @Order(11)
    @DisplayName("11. Lấy toàn bộ danh sách người dùng thành công")
    void testGetAllUsers_Success() throws Exception {
        List<User> list = new ArrayList<>();
        list.add(new User());
        list.add(new User());
        when(userDAOMock.getAllUsers()).thenReturn(list);

        List<User> result = userService.getAllUsers();
        assertEquals(2, result.size());
        verify(userDAOMock, times(1)).getAllUsers();
    }
}