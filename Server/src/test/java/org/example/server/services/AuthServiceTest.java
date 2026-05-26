package org.example.server.services;

import org.example.core.dto.userDTO.LoginRequestDTO;
import org.example.core.dto.userDTO.RegisterRequestDTO;
import org.example.core.models.users.User;
import org.example.core.shared.enums.UserStatus;
import org.example.server.daos.UserDAO;
import org.junit.jupiter.api.*;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthServiceTest {

    private UserDAO userDAOMock;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userDAOMock = mock(UserDAO.class);
        authService = new AuthService(userDAOMock);
    }

    // =========================================================================
    // NHÓM 1: KIỂM THỬ LUỒNG ĐĂNG KÝ (REGISTER)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1. Đăng ký thất bại do bỏ trống thông tin bắt buộc (Username)")
    void testRegister_EmptyUsername_ThrowsException() {
        RegisterRequestDTO dto = new RegisterRequestDTO("", "password123", "test@gmail.com", "0987654321");

        Exception exception = assertThrows(Exception.class, () -> authService.register(dto));
        assertEquals("Vui lòng nhập tên tài khoản (Username)!", exception.getMessage());
    }

    @Test
    @Order(2)
    @DisplayName("2. Đăng ký thất bại do sai định dạng Email")
    void testRegister_InvalidEmailFormat_ThrowsException() {
        RegisterRequestDTO dto = new RegisterRequestDTO("thongnh", "password123", "sai-dinh-dang-email", "0987654321");

        Exception exception = assertThrows(Exception.class, () -> authService.register(dto));
        assertEquals("Định dạng thư điện tử không hợp lệ (Ví dụ chính xác: tennguoidung@gmail.com).", exception.getMessage());
    }

    @Test
    @Order(3)
    @DisplayName("3. Đăng ký thất bại do sai định dạng số điện thoại VN")
    void testRegister_InvalidPhoneFormat_ThrowsException() {
        RegisterRequestDTO dto = new RegisterRequestDTO("thongnh", "password123", "test@gmail.com", "123456");

        Exception exception = assertThrows(Exception.class, () -> authService.register(dto));
        assertEquals("Số điện thoại không hợp lệ (Phải bao gồm chính xác 10 chữ số và bắt đầu bằng số 0).", exception.getMessage());
    }

    @Test
    @Order(4)
    @DisplayName("4. Đăng ký thất bại do tài khoản Username đã tồn tại trong DB")
    void testRegister_UsernameExisted_ThrowsException() throws Exception {
        RegisterRequestDTO dto = new RegisterRequestDTO("thongnh", "0987654321", "test@gmail.com", "password123");

        when(userDAOMock.getUserByUsername("thongnh")).thenReturn(new User());

        Exception exception = assertThrows(Exception.class, () -> authService.register(dto));
        assertEquals("Tên tài khoản này đã được sử dụng bởi người khác trong hệ thống!", exception.getMessage());
    }

    @Test
    @Order(5)
    @DisplayName("5. Đăng ký thất bại do lỗi ghi nhận dữ liệu phía Database")
    void testRegister_DatabaseFailure_ThrowsException() throws Exception {
        RegisterRequestDTO dto = new RegisterRequestDTO("thongnh", "0987654321", "test@gmail.com", "password123");

        when(userDAOMock.getUserByUsername("thongnh")).thenReturn(null);
        when(userDAOMock.registerUser(any(User.class))).thenReturn(false);

        Exception exception = assertThrows(Exception.class, () -> authService.register(dto));
        assertEquals("Đã xảy ra lỗi hệ thống khi đồng bộ lưu trữ thông tin đăng ký vào cơ sở dữ liệu.", exception.getMessage());
    }

    @Test
    @Order(6)
    @DisplayName("6. Đăng ký thành công (Luồng chuẩn, có mã hóa mật khẩu)")
    void testRegister_Success() throws Exception {
        RegisterRequestDTO dto = new RegisterRequestDTO("thongnh", "0987654321", "test@gmail.com", "password123");

        when(userDAOMock.getUserByUsername("thongnh")).thenReturn(null);
        when(userDAOMock.registerUser(any(User.class))).thenReturn(true);

        User result = authService.register(dto);

        assertNotNull(result);
        assertEquals("thongnh", result.getUsername());
        assertEquals("test@gmail.com", result.getEmail());
        assertTrue(BCrypt.checkpw("password123", result.getPassword()));

        verify(userDAOMock, times(1)).registerUser(any(User.class));
    }

    // =========================================================================
    // NHÓM 2: KIỂM THỬ LUỒNG ĐĂNG NHẬP (LOGIN)
    // =========================================================================

    @Test
    @Order(7)
    @DisplayName("7. Đăng nhập thất bại do bỏ trống tài khoản")
    void testLogin_EmptyUsername_ThrowsException() {
        LoginRequestDTO dto = new LoginRequestDTO("", "password123");

        Exception exception = assertThrows(Exception.class, () -> authService.login(dto));
        assertEquals("Tên tài khoản đăng nhập không được bỏ trống.", exception.getMessage());
    }

    @Test
    @Order(8)
    @DisplayName("8. Đăng nhập thất bại do không tìm thấy Username trong DB")
    void testLogin_WrongUsername_ThrowsException() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO("unknownUser", "password123");

        when(userDAOMock.getUserByUsername("unknownUser")).thenReturn(null);

        Exception exception = assertThrows(Exception.class, () -> authService.login(dto));
        assertEquals("Tên tài khoản hoặc mật khẩu bạn nhập không chính xác.", exception.getMessage());
    }

    @Test
    @Order(9)
    @DisplayName("9. Đăng nhập thất bại do tài khoản đang bị khóa (BANNED)")
    void testLogin_UserBanned_ThrowsException() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO("bannedUser", "password123");

        User mockUser = new User();
        mockUser.setUsername("bannedUser");
        mockUser.setStatus(UserStatus.BANNED);

        when(userDAOMock.getUserByUsername("bannedUser")).thenReturn(mockUser);

        Exception exception = assertThrows(Exception.class, () -> authService.login(dto));
        assertEquals("Tài khoản của bạn đã bị khóa hoặc đình chỉ hoạt động bởi ban quản trị.", exception.getMessage());
    }

    @Test
    @Order(10)
    @DisplayName("10. Đăng nhập thất bại do sai Mật khẩu")
    void testLogin_WrongPassword_ThrowsException() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO("thongnh", "wrong_password");

        User mockUser = new User();
        mockUser.setUsername("thongnh");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setPassword(BCrypt.hashpw("password123", BCrypt.gensalt()));

        when(userDAOMock.getUserByUsername("thongnh")).thenReturn(mockUser);

        Exception exception = assertThrows(Exception.class, () -> authService.login(dto));
        assertEquals("Tên tài khoản hoặc mật khẩu bạn nhập không chính xác.", exception.getMessage());
    }

    @Test
    @Order(11)
    @DisplayName("11. Đăng nhập thành công (Luồng chuẩn)")
    void testLogin_Success() throws Exception {
        LoginRequestDTO dto = new LoginRequestDTO("thongnh", "password123");

        User mockUser = new User();
        mockUser.setUsername("thongnh");
        mockUser.setStatus(UserStatus.ACTIVE);
        mockUser.setPassword(BCrypt.hashpw("password123", BCrypt.gensalt()));

        when(userDAOMock.getUserByUsername("thongnh")).thenReturn(mockUser);

        User result = authService.login(dto);

        assertNotNull(result);
        assertEquals("thongnh", result.getUsername());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
    }
}